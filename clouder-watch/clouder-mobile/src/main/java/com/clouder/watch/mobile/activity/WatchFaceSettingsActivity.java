package com.clouder.watch.mobile.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.clouder.watch.common.sync.SyncMessage;
import com.clouder.watch.common.sync.SyncMessagePathConfig;
import com.clouder.watch.common.sync.message.CurrentWatchFaceSyncMessage;
import com.clouder.watch.common.sync.message.WatchFaceSyncMessage;
import com.clouder.watch.common.utils.Constant;
import com.clouder.watch.common.utils.StringUtils;
import com.clouder.watch.mobile.ClouderApplication;
import com.clouder.watch.mobile.R;
import com.clouder.watch.mobile.SyncActivity;
import com.clouder.watch.mobile.sync.INodeListener;
import com.clouder.watch.mobile.utils.ImageUtils;
import com.clouder.watch.mobile.utils.ToastUtils;
import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.wearable.Asset;
import com.cms.android.wearable.DataItem;
import com.cms.android.wearable.DataMap;
import com.cms.android.wearable.DataMapItem;
import com.cms.android.wearable.Node;
import com.cms.android.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by yang_shoulai on 7/27/2015.
 */
public class WatchFaceSettingsActivity extends SyncActivity implements AdapterView.OnItemClickListener {

    private static final String TAG = "WatchFaceActivity";

    private GridView gridView;

    private WatchFaceAdapter adapter;

    private List<WatchFaceInfo> watchFaces = new ArrayList<>();

    private LayoutInflater inflater;

    private String currentWatchFacePkg = "";

    private String currentWatchFaceService = "";

    private WatchFaceLoader watchFaceLoader = new WatchFaceLoader();

    private ProgressDialog progressDialog;

    private boolean flag = false;

    private int total;

    private int count;

