package com.clouder.watch.mobile.notification2;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.util.Log;

import com.clouder.watch.common.utils.SettingsKey;
import com.clouder.watch.mobile.R;
import com.cms.android.common.ConnectionResult;
import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.common.api.ResultCallback;
import com.cms.android.common.api.Status;
import com.cms.android.wearable.DataApi;
import com.cms.android.wearable.DataEvent;
import com.cms.android.wearable.DataEventBuffer;
import com.cms.android.wearable.DataMap;
import com.cms.android.wearable.DataMapItem;
import com.cms.android.wearable.PutDataMapRequest;
import com.cms.android.wearable.PutDataRequest;
import com.cms.android.wearable.Wearable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by yang_shoulai on 12/8/2015.
 */
public class NotificationListenerService extends Service implements MobvoiApiClient.OnConnectionFailedListener, MobvoiApiClient.ConnectionCallbacks, DataApi.DataListener {

    private static final String TAG = "NotificationListener";

    public static final int MSG_WHAT_REGISTER_LISTENER = 1;
    public static final int MSG_WHAT_UNREGISTER_LISTENER = 2;
    public static final int MSG_WHAT_GET_NOTIFICATION_LIST = 3;
    public static final String EXTRA_NOTIFICATION_LIST = "extra_notification_list";
    public static final int MSG_WHAT_NOTIFICATION_ADD = 4;
    public static final int MSG_WHAT_NOTIFICATION_DELETE = 5;
    public static final String EXTRA_UUID = "extra_uuid";
    public static final int MSG_WHAT_NOTIFICATION_UPDATE = 6;
    public static final String EXTRA_NOTIFICATION = "extra_notification";
    public static final int MSG_WHAT_SEE_ON_PHONE = 7;
    public static final int MSG_WHAT_INIT_PASSWORD = 8;
    public static final String EXTRA_PASSWORD = "extra_password";
    public static final int MSG_WHAT_LOCK_STATUS = 9;
    public static final String EXTRA_LOCK_STATUS = "extra_lock_status";


    public static final String PREFERENCE_PACKAGE = "com.clouder.watch.locker";
    public static final String PREFERENCE_NAME = "configuration";
    private static final String PATHPASSWORD = "/path_watch_password";

    private static final String[] APP_WHITE_LIST = new String[]{"com.tencent.mm", "com.tencent.mobileqq"};

    public static final int NOTIFICATION_MAX_SIZE = 50;
    private List<Notification> notifications = new ArrayList<>();
    private NotificationListenerHandler handler = new NotificationListenerHandler();
    private Messenger messenger = new Messenger(handler);
    private List<Messenger> clients = new ArrayList<>();
    private MobvoiApiClient mobvoiApiClient;

    private boolean callOn; //当前设备是否处于Calling状态

