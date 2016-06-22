package com.clouder.watch.mobile.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.clouder.watch.common.sync.SyncMessage;
import com.clouder.watch.common.sync.SyncMessagePathConfig;
import com.clouder.watch.common.sync.message.CurrentTimeSyncMessage;
import com.clouder.watch.mobile.ClouderApplication;
import com.clouder.watch.mobile.R;
import com.clouder.watch.mobile.SyncActivity;
import com.clouder.watch.mobile.SyncService;
import com.clouder.watch.mobile.utils.StringUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;


public class MainActivity extends SyncActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    /**
     * 启用通知拦截处理的app
     */
    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    /**
     * 跳转到通知设置的activity action
     */
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

    private ClouderApplication application;

    private TextView tvConnectState;

    private View manageNotification;

    private View manageWatchFace;

    private View manageConnect;

    private View manageLocker;

    private View manageAbout;

    private AlertDialog alertDialog;

    private Handler handler = new Handler();

    private boolean firstStart;

    private ProgressDialog syncProgressDialog;


    private boolean testConnecting = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        application = (ClouderApplication) getApplication();
        if (!isPaired()) {
            Log.d(TAG, "mobile phone has no bind watch, application will turn to first user page for pairing.");
            startActivity(new Intent(this, FirstUseActivity.class));
            finish();
        } else {
            setContentView(R.layout.activity_main);
            hideActionBar();
            tvConnectState = (TextView) findViewById(R.id.connected_state);
            manageNotification = findViewById(R.id.manage_notification);
            manageWatchFace = findViewById(R.id.manage_watchface);
            manageConnect = findViewById(R.id.manage_device_connect);
            manageLocker = findViewById(R.id.manage_locker);
            manageAbout = findViewById(R.id.manage_about);
            manageNotification.setOnClickListener(this);
            manageWatchFace.setOnClickListener(this);
            manageConnect.setOnClickListener(this);
            manageLocker.setOnClickListener(this);
            manageAbout.setOnClickListener(this);
            if (!isNotificationAccessEnabled()) {
                showConfirmDialog();
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.manage_notification:
                startActivity(new Intent(this, NotificationSettingsActivity.class));
                break;
            case R.id.manage_watchface:
                startActivity(new Intent(this, WatchFaceSettingsActivity.class));
                break;
            case R.id.manage_device_connect:
                startActivity(new Intent(this, DeviceConnectActivity.class));
                break;
            case R.id.manage_locker:
                Intent i = new Intent(this, LockerSettingsActivity.class);
                i.putExtra("bleStatus", tvConnectState.getText().toString());
                startActivity(i);
                break;
            case R.id.manage_about:
                startActivity(new Intent(this, AboutActivity.class));
                break;
            default:
                break;
        }
    }

    /**
     * 判断该手机助手是否已经绑定手表设备
     * 当绑定的手表设备的蓝牙MAC地址或者蓝牙名称都不为空
     * 即可认为该手机已经绑定手表设备
     *
     * @return false, 未绑定；true,已经绑定
     */
    private boolean isPaired() {
        String btAddress = application.getBindBtAddress();
        String btName = application.getPackageName();
        return StringUtils.isNotEmpty(btAddress) && StringUtils.isNotEmpty(btName);
    }


    /**
     * 判断手机助手app是否有通知使用权限
     *
     * @return
     */
    private boolean isNotificationAccessEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(), ENABLED_NOTIFICATION_LISTENERS);
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 弹出对话框，提示用户去设置里打开手机助手的通知使用权限
     */
    private void showConfirmDialog() {
        if (alertDialog == null) {
            alertDialog = new AlertDialog.Builder(this)
                    .setMessage("提 示")
                    .setTitle("请打开手机助手的通知使用权限")
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
                                }
                            })
                    .create();
        }
        alertDialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        testConnecting = false;
    }

    @Override
    public void onConnectSyncServiceSuccess() {
        Log.d(TAG, "onConnectSyncServiceSuccess");
        tvConnectState.setText("同步服务已连接");
        firstStart = application.isDeviceFirstStart();
        checkNodesConnected(this);
    }

    @Override
    public void onSendFailed(final SyncMessage syncMessage, int errorCode) {
        Log.d(TAG, "send failed, message = " + syncMessage.toString());
        //super.onSendFailed(syncMessage, errorCode);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (syncMessage.getPath().equals(SyncMessagePathConfig.SOCKET_CONNECTED)) {
                    tvConnectState.setText("设备未连接");
                }
            }
        });

    }

    @Override
    public void onMessageReceived(String path, SyncMessage message) {
        super.onMessageReceived(path, message);
        Log.d(TAG, "onMessageReceived path = " + path + "");
        if (path.equals(SyncMessagePathConfig.SOCKET_CONNECTED)) {
            tvConnectState.setText("设备已连接");
            if (firstStart) {
                firstStart = false;
                application.setDeviceFirstStart(false);
                //syncWearableApps();
            }
        }
    }

    @Override
    public void onConnectSyncServiceFailed() {
        super.onConnectSyncServiceFailed();
        tvConnectState.setText("同步服务未连接");
    }

    @Override
    public void onHasNodesConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvConnectState.setText("设备已连接");
                if (firstStart) {
                    firstStart = false;
                    application.setDeviceFirstStart(false);
                    //sync time
                    CurrentTimeSyncMessage currentTimeSyncMessage = new CurrentTimeSyncMessage(SyncMessagePathConfig.CURRENT_TIME, new Date().getTime());
                    currentTimeSyncMessage.setTimeZoneId(TimeZone.getDefault().getID());
                    sendMessage(currentTimeSyncMessage);

                    if (syncProgressDialog == null) {
                        syncProgressDialog = new ProgressDialog(MainActivity.this);
                        syncProgressDialog.setCancelable(false);
                        syncProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        syncProgressDialog.setCanceledOnTouchOutside(false);
                        syncProgressDialog.setMessage("正在同步应用...");
                    }
                    syncProgressDialog.show();
                    syncWearableApps(new SyncService.ISyncAppCallback() {
                        @Override
                        public void onSyncComplete(final int total) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (total == 0) {
                                        syncProgressDialog.setMessage("同步完成，未发现应用！");
                                    } else if (total > 0) {
                                        syncProgressDialog.setMessage("同步完成");
                                    }
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (syncProgressDialog != null && syncProgressDialog.isShowing()) {
                                                syncProgressDialog.dismiss();
                                            }
                                        }
                                    }, 100);
                                }
                            });
                        }

                        @Override
                        public void onSyncBegin(final int total) {
                            if (total > 0) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        syncProgressDialog.setMessage("开始同步应用，总共" + total + "个");
                                    }
                                });
                            }
                        }

                        @Override
                        public void onSyncPost(final int index, final int total, final String packageName) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    syncProgressDialog.setMessage("开始同步第" + (index + 1) + "个应用[" + packageName + "]，总共" + total + "个");
                                }
                            });

                        }
                    });
                }
            }
        });
    }

    @Override
    public void onNoNodeConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvConnectState.setText("设备未连接");
            }
        });
    }

    @Override
    public List<String> messageListenerPaths() {
        return Arrays.asList(SyncMessagePathConfig.SOCKET_CONNECTED);
    }

}
