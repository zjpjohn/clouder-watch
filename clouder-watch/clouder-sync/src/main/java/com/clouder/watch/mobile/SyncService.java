package com.clouder.watch.mobile;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.clouder.watch.common.sync.IMessageListener;
import com.clouder.watch.common.sync.SyncMessage;
import com.clouder.watch.common.sync.SyncMessageParser;
import com.clouder.watch.common.sync.SyncMessagePathConfig;
import com.clouder.watch.common.sync.SyncServiceHelper;
import com.clouder.watch.common.sync.message.WatchFaceSyncMessage;
import com.clouder.watch.common.utils.StringUtils;
import com.clouder.watch.mobile.handler.CurrentTimeHandler;
import com.clouder.watch.mobile.handler.CurrentWatchFaceHandler;
import com.clouder.watch.mobile.handler.NotificationBlackListHandler;
import com.clouder.watch.mobile.handler.NotificationSettingsHandler;
import com.clouder.watch.mobile.handler.ScreenLockerHandler;
import com.clouder.watch.mobile.handler.SocketConnectHandler;
import com.clouder.watch.mobile.handler.UninstallApkHandler;
import com.clouder.watch.mobile.handler.WatchFacesHandler;
import com.clouder.watch.mobile.handler.WifiHandler;
import com.clouder.watch.mobile.notification.SyncNotificationService;
import com.cms.android.common.ConnectionResult;
import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.common.api.ResultCallback;
import com.cms.android.common.api.Status;
import com.cms.android.wearable.Asset;
import com.cms.android.wearable.DataApi;
import com.cms.android.wearable.DataEvent;
import com.cms.android.wearable.DataEventBuffer;
import com.cms.android.wearable.DataItem;
import com.cms.android.wearable.DataMapItem;
import com.cms.android.wearable.MessageApi;
import com.cms.android.wearable.MessageEvent;
import com.cms.android.wearable.Node;
import com.cms.android.wearable.NodeApi;
import com.cms.android.wearable.PutDataMapRequest;
import com.cms.android.wearable.PutDataRequest;
import com.cms.android.wearable.Wearable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据同步服务
 * 主要用于接收手机端Clouder助手传递过来的数据或者向手机端APP发送数据
 * 如是否打开通知推送，是否启用锁屏等
 * 服务会在系统开机完成后启动
 * Created by yang_shoulai on 8/4/2015.
 */