    @Override
    public void onCreate() {
        super.onCreate();
        mobvoiApiClient = new MobvoiApiClient.Builder(this).addApi(Wearable.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.clouder.watch.ACTION_CALL_STATE_CHANGE");
        registerReceiver(mReceiver, filter);
/*
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Notification notification = new Notification();
                notification.setTime("xx" + Math.random() * 100);
                notification.setTitle("tt" + Math.random() * 100);
                notification.setContent("content" + Math.random() * 100);

                Intent intent = new Intent(NotificationListenerService.this, NotificationSingleActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(EXTRA_NOTIFICATION, notification);
                ActivityOptionsCompat compat = ActivityOptionsCompat.makeCustomAnimation(NotificationListenerService.this, R.anim.base_slide_bottom_in, 0);
                startActivity(intent, compat.toBundle());
            }
        };

        timer.schedule(task, 5000, 5000);*/
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        if (intent != null && intent.getBooleanExtra("paired_device_change", false)) {
            Log.d(TAG, "更换配对手机，删除之前配对手机的通知内容");
            notifications.clear();
        }
        if (!mobvoiApiClient.isConnected()) {
            mobvoiApiClient.connect();
        }
        return START_STICKY;
    }

    private void replyNotificationList(Messenger client) {
        Message msg = Message.obtain();
        msg.what = MSG_WHAT_GET_NOTIFICATION_LIST;
        ArrayList<Notification> notificationArrayList = null;
        if (notifications != null && !notifications.isEmpty()) {
            Notification[] array = new Notification[notifications.size()];
            System.arraycopy(notifications.toArray(), 0, array, 0, notifications.size());
            notificationArrayList = new ArrayList<>(Arrays.asList(array));
        }
        msg.getData().putParcelableArrayList(EXTRA_NOTIFICATION_LIST, notificationArrayList);
        try {
            client.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 通知新增
     */
    private void replyNotificationAdd(Notification notification) {
        replyNotificationChange(MSG_WHAT_NOTIFICATION_ADD, notification);
    }

    /**
     * 通知删除
     */
    private void replyNotificationDelete(Notification notification) {
        replyNotificationChange(MSG_WHAT_NOTIFICATION_DELETE, notification);
    }

    /**
     * 通知更新
     */
    private void replyNotificationUpdate(Notification notification) {
        replyNotificationChange(MSG_WHAT_NOTIFICATION_UPDATE, notification);
    }


    private void replyNotificationChange(int what, Notification notification) {
        Message msg = Message.obtain();
        msg.what = what;
        msg.getData().putParcelable(EXTRA_NOTIFICATION, notification);
        Iterator<Messenger> iterator = clients.iterator();
        while (iterator.hasNext()) {
            Messenger client = iterator.next();
            try {
                client.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
                iterator.remove();
            }
        }
    }

    private void onNotificationChange(Notification notification) {
        boolean isWhite = isWhite(notification.getPackageName());
        Notification existByUuid = getNotificationByUuid(notification.getUuid());
        Notification existById = getNotificationById(notification.getId());
        if (isWhite) {
            //处于白名单，通知分开展示
            if (existByUuid != null) {
                copy(notification, existByUuid);
                replyNotificationUpdate(existByUuid);
            } else {
                notifications.add(0, notification);
                replyNotificationAdd(notification);
            }

        } else {
            //处于黑名单，通知合并展示
            if (existById != null) {
                copy(notification, existById);
                replyNotificationUpdate(existById);
            } else {
                notifications.add(0, notification);
                replyNotificationAdd(notification);
            }
        }
    }

    public void copy(Notification src, Notification target) {
        target.setId(src.getId());
        target.setUuid(src.getUuid());
        target.setIcon(src.getIcon());
        target.setTime(src.getTime());
        target.setContent(src.getContent());
        target.setShock(src.isShock());
        target.setPackageName(src.getPackageName());
        target.setDate(src.getDate());
        target.setTitle(src.getTitle());
    }


    private boolean isWhite(String packageName) {
        for (String white : APP_WHITE_LIST) {
            if (white.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private Notification getNotificationByUuid(String uuid) {
        if (notifications != null) {
            for (Notification notification : notifications) {
                if (notification.getUuid().equals(uuid)) {
                    return notification;
                }
            }
        }
        return null;
    }

    private Notification getNotificationById(String id) {
        if (notifications != null) {
            for (Notification notification : notifications) {
                if (notification.getId().equals(id)) {
                    return notification;
                }
            }
        }
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mobvoiApiClient != null) {
            Wearable.DataApi.removeListener(mobvoiApiClient, this);
            mobvoiApiClient.disconnect();
        }
        unregisterReceiver(mReceiver);
    }

    public void reconnect() {
        Wearable.DataApi.removeListener(mobvoiApiClient, this);
        mobvoiApiClient.disconnect();
        Log.e(TAG, "Mobvoi api client will try connect again 5 seconds later! thread =" + Thread.currentThread().getName());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mobvoiApiClient.connect();
            }
        }, 5000);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mobvoiApiClient, this).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status result) {
                        Log.d(TAG, "Add data listener success ? " + result.isSuccess());
                    }
                });
    }


    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended cause = " + i);
        reconnect();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        Log.d(TAG, "onDataChanged");
        for (DataEvent event : dataEventBuffer) {
            int eventType = event.getType();
            Uri uri = event.getDataItem().getUri();
            Log.d(TAG, String.format("EventType = %s, URI = %s, Path = %s", eventType, uri, uri.getPath()));
            if (event.getDataItem().getUri().getPath().equals("/path_phone_notification")) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                String[] a = dataMapItem.getDataMap().getStringArray("data");
                byte[] largeIcon = dataMapItem.getDataMap().getByteArray("largeIcon");
                if (a == null || a.length != 9) {
                    return;
                }
                if (notifications.size() >= NOTIFICATION_MAX_SIZE) {
                    notifications.remove(NOTIFICATION_MAX_SIZE - 1);
                }
                Notification notification = new Notification();
                notification.setId(a[8]);
                notification.setUuid(a[7]);
                notification.setContent(a[1]);
                notification.setTitle(a[0]);
                notification.setDate(a[2]);
                notification.setTime(a[3]);
                notification.setPackageName(a[4]);
                notification.setShock(a[5] != null && "true".equals(a[5]));
                notification.setIcon(largeIcon == null ? null : BitmapFactory.decodeByteArray(largeIcon, 0, largeIcon.length));
                Log.d(TAG, "Receive notification change event, Notification = " + notification.toString());
                if (!callOn) {
                    Intent intent = new Intent(this, NotificationSingleActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(EXTRA_NOTIFICATION, notification);
                    ActivityOptionsCompat compat = ActivityOptionsCompat.makeCustomAnimation(this, R.anim.base_slide_bottom_in, 0);
                    startActivity(intent, compat.toBundle());
                }
                onNotificationChange(notification);
            } else if (event.getDataItem().getUri().getPath().equals("/path_phone_lock")) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                String a = dataMapItem.getDataMap().getString("change_password_lock");
                if ("change_password_lock".equals(a)) {
                    Log.d(TAG, "收到手机端密码请求消息...");
                    Context c = null;
                    try {
                        c = this.createPackageContext(PREFERENCE_PACKAGE, Context.CONTEXT_IGNORE_SECURITY);
                    } catch (PackageManager.NameNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    if (c != null) {
                        SharedPreferences sharedPreferences = c.getSharedPreferences(PREFERENCE_NAME, Context.MODE_MULTI_PROCESS);
                        final String psw = sharedPreferences.getString("PassWord", null);
                        Log.d(TAG, "跨应用获取到锁屏密码为：" + psw);
                        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATHPASSWORD);
                        DataMap dataMap = putDataMapRequest.getDataMap();
                        if (psw == null) {
                            dataMap.putString("password", "null");
                            PutDataRequest request = putDataMapRequest.asPutDataRequest();
                            Wearable.DataApi.putDataItem(mobvoiApiClient, request).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                                @Override
                                public void onResult(DataApi.DataItemResult dataItemResult) {
                                    if (dataItemResult.getStatus().isSuccess()) {
                                        Log.d(TAG, "发送密码为空提示成功成功");
                                    } else {
                                        Log.d(TAG, "发送密码为空提示失败");
                                    }
                                }
                            });
                        } else {
                            dataMap.putString("password", psw);
                            PutDataRequest request = putDataMapRequest.asPutDataRequest();
                            Wearable.DataApi.putDataItem(mobvoiApiClient, request).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                                @Override
                                public void onResult(DataApi.DataItemResult dataItemResult) {
                                    if (dataItemResult.getStatus().isSuccess()) {
                                        Log.d(TAG, "发送手表端锁屏密码成功" + "----密码-----" + psw);
                                    } else {
                                        Log.d(TAG, "发送手表端锁屏密码失败");
                                    }
                                }
                            });
                        }
                    }

                }
            } else if (event.getDataItem().getUri().getPath().equals("/path_phone_new")) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                String b = dataMapItem.getDataMap().getString("new_password");
                Intent i = new Intent();
                i.putExtra("newPassword", b);
                i.setComponent(new ComponentName("com.clouder.watch.locker", "com.clouder.watch.locker.LockService"));
                startService(i);
                Log.d(TAG, "收到手机端修改后的密码：" + b);
            } else if (event.getDataItem().getUri().getPath().equals("/path_phone_status")) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                String b = dataMapItem.getDataMap().getString("switch_status");
                if (b.equals("true")) {
                    Settings.System.putInt(getContentResolver(), SettingsKey.SCREEN_LOCKER_ENABLE, 1);
                    Log.d(TAG, "查看是否设置成功:" + Settings.System.getInt(getContentResolver(), SettingsKey.SCREEN_LOCKER_ENABLE, 0) + "");
                }
                if (b.equals("false")) {
                    Settings.System.putInt(getContentResolver(), SettingsKey.SCREEN_LOCKER_ENABLE, 0);
                    Log.d(TAG, "查看是否设置成功:" + Settings.System.getInt(getContentResolver(), SettingsKey.SCREEN_LOCKER_ENABLE, 0) + "");
                }
                Log.d(TAG, "收到手机端锁屏状态：" + b);
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult != null && connectionResult.getErrorCode() == 9) {
            reconnect();
        }
    }

    private void sendNfDelete(String uuid) {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/path_watch_delete");
        DataMap dataMap = putDataMapRequest.getDataMap();
        dataMap.putString("removePackage", uuid);
        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(mobvoiApiClient, request);
    }


    private void sendSeeOnPhone(String uuid) {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/path_watch");
        DataMap dataMap = putDataMapRequest.getDataMap();
        dataMap.putString("packageName", uuid);
        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(mobvoiApiClient, request);
    }

    private void sendLockStatus(String status) {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/path_watch_lock_status");
        DataMap dataMap = putDataMapRequest.getDataMap();
        dataMap.putString("path_watch_lock_status", status);
        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(mobvoiApiClient, request).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                if (dataItemResult.getStatus().isSuccess()) {
                    Log.d(TAG, "发送锁屏状态成功！");
                } else {
                    Log.d(TAG, "发送锁屏状态失败！");
                }
            }
        });
    }

    private void sendInitPassword(String password) {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/path_watch_lock_status");
        DataMap dataMap = putDataMapRequest.getDataMap();
        dataMap.putString("path_watch_lock_status", password);
        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(mobvoiApiClient, request).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                if (dataItemResult.getStatus().isSuccess()) {
                    Log.d(TAG, "发送锁屏状态成功！");
                } else {
                    Log.d(TAG, "发送锁屏状态失败！");
                }
            }
        });
    }


    private class NotificationListenerHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            String uuid;
            switch (what) {
                case MSG_WHAT_REGISTER_LISTENER:
                    if (msg.replyTo != null) {
                        clients.add(msg.replyTo);
                        replyNotificationList(msg.replyTo);
                    }
                    break;

                case MSG_WHAT_UNREGISTER_LISTENER:
                    if (msg.replyTo != null) {
                        clients.remove(msg.replyTo);
                    }
                    break;

                case MSG_WHAT_NOTIFICATION_DELETE:
                    uuid = msg.getData().getString(EXTRA_UUID);
                    if (uuid != null) {
                        Notification notification = getNotificationByUuid(uuid);
                        if (notification != null) {
                            notifications.remove(notification);
                            replyNotificationDelete(notification);
                        }
                        sendNfDelete(uuid);
                    }
                    break;
                case MSG_WHAT_SEE_ON_PHONE:
                    uuid = msg.getData().getString(EXTRA_UUID);
                    if (uuid != null) {
                        sendSeeOnPhone(uuid);
                    }
                    break;
                case MSG_WHAT_INIT_PASSWORD:
                    String password = msg.getData().getString(EXTRA_PASSWORD);
                    sendInitPassword(password);
                    break;

                case MSG_WHAT_LOCK_STATUS:
                    String status = msg.getData().getString(EXTRA_LOCK_STATUS);
                    sendLockStatus(status);
                    break;
                default:
                    break;
            }
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Call State Change Action = " + intent.getAction());
            if ("com.clouder.watch.ACTION_CALL_STATE_CHANGE".equals(intent.getAction())) {
                int state = intent.getIntExtra("extra_state", -1);
                Log.d(TAG, "Call State = " + intent.getAction());
                if (state == 0 || state == 2 || state == 4) {
                    callOn = true;
                } else {
                    callOn = false;
                }
            }
        }
    };


}
