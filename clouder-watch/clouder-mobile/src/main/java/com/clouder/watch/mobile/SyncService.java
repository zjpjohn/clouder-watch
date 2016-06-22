package com.clouder.watch.mobile;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.clouder.watch.common.sync.IMessageListener;
import com.clouder.watch.common.sync.SyncMessage;
import com.clouder.watch.common.sync.SyncMessageParser;
import com.clouder.watch.common.sync.SyncMessagePathConfig;
import com.clouder.watch.common.sync.message.UninstallApkSyncMessage;
import com.clouder.watch.common.utils.StringUtils;
import com.clouder.watch.mobile.sync.CmsServiceConnectionListener;
import com.clouder.watch.mobile.sync.IDataListener;
import com.clouder.watch.mobile.sync.INodeListener;
import com.clouder.watch.mobile.sync.app.WearableAppParser;
import com.clouder.watch.mobile.sync.handler.PhoneNumberSyncHandler;
import com.clouder.watch.mobile.sync.handler.SearchPhoneHandler;
import com.clouder.watch.mobile.sync.handler.WifiSyncHandler;
import com.cms.android.common.ConnectionResult;
import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.common.api.ResultCallback;
import com.cms.android.common.api.Status;
import com.cms.android.wearable.Asset;
import com.cms.android.wearable.DataApi;
import com.cms.android.wearable.DataEvent;
import com.cms.android.wearable.DataEventBuffer;
import com.cms.android.wearable.DataItem;
import com.cms.android.wearable.MessageApi;
import com.cms.android.wearable.MessageEvent;
import com.cms.android.wearable.Node;
import com.cms.android.wearable.NodeApi;
import com.cms.android.wearable.PutDataMapRequest;
import com.cms.android.wearable.PutDataRequest;
import com.cms.android.wearable.Wearable;
import com.cms.android.wearable.service.codec.CRCUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by yang_shoulai on 9/2/2015.
 */