public class SyncService extends Service implements MobvoiApiClient.OnConnectionFailedListener, MobvoiApiClient.ConnectionCallbacks,

        MessageApi.MessageListener, NodeApi.NodeListener, DataApi.DataListener {

    private static final String TAG = "SyncService";

    private static final int CONNECTION_SLEEP = 10000;

    private MobvoiApiClient mobvoiApiClient;

    public Map<IMessageListener, List<String>> listeners;

    public Map<Messenger, List<String>> messengerListeners;

    private Handler mIncomingHandler = new IncomingHandler();

    private Messenger mMessenger = new Messenger(mIncomingHandler);

    private int flag = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate()...");
        Log.i(TAG, "Building mobvoi api client...");
        listeners = new ConcurrentHashMap<>();
        messengerListeners = new ConcurrentHashMap<>();
        registerListener(new CurrentWatchFaceHandler(this), Arrays.asList(SyncMessagePathConfig.CURRENT_WATCH_FACE));
        registerListener(new NotificationBlackListHandler(this), Arrays.asList(SyncMessagePathConfig.NOTIFICATION_BLACK_LIST));
        registerListener(new NotificationSettingsHandler(this), Arrays.asList(SyncMessagePathConfig.NOTIFICATION_SETTINGS));
        registerListener(new ScreenLockerHandler(this), Arrays.asList(SyncMessagePathConfig.SCREEN_LOCKER_SETTINGS));
        registerListener(new WatchFacesHandler(this), Arrays.asList(SyncMessagePathConfig.WATCH_FACES));
        registerListener(new WifiHandler(), Arrays.asList(SyncMessagePathConfig.WIFI_INFO));
        registerListener(new SocketConnectHandler(this), Arrays.asList(SyncMessagePathConfig.SOCKET_CONNECTED));
        registerListener(new UninstallApkHandler(this), Arrays.asList(SyncMessagePathConfig.UNINSTALL_APK));
        registerListener(new CurrentTimeHandler(this), Arrays.asList(SyncMessagePathConfig.CURRENT_TIME));
        mobvoiApiClient = new MobvoiApiClient.Builder(this).addApi(Wearable.API).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();
        Log.i(TAG, "Start to connect with cms service...");
        //mobvoiApiClient.connect();

        new Thread(new Runnable() {
            @Override
            public void run() {
                startService(new Intent(SyncService.this, CallMessageListenerService.class));
                startService(new Intent(SyncService.this, SyncContactsListenerService.class));
                startService(new Intent(SyncService.this, SyncNotificationService.class));
            }
        }).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand()");
        if (!mobvoiApiClient.isConnected()) {
            mobvoiApiClient.connect();
        }
        //return super.onStartCommand(intent, flags, startId);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy()");
        Log.i(TAG, "Mobvoi api client will disconnect with cms service");
        listeners.clear();
        messengerListeners.clear();
        Wearable.MessageApi.removeListener(mobvoiApiClient, this);
        Wearable.NodeApi.removeListener(mobvoiApiClient, this);
        Wearable.DataApi.removeListener(mobvoiApiClient, this);
        mobvoiApiClient.disconnect();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Mobvoi api client connected to cms service!");
        Log.i(TAG, "Register listener to cms service, then we can receive or send message to phone devices");
        Wearable.MessageApi.addListener(mobvoiApiClient, this).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    Log.i(TAG, "Register listener to cms service success!");
                } else {
                    Log.e(TAG, "Register listener to cms service failed! something may be wrong! client will try reconnect!");
                }
            }
        });

        Wearable.DataApi.addListener(mobvoiApiClient, SyncService.this).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    Log.d(TAG, "add data listener success!");

                } else {
                    Log.e(TAG, "add data listener failed!");
                }
            }
        });
        Wearable.NodeApi.addListener(mobvoiApiClient, SyncService.this).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    Log.d(TAG, "add node listener success!");

                } else {
                    Log.e(TAG, "add node listener failed!");
                }
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "Mobvoi api client connection was suspended! mobvoi api client will try reconnect! ");
        reconnect();
    }

    /**
     * 接收手机端传递过来的数据
     *
     * @param messageEvent
     */
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        String node = messageEvent.getSourceNodeId();
        String path = messageEvent.getPath();
        byte[] data = messageEvent.getData();
        Log.i(TAG, String.format("Receive message from handled devices with package name [%s] and path [%s]", node, path));
        SyncMessage message = SyncMessageParser.parse(path, data);
        if (message == null) {
            Log.e(TAG, "Can not parse message as message is null or path is not correct.");
            return;
        }
        List<IMessageListener> listeners = getListenerByPath(path);
        for (IMessageListener listener : listeners) {
            Log.d(TAG, "回调接口监听" + listener.getClass().getName() + ", path = " + path);
            listener.onMessageReceived(path, message);
        }
        List<Messenger> messengers = getMessengerListenerByPath(path);
        Message msg = Message.obtain();
        msg.getData().putByteArray(SyncServiceHelper.KEY_SYNC_MESSAGE_IN_DATA, message.toBytes());
        msg.getData().putString(SyncServiceHelper.KEY_SYNC_MESSAGE_PATH, path);
        for (Messenger messenger : messengers) {
            try {
                messenger.send(msg);
            } catch (DeadObjectException e1) {
                messengerListeners.remove(messenger);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Mobvoi api client can not connect to cms service, and result is: " + connectionResult);
        if (connectionResult != null && connectionResult.getErrorCode() == 9) {
            reconnect();
        }
    }


    public synchronized void sendMessage(final SyncMessage message) {
        sendMessage(message, null);
    }

    /**
     * 向手机端发送消息
     *
     * @param message 消息内容
     */
    public synchronized void sendMessage(final SyncMessage message, final Messenger client) {
        Log.d(TAG, "Send Message " + message.toString());
        final Message msg = Message.obtain();
        msg.getData().putByteArray(SyncServiceHelper.KEY_SYNC_MESSAGE_IN_DATA, message.toBytes());
        msg.getData().putString(SyncServiceHelper.KEY_SYNC_MESSAGE_PATH, message.getPath());
        if (!mobvoiApiClient.isConnected()) {
            Log.e(TAG, "Mobvoi api client disconnected to cms service and will try reconnect!");
            msg.what = SyncServiceHelper.STATUS_FAILED_CMS_SERVICE_DISCONNECTED;
            replyMessage(msg, client);

            reconnect();
            return;
        }
        final String path = message.getPath();
        if (StringUtils.isEmpty(path)) {
            Log.e(TAG, "Send failed as either package name or path is empty!");
            msg.what = SyncServiceHelper.STATUS_FAILED_UNKNOWN_ERROR;
            replyMessage(msg, client);
            return;
        }
        Log.d(TAG, "************");
        NodeApi.GetConnectedNodesResult result = Wearable.NodeApi.getConnectedNodes(mobvoiApiClient).await();
        if (result != null) {
            if (result.getStatus().isSuccess()) {
                Log.d(TAG, "get Connected Nodes success!");
                List<Node> nodes = result.getNodes();
                if (nodes != null && !nodes.isEmpty()) {
                    for (Node node : nodes) {
                        Log.d(TAG, "Send message to node " + node.getId());
                        Wearable.MessageApi.sendMessage(mobvoiApiClient, node.getId(), path, message.toBytes())
                                .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                                    @Override
                                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                                        if (sendMessageResult.getStatus().isSuccess()) {
                                            Log.i(TAG, String.format("Message with path [%s] was sent successfully!", path));
                                            msg.what = SyncServiceHelper.STATUS_SUCCESS;
                                            replyMessage(msg, client);
                                        } else {
                                            Log.i(TAG, String.format("Send Message with  path [%s] failed!", path));
                                            msg.what = SyncServiceHelper.STATUS_FAILED_UNKNOWN_ERROR;
                                            replyMessage(msg, client);
                                        }
                                    }
                                });
                    }
                } else {
                    Log.d(TAG, "Send failed, no nodes found!");
                    msg.what = SyncServiceHelper.STATUS_FAILED_CMS_SERVICE_DISCONNECTED;
                    replyMessage(msg, client);
                }

            } else {

                Log.e(TAG, "get Connected Nodes failed!");
                msg.what = SyncServiceHelper.STATUS_FAILED_CMS_SERVICE_DISCONNECTED;
                replyMessage(msg, client);
            }
        } else {
            Log.e(TAG, "get Connected Nodes result null!");
            msg.what = SyncServiceHelper.STATUS_FAILED_CMS_SERVICE_DISCONNECTED;
            replyMessage(msg, client);
        }
    }

    @Override
    public void onPeerConnected(Node node) {
        Log.d(TAG, "node " + node.getId() + " onPeerConnected");
    }

    @Override
    public void onPeerDisconnected(Node node) {
        Log.e(TAG, "node " + node.getId() + " onPeerDisconnected");
    }

    public synchronized void registerListener(IMessageListener listener, List<String> paths) {
        listeners.put(listener, paths);

    }


    public List<IMessageListener> getListenerByPath(String path) {
        List<IMessageListener> list = new ArrayList<>();
        for (Map.Entry<IMessageListener, List<String>> entry : listeners.entrySet()) {
            if (entry.getValue().contains(path)) {
                list.add(entry.getKey());
            }
        }
        return list;
    }

    public List<Messenger> getMessengerListenerByPath(String path) {
        List<Messenger> list = new ArrayList<>();
        for (Map.Entry<Messenger, List<String>> entry : messengerListeners.entrySet()) {
            if (entry.getValue().contains(path)) {
                list.add(entry.getKey());
            }
        }
        return list;
    }


    /**
     * 重连
     */
    public void reconnect() {
        Wearable.MessageApi.removeListener(mobvoiApiClient, this);
        Wearable.NodeApi.removeListener(mobvoiApiClient, this);
        Wearable.DataApi.removeListener(mobvoiApiClient, this);
        mobvoiApiClient.disconnect();
        Log.e(TAG, "Mobvoi api client will try connect again " + CONNECTION_SLEEP / 1000
                + " seconds later!" + " flag = " + flag++ + ", thread =" + Thread.currentThread().getName());

        mIncomingHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mobvoiApiClient.connect();
            }
        }, CONNECTION_SLEEP);
    }

    public void replyMessage(Message message, Messenger messenger) {
        if (messenger == null) {
            return;
        }
        try {
            messenger.send(message);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException", e);
        }
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
            if (SyncMessagePathConfig.WEARABLE_APK.equals(path)) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                new WearableApkSaver(SyncService.this, mobvoiApiClient, dataMapItem).start();
            }
        }
    }

    /**
     * 同步所有表盘信息
     *
     * @param list
     */
    public void syncWatchFaces(final List<WatchFaceSyncMessage.WatchFace> list) {
        new Thread() {
            @Override
            public void run() {
                Log.d(TAG, "Start sync " + list.size() + " watch faces!");
                if (list != null && !list.isEmpty()) {
                    for (WatchFaceSyncMessage.WatchFace watchFace : list) {
                        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(SyncMessagePathConfig.WATCH_FACES);
                        putDataMapRequest.getDataMap().putString("name", watchFace.getName());
                        putDataMapRequest.getDataMap().putString("packageName", watchFace.getPackageName());
                        putDataMapRequest.getDataMap().putString("serviceName", watchFace.getServiceName());
                        Asset asset = Asset.createFromBytes(watchFace.getThumbnail());
                        putDataMapRequest.getDataMap().putAsset("thumbnail", asset);
                        PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();
                        Log.d(TAG, String.format("Sync watch face name [%s], package [%s], service [%s].", watchFace.getName(), watchFace.getPackageName(), watchFace.getServiceName()));
                        Wearable.DataApi.putDataItem(mobvoiApiClient, putDataRequest);
                    }

                }
                Log.d(TAG, "Sync watch faces complete!");
            }
        }.start();

    }

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int what = msg.what;
            Messenger client = msg.replyTo;
            String path = msg.getData().getString(SyncServiceHelper.KEY_SYNC_MESSAGE_PATH);
            if (what == SyncServiceHelper.MSG_TYPE_SEND_MESSAGE) {
                SyncMessage message = SyncMessageParser.parse(path, msg.getData().getByteArray(SyncServiceHelper.KEY_SYNC_MESSAGE_IN_DATA));
                if (message != null) {
                    SyncService.this.sendMessage(message, client);
                }
            } else if (what == SyncServiceHelper.MSG_TYPE_REGISTER_LISTENER) {
                messengerListeners.put(client, Arrays.asList(path));
            } else if (what == SyncServiceHelper.MSG_TYPE_UNREGISTER_LISTENER) {
                messengerListeners.remove(client);
            }

        }
    }
}
