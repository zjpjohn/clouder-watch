package com.clouder.watch.mobile;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.clouder.watch.common.sync.IMessageListener;
import com.clouder.watch.common.sync.SyncMessage;
import com.clouder.watch.common.utils.StringUtils;
import com.clouder.watch.mobile.sync.CmsServiceConnectionListener;
import com.clouder.watch.mobile.sync.IDataListener;
import com.clouder.watch.mobile.sync.INodeListener;
import com.clouder.watch.mobile.utils.ToastUtils;
import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.wearable.DataItem;
import com.cms.android.wearable.Node;
import com.cms.android.wearable.NodeApi;

import java.util.List;

/**
 * Created by yang_shoulai on 8/5/2015.
 */
public class SyncActivity extends BaseActivity implements SyncService.ISendCallback,
        IMessageListener,
        IDataListener,
        INodeListener,
        CmsServiceConnectionListener {

    private String TAG = getClass().getSimpleName();

    private SyncService syncService;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Connected with sync service!");
            syncService = ((SyncService.SyncBinder) service).getService();
            if (messageListenerPaths() != null) {
                Log.d(TAG, "add message listener with paths size " + messageListenerPaths().size());
                syncService.addMessageListener(SyncActivity.this, messageListenerPaths());
            }
            if (dataListenerPaths() != null) {
                Log.d(TAG, "add data listener with paths size " + dataListenerPaths().size());
                syncService.addDataListener(SyncActivity.this, dataListenerPaths());
            }

            syncService.addNodeListener(SyncActivity.this);
            syncService.addConnectionListener(SyncActivity.this);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onConnectSyncServiceSuccess();
                }
            });

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.w(TAG, "Disconnected with sync service!");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onConnectSyncServiceFailed();

                }
            });

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart!!!");
        bindService(new Intent(this, SyncService.class), connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop!!!");
        if (syncService != null) {
            syncService.removeMessageListener(this);
            syncService.removeDataListener(this);
            syncService.removeNodeListener(this);
            syncService.removeConnectionListener(this);
            unbindService(connection);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    protected final void sendMessage(final SyncMessage message) {
        if (syncService == null) {
            ToastUtils.show(this, "发送失败，同步服务未连接");
            onSendFailed(message, -1);
            return;
        }
        if (message == null) {
            Log.e(TAG, "Can not send a null message!");
            ToastUtils.show(this, "发送失败，空消息");
            onSendFailed(null, -1);
            return;
        }
        final String path = message.getPath();
        if (StringUtils.isEmpty(path)) {
            Log.e(TAG, "Send failed as path is empty!");
            ToastUtils.show(this, "发送失败，消息Path为空");
            onSendFailed(message, -1);
            return;
        }

        syncService.sendMessage(message, SyncActivity.this);
        /*new Thread(new Runnable() {
            @Override
            public void run() {
                syncService.sendMessage(message, SyncActivity.this);
            }
        }).start();*/

    }

    /**
     * 连接至同步服务
     */
    public void onConnectSyncServiceSuccess() {

    }

    /**
     * 连接同步服务失败
     */
    public void onConnectSyncServiceFailed() {
        ToastUtils.show(this, "发送失败，同步服务未连接");
    }

    @Override
    public void onSendSuccess(SyncMessage syncMessage) {

    }

    @Override
    public void onSendFailed(SyncMessage syncMessage, final int errorCode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (errorCode == SyncService.SEND_MSG_FAILED_CMS_NOT_CONNECTED) {
                    ToastUtils.show(SyncActivity.this, "发送失败，CMS服务未连接");
                } else if (errorCode == SyncService.SEND_MSG_FAILED_NO_NODES_FOUND) {
                    ToastUtils.show(SyncActivity.this, "发送失败，设备未连接");

                } else {
                    ToastUtils.show(SyncActivity.this, "发送失败，未知原因");
                }
            }
        });

    }


    @Override
    public void onMessageReceived(String path, SyncMessage message) {

    }

    public List<String> messageListenerPaths() {
        return null;
    }

    public List<String> dataListenerPaths() {
        return null;
    }

    @Override
    public void onDataChanged(String path, DataItem dataItem, MobvoiApiClient client) {

    }

    protected void syncWearableApps(SyncService.ISyncAppCallback callback) {
        this.syncService.syncAllApps(callback);
    }


    /**
     * CMS服务已连接
     */
    @Override
    public void onConnected() {

    }


    /**
     * CMS服务未连接
     */
    @Override
    public void onDisconnected() {

    }


    /**
     * 无设备连接
     */
    @Override
    public void onNoNodeConnected() {

    }


    /**
     * 有设备连接
     */
    @Override
    public void onHasNodesConnected() {

    }

    /**
     * 获取连接设备失败
     */
    @Override
    public final void onGetNodeFailed() {
        onNoNodeConnected();
    }

    /**
     * 检查当前是否有设备连接
     * @param listener
     */
    public void checkNodesConnected(INodeListener listener) {
        syncService.checkNodesConnected(listener);
    }
}