    private Handler uiHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inflater = LayoutInflater.from(this);
        setContentView(R.layout.activity_watch_face_center);
        setActionBarTitle(R.string.watchface_center_title);
        gridView = (GridView) findViewById(R.id.watch_face_grid_view);
        adapter = new WatchFaceAdapter();
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(this);
        currentWatchFacePkg = ClouderApplication.getInstance().getCurrentWatchFacePackage();
        currentWatchFaceService = ClouderApplication.getInstance().getCurrentWatchFaceService();
        loadFromCache();
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        WatchFaceInfo watchFace = watchFaces.get(position);
        CurrentWatchFaceSyncMessage message = new CurrentWatchFaceSyncMessage(Constant.CLOUDER_SETTINGS_PKG, SyncMessagePathConfig.CURRENT_WATCH_FACE);
        message.setMethod(SyncMessage.Method.Set);
        message.setPackageName(watchFace.packageName);
        message.setServiceName(watchFace.serviceName);
        sendMessage(message);
    }


    private class WatchFaceAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return watchFaces == null ? 0 : watchFaces.size();
        }

        @Override
        public Object getItem(int position) {
            return watchFaces.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = inflater.inflate(R.layout.item_watch_face_center, null);
            String pkg = watchFaces.get(position).packageName;
            String service = watchFaces.get(position).serviceName;
            ImageView imageView = (ImageView) view.findViewById(R.id.imageView);
            TextView textView = (TextView) view.findViewById(R.id.watchFaceName);
            imageView.setImageBitmap(watchFaces.get(position).thumbnail);
            textView.setText(watchFaces.get(position).name);
            if (currentWatchFacePkg.equals(pkg) && currentWatchFaceService.equals(service)) {
                //特殊处理展示当前正在使用的表盘

                textView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }
            return view;
        }
    }

    @Override
    public void onConnectSyncServiceSuccess() {
        Log.d(TAG, "onConnectSyncServiceSuccess");
        if (flag) {
            return;
        } else {
            flag = true;
        }
        checkNodesConnected(new INodeListener() {
            @Override
            public void onNoNodeConnected() {
                WatchFaceSettingsActivity.this.onNoNodeConnected();
            }

            @Override
            public void onHasNodesConnected() {
                WatchFaceSyncMessage watchFaceSyncMessage = new WatchFaceSyncMessage(Constant.CLOUDER_SETTINGS_PKG, SyncMessagePathConfig.WATCH_FACES);
                watchFaceSyncMessage.setMethod(SyncMessage.Method.Get);
                watchFaceSyncMessage.setNeedSync(false);
                sendMessage(watchFaceSyncMessage);

                CurrentWatchFaceSyncMessage currentWatchFaceSyncMessage = new CurrentWatchFaceSyncMessage(Constant.CLOUDER_SETTINGS_PKG, SyncMessagePathConfig.CURRENT_WATCH_FACE);
                currentWatchFaceSyncMessage.setMethod(SyncMessage.Method.Get);
                sendMessage(currentWatchFaceSyncMessage);
            }

            @Override
            public void onGetNodeFailed() {
                onNoNodeConnected();
            }
        });


    }

    @Override
    public void onSendSuccess(SyncMessage syncMessage) {
        super.onSendSuccess(syncMessage);
    }

    @Override
    public void onSendFailed(SyncMessage syncMessage, int errorCode) {
        if (syncMessage instanceof WatchFaceSyncMessage) {
            //final List<ClouderApplication.WatchFace> faces = ClouderApplication.getInstance().getWatchFaces();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                   /* String message = "同步表盘失败";
                    if (faces != null && faces.isEmpty()) {
                        message = "同步表盘失败,展现缓存的表盘信息";
                    }
                    ToastUtils.show(WatchFaceSettingsActivity.this, message);*/
                    Log.e(TAG, "发送表盘同步消息失败！");
                }
            });
        }
    }

    @Override
    public void onMessageReceived(String path, SyncMessage syncMessage) {
        if (syncMessage instanceof CurrentWatchFaceSyncMessage) {
            CurrentWatchFaceSyncMessage message = (CurrentWatchFaceSyncMessage) syncMessage;
            currentWatchFacePkg = message.getPackageName();
            currentWatchFaceService = message.getServiceName();
            ClouderApplication.getInstance().setCurrentWatchFacePackage(currentWatchFacePkg);
            ClouderApplication.getInstance().setCurrentWatchFaceService(currentWatchFaceService);
            Log.d(TAG, "Current Watch Face, package = " + currentWatchFacePkg + ", service = " + currentWatchFaceService);
            adapter.notifyDataSetChanged();
        } else if (syncMessage instanceof WatchFaceSyncMessage) {
            List<WatchFaceSyncMessage.WatchFace> watchFaces = ((WatchFaceSyncMessage) syncMessage).getWatchFaces();
            int count = watchFaces == null ? 0 : watchFaces.size();
            ToastUtils.show(WatchFaceSettingsActivity.this, "共发现" + count + "个表盘！");
            final List<ClouderApplication.WatchFace> faces = ClouderApplication.getInstance().getWatchFaces();
            boolean same = true;
            if (faces == null || faces.size() != count) {
                if (count == 0) {
                    same = true;
                    if (WatchFaceSettingsActivity.this.watchFaces != null) {
                        WatchFaceSettingsActivity.this.watchFaces.clear();
                        adapter.notifyDataSetChanged();
                    }
                } else {
                    same = false;
                }
            } else {

                if (watchFaces != null && count != 0) {
                    for (int i = 0; i < count; i++) {
                        WatchFaceSyncMessage.WatchFace wf1 = watchFaces.get(i);
                        boolean hasWf = false;
                        for (int j = 0; j < count; j++) {
                            ClouderApplication.WatchFace wf2 = faces.get(j);
                            if (wf1.getPackageName().equals(wf2.getPackageName()) && wf1.getServiceName().equals(wf2.getServiceName())) {
                                hasWf = true;
                                break;
                            }
                        }
                        if (!hasWf) {
                            same = false;
                            break;
                        }
                    }
                } else {
                    same = true;
                    if (WatchFaceSettingsActivity.this.watchFaces != null) {
                        WatchFaceSettingsActivity.this.watchFaces.clear();
                        adapter.notifyDataSetChanged();
                    }

                }

            }
            if (same) {
                //表盘无需更新，使用缓存
                //loadFromCache();
            } else {
                //表盘有更新,需要重新加载
                //清空缓存的表盘信息，等待重新加载
                ClouderApplication.getInstance().clearWatchFace();
                WatchFaceSettingsActivity.this.watchFaces.clear();
                adapter.notifyDataSetChanged();
                if (progressDialog == null) {
                    progressDialog = new ProgressDialog(this);
                    progressDialog.setCancelable(true);
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    progressDialog.setCanceledOnTouchOutside(true);
                    progressDialog.setMessage("正在更新表盘...");
                }
                progressDialog.show();
                WatchFaceSyncMessage watchFaceSyncMessage = new WatchFaceSyncMessage(Constant.CLOUDER_SETTINGS_PKG, SyncMessagePathConfig.WATCH_FACES);
                watchFaceSyncMessage.setMethod(SyncMessage.Method.Get);
                watchFaceSyncMessage.setNeedSync(true);
                sendMessage(watchFaceSyncMessage);
            }
            total = count;
        }
    }

    @Override
    public List<String> messageListenerPaths() {
        return Arrays.asList(SyncMessagePathConfig.CURRENT_WATCH_FACE, SyncMessagePathConfig.WATCH_FACES);
    }

    @Override
    public void onDataChanged(final String path, final DataItem dataItem, final MobvoiApiClient client) {
        super.onDataChanged(path, dataItem, client);
        Log.d(TAG, "onDataChanged path = " + path);
        if (SyncMessagePathConfig.WATCH_FACES.equals(path)) {
            count++;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (count <= total) {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.setMessage("同步第" + count + "个表盘，总共" + total + "个");
                        }
                    }

                    if (count == total) {
                        progressDialog.dismiss();
                        total = 0;
                        count = 0;
                    }
                }
            });
            DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
            DataMap map = dataMapItem.getDataMap();
            final String name = map.getString("name");
            String packageName = map.getString("packageName");
            String serviceName = map.getString("serviceName");
            if (exist(packageName, serviceName)) {
                return;
            }
            Bitmap thumbnail = null;
            Asset asset = map.getAsset("thumbnail");
            Log.d(TAG, String.format("Load watch face name [%s], package [%s], service[%s].", name, packageName, serviceName));
            if (StringUtils.isEmpty(packageName) || StringUtils.isEmpty(serviceName)) {
                return;
            }
            InputStream assetInputStream = Wearable.DataApi.getFdForAsset(client, asset).await().getInputStream();
            if (assetInputStream == null) {
                Log.w(TAG, "Load watch face thumbnail but inputStream is null !");
            } else {
                ByteArrayOutputStream outstream = new ByteArrayOutputStream();
                byte[] cache = new byte[1024];
                int readLength = -1;
                int size = 0;
                try {
                    while ((readLength = assetInputStream.read(cache)) != -1) {
                        outstream.write(cache, 0, readLength);
                        size += readLength;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                byte[] bytes = outstream.toByteArray();
                thumbnail = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                try {
                    Log.d(TAG, String.format("Load watch face name [%s], package [%s], service[%s], assertInputStream size = [%s], thumbnail size = [%s].", name, packageName, serviceName, assetInputStream.available(), thumbnail == null ? 0 : thumbnail.getByteCount()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    outstream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    assetInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            final WatchFaceInfo info = new WatchFaceInfo();
            info.packageName = packageName;
            info.serviceName = serviceName;
            info.name = name;
            info.thumbnail = thumbnail;
            if (exist(info)) {
                return;
            }
            watchFaces.add(info);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();

                }
            });
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (watchFaceLoader.save(info.packageName, info.serviceName, info.name, ImageUtils.bitmap2bytes(info.thumbnail))) {
                        Log.d(TAG, "保存边表盘package = " + info.packageName + ", service = " + info.serviceName + ", name = " + name + "成功");
                        com.clouder.watch.mobile.ClouderApplication.WatchFace watchFace = new com.clouder.watch.mobile.ClouderApplication.WatchFace();
                        watchFace.setPackageName(info.packageName);
                        watchFace.setServiceName(info.serviceName);
                        watchFace.setName(info.name);
                        ClouderApplication.getInstance().addWatchFaces(watchFace);
                    } else {
                        Log.d(TAG, "保存边表盘package = " + info.packageName + ", service = " + info.serviceName + ", name = " + name + "失败");
                    }
                }
            }).start();
        }


    }

    @Override
    public List<String> dataListenerPaths() {
        return Arrays.asList(SyncMessagePathConfig.WATCH_FACES);
    }


    private boolean exist(String packageName, String serviceName) {
        if (watchFaces == null || watchFaces.size() == 0) {
            return false;
        }

        for (WatchFaceInfo info : watchFaces) {
            if (info.packageName.equals(packageName) && info.serviceName.equals(serviceName)) {
                return true;
            }
        }
        return false;
    }

    public static class WatchFaceInfo {

        public String name;

        public String packageName;

        public String serviceName;

        public Bitmap thumbnail;


    }

    @Override
    public void onConnectSyncServiceFailed() {
        super.onConnectSyncServiceFailed();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }


    @Override
    public void onNoNodeConnected() {
        super.onNoNodeConnected();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtils.show(WatchFaceSettingsActivity.this, "设备连接断开");
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void loadFromCache() {
        final List<ClouderApplication.WatchFace> faces = ClouderApplication.getInstance().getWatchFaces();
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (faces != null && !faces.isEmpty()) {
                    for (ClouderApplication.WatchFace face : faces) {
                        final WatchFaceInfo watchFaceInfo = new WatchFaceInfo();
                        watchFaceInfo.packageName = face.getPackageName();
                        watchFaceInfo.serviceName = face.getServiceName();
                        watchFaceInfo.name = face.getName();
                        if (exist(watchFaceInfo)) {
                            continue;
                        }
                        watchFaceInfo.thumbnail = watchFaceLoader.load(watchFaceInfo.packageName, watchFaceInfo.serviceName, watchFaceInfo.name);
                        if (watchFaceInfo.thumbnail != null) {
                            watchFaces.add(watchFaceInfo);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    adapter.notifyDataSetChanged();
                                }
                            });
                        }

                    }

                }
            }
        }).start();
    }

    private boolean exist(WatchFaceInfo watchFaceInfo) {
        for (WatchFaceInfo info : watchFaces) {
            if (info.packageName.equals(watchFaceInfo.packageName)
                    && info.serviceName.equals(watchFaceInfo.serviceName)
                    && info.name.equals(watchFaceInfo.name)) {
                return true;
            }
        }
        return false;
    }


    static class WatchFaceLoader {

        private static final String TAG = "WatchFaceLoader";

        public static final String SDCARD_PATH = Environment.getExternalStorageDirectory().getPath() +
                File.separator + "ClouderWatch" + File.separator + "watchfaces";

        public Bitmap load(String packageName, String serviceName, String name) {
            if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                Log.e(TAG, "SD 卡不可用！");
                return null;
            }
            String path = SDCARD_PATH + File.separator + packageName + File.separator + serviceName + File.separator + name;
            File file = new File(path);
            if (!file.exists()) {
                Log.e(TAG, "表盘不存在，Path = " + path);
                return null;
            }
            return BitmapFactory.decodeFile(path);
        }

        public boolean save(String packageName, String serviceName, String name, byte[] data) {
            if (StringUtils.isEmpty(packageName) || StringUtils.isEmpty(serviceName) || StringUtils.isEmpty(name)) {
                Log.e(TAG, String.format("无法保存表盘,PackageName = %s, ServiceName = %s, Name = %s", packageName, serviceName, name));
                return false;
            }
            if (data == null) {
                Log.e(TAG, String.format("表盘数据为NULL"));
                return false;
            }
            if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                Log.e(TAG, "SD 卡不可用！");
                return false;
            }
            String path = SDCARD_PATH + File.separator + packageName + File.separator + serviceName;
            File file = new File(path);
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    Log.e(TAG, "无法创建文件夹 Path = " + path);
                    return false;
                }
            }
            File tmp = new File(file, name);
            if (tmp.exists()) {
                if (!tmp.delete()) {
                    Log.e(TAG, "无法删除文件 path = " + tmp.getAbsolutePath());
                    return false;
                }
            }
            FileOutputStream fos = null;

            try {
                if (tmp.createNewFile()) {
                    fos = new FileOutputStream(tmp);
                    fos.write(data);
                    fos.flush();
                    return true;
                } else {
                    Log.e(TAG, "无法新建文件 path = " + tmp.getAbsolutePath());
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }

    }


}