public class SyncService extends Service implements MobvoiApiClient.OnConnectionFailedListener,
        MobvoiApiClient.ConnectionCallbacks,
        MessageApi.MessageListener, NodeApi.NodeListener, DataApi.DataListener {

    private static final String TAG = "SyncService";

    public static final String SDCARD_PATH = Environment.getExternalStorageDirectory().getPath() +
            File.separator + "ClouderWatch" + File.separator + "AppsToSync";

    public static final int SEND_MSG_FAILED_CMS_NOT_CONNECTED = 1;

    public static final int SEND_MSG_FAILED_MESSAGE_PATH_EMPTY = 2;

    public static final int SEND_MSG_FAILED_NO_NODES_FOUND = 3;

    public static final int SEND_MSG_FAILED_UNKNOWN_ERROR = 4;

    private WearableAppParser parser;

    private MobvoiApiClient mobvoiApiClient;

    public Handler mHandler = new Handler();

    private Map<IMessageListener, List<String>> messageListeners = new HashMap<>();

    private Map<IDataListener, List<String>> dataListeners = new HashMap<>();

    private List<INodeListener> nodeListeners = new ArrayList<>();

    private List<CmsServiceConnectionListener> cmsConnectionListeners = new ArrayList<>();

    private boolean cmsConnecting = true;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "同步服务 onCreate！！！");
        parser = new WearableAppParser(this);
        messageListeners.put(new SearchPhoneHandler(this), Arrays.asList(SyncMessagePathConfig.SEARCH_PHONE));
        messageListeners.put(new WifiSyncHandler(this), Arrays.asList(SyncMessagePathConfig.WIFI_INFO));
        messageListeners.put(new PhoneNumberSyncHandler(this), Arrays.asList(SyncMessagePathConfig.SYNC_PHONE_NUMBER));
        Log.d(TAG, "SyncService onCreate, build mobvoi api client!");
        mobvoiApiClient = new MobvoiApiClient.Builder(this).addApi(Wearable.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
        Log.d(TAG, "开始连接CMS服务！");
        cmsConnecting = true;
        mobvoiApiClient.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "同步服务 onDestroy！！！");
        Wearable.MessageApi.removeListener(mobvoiApiClient, this);
        Wearable.NodeApi.removeListener(mobvoiApiClient, this);
        Wearable.NodeApi.removeListener(mobvoiApiClient, this);
        Log.d(TAG, "断开CMS服务！");
        mobvoiApiClient.disconnect();
    }

    @Override
    public IBinder onBind(Intent intent) {

        return new SyncBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            final String mobilePackage = intent.getStringExtra("mobilePackage");
            final boolean install = intent.getBooleanExtra("install", true);
            if (!StringUtils.isEmpty(mobilePackage)) {
                if (install) {
                    Log.d(TAG, "app " + mobilePackage + " has changed, a thread will start to check if it has a wearable apk!");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            WearableAppParser.WearableApp wearableApp = parser.loadWearableApp(mobilePackage);
                            if (wearableApp != null) {
                                Log.d(TAG, "app " + mobilePackage + " has a wearable apk!,sync it!");
                                wearableApp = parser.loadDetailWearableAppInfo(wearableApp);
                                syncSingleApp(wearableApp);
                            }
                        }
                    }).start();
                } else {
                    sendMessage(new UninstallApkSyncMessage(mobilePackage));
                }
            }
        }
        //return super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    /**
     * 当连接建立失败的时候
     * 5秒之后尝试重连
     */
    private void reconnect() {
        cmsConnecting = true;
        Wearable.MessageApi.removeListener(mobvoiApiClient, this);
        Wearable.NodeApi.removeListener(mobvoiApiClient, this);
        Wearable.DataApi.removeListener(mobvoiApiClient, this);
        mobvoiApiClient.disconnect();
        Log.d(TAG, "5秒之后尝试重新连接CMS服务!");
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                cmsConnecting = true;
                mobvoiApiClient.connect();
            }
        }, 5000);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "同步服务连接到CMS服务！！！");
        cmsConnecting = false;
        Log.d(TAG, "添加NodeListener");
        Wearable.NodeApi.addListener(mobvoiApiClient, this).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    Log.e(TAG, "添加NodeListener成功");
                } else {
                    Log.e(TAG, "添加NodeListener失败! status = " + status);
                }
            }
        });
        Log.d(TAG, "添加MessageListener");
        Wearable.MessageApi.addListener(mobvoiApiClient, this).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    Log.e(TAG, "添加MessageListener成功!");
                } else {
                    Log.e(TAG, "添加MessageListener失败! status = " + status);
                }
            }
        });
        Log.d(TAG, "添加DataListener");
        Wearable.DataApi.addListener(mobvoiApiClient, this).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    Log.e(TAG, "添加DataListener成功!");
                } else {
                    Log.e(TAG, "添加DataListener失败! status = " + status);
                }
            }
        });

        onCmsConnected();

        NodeApi.GetConnectedNodesResult result = Wearable.NodeApi.getConnectedNodes(mobvoiApiClient).await();
        if (result != null) {
            if (result.getStatus().isSuccess()) {
                List<Node> nodes = result.getNodes();
                if (nodes != null && !nodes.isEmpty()) {
                    onHasNodesConnected();
                } else {
                    onNoNodeConnected();
                }
            } else {
                onGetNodesFailed();
            }
        } else {
            onGetNodesFailed();
        }

    }


    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "与CMS服务的连接被挂起!!");
        onCmsDisconnected();
        reconnect();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "接收到消息" + messageEvent.toString());
        final String path = messageEvent.getPath();
        byte[] data = messageEvent.getData();
        final SyncMessage syncMessage = SyncMessageParser.parse(path, data);
        if (syncMessage != null) {
            List<IMessageListener> msgListeners = getMessageListenerByPath(path);
            for (final IMessageListener listener : msgListeners) {
                Log.d(TAG, "回调消息监听器：" + listener.getClass().getName());
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onMessageReceived(path, syncMessage);
                    }
                });

            }

        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "同步服务连接CMS服务失败！result =  " + (connectionResult == null ? "null" : connectionResult));
        if (connectionResult != null && connectionResult.getErrorCode() == 9) {
            reconnect();
        }
    }

    @Override
    public void onPeerConnected(Node node) {
        Log.e(TAG, "Node已连接! Node = [displayName : " + node.getDisplayName() + ", id : " + node.getId() + "]");
        onHasNodesConnected();
    }


    @Override
    public void onPeerDisconnected(Node node) {
        Log.e(TAG, "Node断开连接! Node = [displayName : " + node.getDisplayName() + ", id : " + node.getId() + "]");
        onNoNodeConnected();
    }

    public void sendMessage(final SyncMessage syncMessage) {
        sendMessage(syncMessage, null);
    }

    public synchronized void sendMessage(final SyncMessage syncMessage, final ISendCallback callback) {
        Log.e(TAG, "发送消息：" + syncMessage.toString());
        if (!mobvoiApiClient.isConnected()) {
            if (cmsConnecting) {
                Log.d(TAG, "正在连接CMS服务.....");

            }
            Log.e(TAG, "发送消息失败[同步服务未连接CMS服务], message = " + syncMessage.toString());
            if (callback != null)
                callback.onSendFailed(syncMessage, SEND_MSG_FAILED_CMS_NOT_CONNECTED);
            onConnectionFailed(null);
            return;
        }
        final String path = syncMessage.getPath();
        if (StringUtils.isEmpty(path)) {
            Log.e(TAG, "发送消息失败[Path为空]!");
            if (callback != null)
                callback.onSendFailed(syncMessage, SEND_MSG_FAILED_MESSAGE_PATH_EMPTY);
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult result = Wearable.NodeApi.getConnectedNodes(mobvoiApiClient).await();
                Log.d(TAG, "onResult");
                if (result != null) {
                    List<Node> nodes = result.getNodes();
                    if (nodes != null && !nodes.isEmpty()) {
                        for (Node node : nodes) {
                            Log.d(TAG, "Send message to node " + node.getId());
                            MessageApi.SendMessageResult sendMessageResult = Wearable.MessageApi.sendMessage(mobvoiApiClient, node.getId(), path, syncMessage.toBytes()).await(5000, TimeUnit.MILLISECONDS);
                            if (sendMessageResult.getStatus().isSuccess()) {
                                Log.i(TAG, String.format("发送消息成功!,path = %s", path));
                                if (callback != null) {
                                    callback.onSendSuccess(syncMessage);
                                }
                            } else {
                                Log.i(TAG, String.format("发送消息失败! path = %s", path));
                                if (callback != null) {
                                    callback.onSendFailed(syncMessage, SEND_MSG_FAILED_UNKNOWN_ERROR);
                                }
                            }

                        }
                    } else {
                        Log.d(TAG, "发送消息失败，Node节点为空!");
                        if (callback != null)
                            callback.onSendFailed(syncMessage, SEND_MSG_FAILED_NO_NODES_FOUND);
                        onGetNodesFailed();
                    }
                } else {
                    Log.e(TAG, "发送消息失败，无法回去Node节点!");
                    if (callback != null)
                        callback.onSendFailed(syncMessage, SEND_MSG_FAILED_NO_NODES_FOUND);
                    onGetNodesFailed();
                }
            }
        }).start();
    }

    public void syncAllApps(final ISyncAppCallback syncAppCallback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "开始同步所有Wearable应用!");
                final List<WearableAppParser.WearableApp> apps = parser.parse();
                if (syncAppCallback != null) syncAppCallback.onSyncBegin(apps.size());
                if (apps != null && apps.size() > 0) {
                    Log.d(TAG, "发现 " + apps.size() + "个 wearable app! 将要按次序依次发送到手表端！");
                    for (int i = 0; i < apps.size(); i++) {
                        WearableAppParser.WearableApp app = apps.get(i);
                        Log.d(TAG, "发送Wearable应用[" + app.wearableAppName + "]");
                        if (syncAppCallback != null)
                            syncAppCallback.onSyncPost(i, apps.size(), app.wearablePackage);
                        syncSingleApp(app);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (syncAppCallback != null) syncAppCallback.onSyncComplete(apps.size());
                } else {
                    Log.d(TAG, "未发现Wearable应用!");
                    if (syncAppCallback != null) syncAppCallback.onSyncComplete(0);
                }

            }
        }).start();
    }

    public void syncSingleApp(final WearableAppParser.WearableApp app) {
        if (app != null) {
            String packageName = app.wearablePackage;
            String versionCode = app.versionCode;
            String appName = app.wearableAppName;
            String versionName = app.versionName;
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(SyncMessagePathConfig.WEARABLE_APK);
            putDataMapRequest.getDataMap().putString("name", appName);
            putDataMapRequest.getDataMap().putString("packageName", packageName);
            putDataMapRequest.getDataMap().putString("versionCode", versionCode);
            putDataMapRequest.getDataMap().putString("versionName", versionName);
            byte[] apk = parser.loadWearableApk(packageName, appName);
            if (apk == null || apk.length == 0) {
                Log.d(TAG, "无法在应用[" + app.wearableAppName + "]中获取Wearable apk！");
                return;
            }
            String folderName = packageName + "-" + versionCode;
            String folderPath = SDCARD_PATH + File.separator + folderName;
            String apkName = appName + ".apk";
            File folder = new File(folderPath);
            if (!folder.exists()) {
                if (!folder.mkdirs()) {
                    Log.w(TAG, "can not create folder!");
                }
            }
            File file = new File(folderPath, apkName);
            if (file.exists()) {
                Log.d(TAG, "检测到文件" + file.getName() + "已经存在，将要先删除以前的文件！");
                boolean success = file.delete();
                if (success) {
                    Log.d(TAG, "删除文件" + file.getName() + "成功！");

                } else {
                    Log.e(TAG, "保存文件失败，" + file.getName() + "已经存在并且删除失败！");
                    return;
                }
            }
            FileOutputStream fos = null;
            try {
                if (!file.createNewFile()) {
                    Log.w(TAG, "can not create new file " + file);
                }
                fos = new FileOutputStream(file);
                fos.write(apk);
                fos.flush();
            } catch (IOException e) {
                Log.e(TAG, "IOException occurs when saving wearable apk file!", e);
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            Log.d(TAG, "在应用[" + app.wearableAppName + "]中获取得到Wearable apk！");
            Log.e(TAG, "同步wearable应用[" + app.wearablePackage + ":" + app.wearableAppName + "], 应用大小为[" + apk.length + "]个字节");
            byte[] crcArray = CRCUtil.makeCrcToBytes(apk);
            Log.d(TAG, "CRC = " + Arrays.toString(crcArray));
            Asset asset = Asset.createFromBytes(apk);
            putDataMapRequest.getDataMap().putAsset("apk", asset);
            PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();

            Wearable.DataApi.putDataItem(mobvoiApiClient, putDataRequest).await();
        }
    }


    public void addMessageListener(IMessageListener listener, List<String> paths) {
        if (listener != null && paths != null && paths.size() > 0) {
            if (messageListeners.containsKey(listener)) {
                List<String> list = messageListeners.get(listener);
                list.addAll(paths);
            } else {
                messageListeners.put(listener, paths);
            }
        }
    }

    public void removeMessageListener(IMessageListener listener) {
        if (messageListeners.containsKey(listener)) {
            messageListeners.remove(listener);
        }

    }

    public void addDataListener(IDataListener listener, List<String> paths) {
        if (listener != null && paths != null && paths.size() > 0) {
            if (dataListeners.containsKey(listener)) {
                List<String> list = dataListeners.get(listener);
                list.addAll(paths);
            } else {
                dataListeners.put(listener, paths);
            }
        }
    }

    public void removeDataListener(IDataListener listener) {
        if (dataListeners.containsKey(listener)) {
            dataListeners.remove(listener);
        }
    }


    public void addNodeListener(INodeListener listener) {
        if (listener != null && !nodeListeners.contains(listener)) {
            nodeListeners.add(listener);
            //checkNodesConnected(listener);
        }

    }


    public void checkNodesConnected(final INodeListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult result = Wearable.NodeApi.getConnectedNodes(mobvoiApiClient).await();
                if (result != null) {
                    if (result.getStatus().isSuccess()) {
                        List<Node> nodes = result.getNodes();
                        if (nodes != null && !nodes.isEmpty()) {
                            listener.onHasNodesConnected();
                        } else {
                            listener.onNoNodeConnected();
                        }
                    } else {
                        listener.onGetNodeFailed();
                    }
                } else {
                    listener.onGetNodeFailed();
                }
            }
        }).start();

    }


    private void onHasNodesConnected() {
        for (INodeListener nodeListener : nodeListeners) {
            Log.d(TAG, "回调Node监听器：" + nodeListener.getClass().getName());
            nodeListener.onHasNodesConnected();
        }
    }

    private void onNoNodeConnected() {
        for (INodeListener nodeListener : nodeListeners) {
            Log.d(TAG, "回调Node监听器：" + nodeListener.getClass().getName());
            nodeListener.onNoNodeConnected();
        }
    }

    private void onGetNodesFailed() {
        for (INodeListener nodeListener : nodeListeners) {
            Log.d(TAG, "回调Node监听器：" + nodeListener.getClass().getName());
            nodeListener.onGetNodeFailed();
        }
    }


    private void onCmsConnected() {
        for (CmsServiceConnectionListener listener : cmsConnectionListeners) {
            Log.d(TAG, "CMS服务连接成功,回调连接成功监听：" + listener.getClass().getName());
            listener.onConnected();
        }
    }

    private void onCmsDisconnected() {
        for (CmsServiceConnectionListener listener : cmsConnectionListeners) {
            Log.d(TAG, "CMS服务连接被挂起,回调监听：" + listener.getClass().getName());
            listener.onDisconnected();
        }
    }

    public void removeNodeListener(INodeListener listener) {
        if (nodeListeners.contains(listener)) {
            nodeListeners.remove(listener);
        }
    }

    public void addConnectionListener(CmsServiceConnectionListener listener) {
        if (listener != null && !cmsConnectionListeners.contains(listener)) {
            cmsConnectionListeners.add(listener);
            if (mobvoiApiClient.isConnected()) {
                listener.onConnected();
            } else {
                listener.onDisconnected();
            }

        }
    }

    public void removeConnectionListener(CmsServiceConnectionListener listener) {
        if (cmsConnectionListeners.contains(listener)) {
            cmsConnectionListeners.remove(listener);
        }
    }


    private List<IMessageListener> getMessageListenerByPath(String path) {
        List<IMessageListener> listeners = new ArrayList<>();
        for (Map.Entry<IMessageListener, List<String>> entry : messageListeners.entrySet()) {
            if (entry.getValue().contains(path)) {
                listeners.add(entry.getKey());
            }
        }
        return listeners;
    }

    private List<IDataListener> getDataListenerByPath(String path) {
        List<IDataListener> listeners = new ArrayList<>();
        for (Map.Entry<IDataListener, List<String>> entry : dataListeners.entrySet()) {
            if (entry.getValue().contains(path)) {
                listeners.add(entry.getKey());
            }
        }
        return listeners;
    }


    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        Log.d(TAG, "onDataChanged");
        for (DataEvent event : dataEventBuffer) {
            DataItem dataItem = event.getDataItem();
            String path = dataItem.getUri().getPath();
            Log.d(TAG, String.format("Data Event Type [%s], Path [%s].", event.getType(), path));
            /*if (event.getType() == DataEvent.TYPE_CHANGED) {
                if (SyncMessagePathConfig.WEARABLE_APK.equals(path)) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                    new WearableApkSaver(SyncService.this, mobvoiApiClient, dataMapItem).start();
                }
            }*/
            Log.d(TAG, "Data Listener Size = " + dataListeners.size());
            List<IDataListener> listeners = getDataListenerByPath(path);
            Log.d(TAG, "Found " + listeners.size() + " data listeners for path " + path);
            for (IDataListener listener : listeners) {
                Log.d(TAG, "回调Data监听：" + listener.getClass().getName());
                listener.onDataChanged(path, dataItem, mobvoiApiClient);
            }
        }
    }

    public class SyncBinder extends Binder {
        public SyncService getService() {
            return SyncService.this;
        }
    }


    public interface ISendCallback {
        void onSendSuccess(SyncMessage message);

        void onSendFailed(SyncMessage message, int failedError);
    }

    public interface ISyncAppCallback {
        void onSyncComplete(int total);

        void onSyncBegin(int total);

        void onSyncPost(int index, int total, String packageName);
    }

}
