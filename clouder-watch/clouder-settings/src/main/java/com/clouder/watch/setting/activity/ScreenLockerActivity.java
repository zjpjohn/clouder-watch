package com.clouder.watch.setting.activity;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.clouder.watch.common.ui.SwipeRightActivity;
import com.clouder.watch.common.utils.Constant;
import com.clouder.watch.common.utils.SettingsKey;
import com.clouder.watch.common.utils.StringUtils;
import com.clouder.watch.setting.R;

/**
 * Created by yang_shoulai on 7/23/2015.
 */
public class ScreenLockerActivity extends SwipeRightActivity {

    private static final String TAG = "ScreenLockerActivity";

    public static final int VIEW_TYPE_ONE = 1;

    public static final int VIEW_TYPE_TWO = 2;

    private WearableListView listView;

    private WearableListView.Adapter adapter;

    private TextView header;

    private ServiceConnection serviceConnection;

    private Messenger serviceMessenger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_locker);
        listView = (WearableListView) findViewById(R.id.list_view);
        header = (TextView) findViewById(R.id.header);
        adapter = new WearableListView.Adapter() {
            @Override
            public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

                if (viewType == VIEW_TYPE_ONE) {
                    return new WearableListView.ViewHolder(getLayoutInflater().inflate(R.layout.item_screen_lock_one, null));
                } else if (viewType == VIEW_TYPE_TWO) {
                    return new WearableListView.ViewHolder(getLayoutInflater().inflate(R.layout.item_screen_lock_two, null));
                }
                return null;
            }

            @Override
            public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
                if (position == 0) {
                    TextView title = (TextView) holder.itemView.findViewById(R.id.title);
                    TextView btn = (TextView) holder.itemView.findViewById(R.id.btn);
                    title.setText(getString(R.string.screen_locker));
                    boolean screenLockEnable = Settings.System.getInt(getContentResolver(), SettingsKey.SCREEN_LOCKER_ENABLE, 0) == 1;
                    if (screenLockEnable) {
                        btn.setText(R.string.state_on);
                        btn.setBackgroundResource(R.drawable.btn_settings_on);
                    } else {
                        btn.setText(R.string.state_off);
                        btn.setBackgroundResource(R.drawable.btn_settings_off);
                    }
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            openOrCloseScreenLocker();
                            adapter.notifyDataSetChanged();
                        }
                    });
                } else if (position == 1) {
                    TextView title = (TextView) holder.itemView.findViewById(R.id.tvChangePwd);
                    String password = SettingsKey.getLockPassword(ScreenLockerActivity.this);
                    if (StringUtils.isEmpty(password)) {
                        title.setText(R.string.set_lock_password);
                    } else {
                        title.setText(R.string.update_lock_password);
                    }
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startChangePwdActivity();
                        }
                    });
                }
            }

            @Override
            public int getItemCount() {
                return 2;
            }

            @Override
            public int getItemViewType(int position) {
                if (position == 0) {
                    return VIEW_TYPE_ONE;
                } else if (position == 1) {
                    return VIEW_TYPE_TWO;
                }
                return -1;
            }
        };
        listView.setAdapter(adapter);
        listView.scrollToPosition(1);
        listView.setEnabled(false);
//
//        serviceConnection = new ServiceConnection() {
//            @Override
//            public void onServiceConnected(ComponentName name, IBinder service) {
//                serviceMessenger = new Messenger(service);
//            }
//
//            @Override
//            public void onServiceDisconnected(ComponentName name) {
//
//            }
//        };

//        Intent callListenerServiceIntent = new Intent();
//        callListenerServiceIntent.setComponent(new ComponentName("com.clouder.watch.mobile", "com.clouder.watch.mobile.notification2.NotificationListenerService"));
//        bindService(callListenerServiceIntent, serviceConnection, BIND_AUTO_CREATE);

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
//        unRegisterListener();
//        if (serviceConnection != null) {
//            unbindService(serviceConnection);
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void sendStatus(String status) {
        if (serviceMessenger != null) {
            Message msg = Message.obtain();
            msg.what = 9;
            msg.getData().putString("extra_lock_status", status);
            try {
                serviceMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendScreenLockStatus(String status) {
        Intent intent = new Intent();
        intent.putExtra("lockStatus", status);
        intent.setComponent(new ComponentName("com.clouder.watch.mobile", "com.clouder.watch.mobile.notification.SyncNotificationService"));
        startService(intent);
    }

    /**
     * 打开或者关闭屏幕锁
     */
    private void openOrCloseScreenLocker() {
        boolean screenLockEnable = Settings.System.getInt(getContentResolver(), SettingsKey.SCREEN_LOCKER_ENABLE, 1) == 1;
        if (screenLockEnable) {
            Settings.System.putInt(getContentResolver(), SettingsKey.SCREEN_LOCKER_ENABLE, 0);
            Log.d(TAG, "发送的状态：" + Settings.System.getInt(getContentResolver(), SettingsKey.SCREEN_LOCKER_ENABLE, 0));
//            sendStatus(Settings.System.getInt(getContentResolver(), SettingsKey.SCREEN_LOCKER_ENABLE, 0) + "");
            sendScreenLockStatus(Settings.System.getInt(getContentResolver(), SettingsKey.SCREEN_LOCKER_ENABLE, 0) + "");
        } else {
            Settings.System.putInt(getContentResolver(), SettingsKey.SCREEN_LOCKER_ENABLE, 1);
            Log.d(TAG, "发送的状态：" + Settings.System.getInt(getContentResolver(), SettingsKey.SCREEN_LOCKER_ENABLE, 1));
//            sendStatus(Settings.System.getInt(getContentResolver(), SettingsKey.SCREEN_LOCKER_ENABLE, 1) + "");
            sendScreenLockStatus(Settings.System.getInt(getContentResolver(), SettingsKey.SCREEN_LOCKER_ENABLE, 1) + "");
        }
    }

    /**
     * 打开屏幕锁密码修改界面
     */
    private void startChangePwdActivity() {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(Constant.CLOUDER_LOCKER_PKG, Constant.CLOUDER_LOCKER_CHANGE_PASSWORD));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "ActivityNotFoundException", e);
        }
    }

}
