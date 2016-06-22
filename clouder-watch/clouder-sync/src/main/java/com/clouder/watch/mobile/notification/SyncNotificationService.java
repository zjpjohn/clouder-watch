package com.clouder.watch.mobile.notification;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;

import com.clouder.watch.common.utils.SettingsKey;
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

import java.io.ByteArrayOutputStream;
import java.util.LinkedList;

/**
 * Created by zhou_wenchong on 8/31/2015.
 */
public class SyncNotificationService extends Service implements MobvoiApiClient.OnConnectionFailedListener, MobvoiApiClient.ConnectionCallbacks, DataApi.DataListener {
    private static final String TAG = "SyncNotificationService";
    private Intent notifyIntent = null;
    private static final String PATH = "/path_watch";
    private static final String PATHS = "/path_watch_lock_status";
    private static final String PATHPASSWORD = "/path_watch_password";
    private MobvoiApiClient mobvoiApiClient;
    private LinkedList<Page> mCache;
    private static final int CACHE_MAX_SIZE = 100;
    //    private Map<String, String> map;
    private boolean TELPHONE = false;
    public static final String PREFERENCE_PACKAGE = "com.clouder.watch.locker";
    public static final String PREFERENCE_NAME = "configuration";
    private Handler mainHandler = new Handler();

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "action" + intent.getAction());
            if (intent.getAction().equals("com.clouder.watch.ACTION_CALL_STATE_CHANGE")) {
                int state = intent.getIntExtra("extra_state", -1);
                if (state != -1) {
                    switch (state) {
                        case 0:
                            //接通
                            TELPHONE = true;
                            Log.d(TAG, "接收到接通广播：" + TELPHONE);
                            break;
                        case 2:
                            //去电
                            TELPHONE = true;
                            Log.d(TAG, "接收到去电广播：" + TELPHONE);
                            break;
                        case 4:
                            //来电
                            TELPHONE = true;
                            Log.d(TAG, "接收到来电广播：" + TELPHONE);
                            break;
                        case 7:
                            //挂断
                            TELPHONE = false;
                            Log.d(TAG, "接收到挂断广播：" + TELPHONE);
                            break;
                    }

                }
                Log.d(TAG, "接收到电话开始的广播：" + intent.getAction());
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mCache = new LinkedList<>();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.clouder.watch.ACTION_CALL_STATE_CHANGE");
        registerReceiver(mReceiver, filter);
        mobvoiApiClient = new MobvoiApiClient.Builder(this).addApi(Wearable.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!mobvoiApiClient.isConnected()) {
            mobvoiApiClient.connect();
        }

        Log.d(TAG, "onStartCommand");
        if (intent != null) {
            String uuid = intent.getStringExtra("REMOVE_PAGE_UUID");
            Log.d(TAG, "UUID=" + uuid);
            if (null != uuid) {
                if (mCache.size() > 0) {
                    for (int i = 0; i < mCache.size(); i++) {
                        if (mCache.get(i).getUuid().equals(uuid)) {
                            Log.d(TAG, "remove uuid" + mCache.get(i).getUuid());
                            mCache.remove(i);
                        }
                    }
                }
                PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/path_watch_delete");
                DataMap dataMap = putDataMapRequest.getDataMap();
                dataMap.putString("removePackage", uuid);
                PutDataRequest request = putDataMapRequest.asPutDataRequest();
                Wearable.DataApi.putDataItem(mobvoiApiClient, request);
            }
            String uid = intent.getStringExtra("OPEN_ON_PHONE");
            Log.d(TAG, "OPEN_ON_PHONE:" + uid);
            if (null != uid) {
                PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH);
                DataMap dataMap = putDataMapRequest.getDataMap();
                dataMap.putString("packageName", uid);
                PutDataRequest request = putDataMapRequest.asPutDataRequest();
                Wearable.DataApi.putDataItem(mobvoiApiClient, request);
                Log.d(TAG, "发送uuid到手机端让手机打来响应的页面:" + uid);
            }
            if (intent.getStringExtra("onSwipeTop") != null && intent.getStringExtra("onSwipeTop").equals("onSwipeTop")) {
                if (mCache.size() == 0) {
                    notifyIntent = new Intent(SyncNotificationService.this, NotificationActivity.class);
                    notifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    SyncNotificationService.this.startActivity(notifyIntent);
                } else {
                    notifyIntent = new Intent(SyncNotificationService.this, NotificationActivity.class);
                    notifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    notifyIntent.putExtra("NOTIFICATION_DATA", mCache);
                    SyncNotificationService.this.startActivity(notifyIntent);
                }
            }

            if (intent.getStringExtra("lockStatus") != null) {
                String a = intent.getStringExtra("lockStatus");
                Log.d(TAG, "获取到Settings中锁屏状态为：" + a);
                PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATHS);
                DataMap dataMap = putDataMapRequest.getDataMap();
                dataMap.putString("path_watch_lock_status", a);
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
                Log.d(TAG, "锁屏状态发送：" + a);

            }

            if (intent.getStringExtra("first_set_password") != null) {
                String a = intent.getStringExtra("first_set_password");
                Log.d(TAG, "初次设置锁屏密码：" + a);
                PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATHS);
                DataMap dataMap = putDataMapRequest.getDataMap();
                dataMap.putString("path_watch_lock_status", a);
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
                Log.d(TAG, "锁屏状态发送：" + a);

                if (intent.getBooleanExtra("paired_device_change", false) == true) {
                    Log.d(TAG, "更换配对手机，删除之前配对手机的通知内容。");
                    mCache.clear();
                }
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Wearable.DataApi.removeListener(mobvoiApiClient, this);
        mobvoiApiClient.disconnect();
        unregisterReceiver(mReceiver);
        //startService(new Intent(this, SyncNotificationService.class));
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected addListener");
        Wearable.DataApi.addListener(mobvoiApiClient, this).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status result) {
                        Log.d(TAG, "DataApi : " + result.isSuccess());
                    }
                });
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.e(TAG, "手机端：CMS服务挂起 cause:" + cause);
        Log.e(TAG, "Mobvoi api client connection was suspended! mobvoi api client will try reconnect! ");
        reconnect();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        Log.d(TAG, "onDataChanged...");
        for (DataEvent event : dataEventBuffer) {
            Log.d(TAG, "" + event.getType() + " URI = " + event.getDataItem().getUri() + " path = "
                    + event.getDataItem().getUri().getPath());
            if (event.getDataItem().getUri().getPath().equals("/path_phone_notification")) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                String[] a = dataMapItem.getDataMap().getStringArray("data");
                byte[] largeIcon = dataMapItem.getDataMap().getByteArray("largeIcon");
                Log.d(TAG, "收到手机端消息——》" + a[0] + "=" + a[1] + "=" + a[2] + "=" + a[3]);
                Log.d(TAG, "收到手机端包名——》" + a[4]);
                Log.d(TAG, "判断是否可以震动——》" + a[5]);
                Log.d(TAG, "判断该通知是否有LargeIcon——》" + a[6]);
                //判断是否可以震动
                if (a[5].trim().equals("true")) {
                    Vibrator vib = (Vibrator) SyncNotificationService.this.getSystemService(Service.VIBRATOR_SERVICE);
                    //vibrator.vibrate(1000);// Only shock second, once
                    long[] pattern = {0, 300};
                    vib.vibrate(pattern, -1);
                }
                if (largeIcon != null && (a[0] != null) && a[6].equals("hasLargeIcon")) {
                    if (!a[4].equals("com.tencent.mm") && !a[4].equals("com.tencent.mobileqq")) {
                        if(mCache.size()>0){
                            for (int i = 0; i < mCache.size(); i++) {
                                if (mCache.get(i).getId().equals(a[8])) {
                                    Log.d(TAG, "remove id" + mCache.get(i).getId());
                                    mCache.remove(i);
                                }
                            }
                        }

                        Page page = new Page(a[0], a[1], a[2], a[3], largeIcon, a[7], a[8]);
                        mCache.addFirst(page);
                        Log.d(TAG, ",mCache.size=" + mCache.size());
                        if (TELPHONE == false) {
                            notifyIntent = new Intent(SyncNotificationService.this, NotificationActivity.class);
                            notifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            notifyIntent.putExtra("NOTIFICATION_DATA", mCache);
                            SyncNotificationService.this.startActivity(notifyIntent);
                        }
                    } else {
                        Page page = new Page(a[0], a[1], a[2], a[3], largeIcon, a[7], a[8]);
                        mCache.addFirst(page);
                        Log.d(TAG, ",mCache.size=" + mCache.size());
                        if (TELPHONE == false) {
                            notifyIntent = new Intent(SyncNotificationService.this, NotificationActivity.class);
                            notifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            notifyIntent.putExtra("NOTIFICATION_DATA", mCache);
                            SyncNotificationService.this.startActivity(notifyIntent);
                        }
                        Log.d(TAG, "执行回调方法 onDataChanged");
                    }
                } else if (a[6].equals("noLargeIcon")) {
                    Page page = new Page(a[0], a[1], a[2], a[3], null, a[7], a[8]);
                    mCache.addFirst(page);
                    if (TELPHONE == false) {
                        notifyIntent = new Intent(SyncNotificationService.this, NotificationActivity.class);
                        notifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        notifyIntent.putExtra("NOTIFICATION_DATA", mCache);
                        SyncNotificationService.this.startActivity(notifyIntent);
                    }
                    Log.d(TAG, "执行回调方法 onDataChanged");
                }
            }
            if (event.getDataItem().getUri().getPath().equals("/path_phone_lock")) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                String a = dataMapItem.getDataMap().getString("change_password_lock");
                if (a.equals("change_password_lock")) {
                    Log.d(TAG, "收到手机端密码请求消息...");

                    Context c = null;
                    try {
                        c = this.createPackageContext(PREFERENCE_PACKAGE, Context.CONTEXT_IGNORE_SECURITY);

                    } catch (PackageManager.NameNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
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
            if (event.getDataItem().getUri().getPath().equals("/path_phone_new")) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                String b = dataMapItem.getDataMap().getString("new_password");
                Intent i = new Intent();
                i.putExtra("newPassword", b);
                i.setComponent(new ComponentName("com.clouder.watch.locker", "com.clouder.watch.locker.LockService"));
                startService(i);
                Log.d(TAG, "收到手机端修改后的密码：" + b);
            }


            if (event.getDataItem().getUri().getPath().equals("/path_phone_status")) {
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
        Log.e(TAG, "手机端：CMS服务连接失败或者断开连接 " + connectionResult.toString());
        Log.e(TAG, "Mobvoi api client can not connect to cms service, and result is: " + connectionResult);
        if (connectionResult != null && connectionResult.getErrorCode() == 9) {
            reconnect();
        }
    }

    //图片转换为字节数组
    public byte[] Bitmap2Bytes(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    /**
     * 重连
     */
    public void reconnect() {
        Wearable.DataApi.removeListener(mobvoiApiClient, this);
        mobvoiApiClient.disconnect();
        Log.e(TAG, "Mobvoi api client will try connect again " + 5
                + " seconds later! thread =" + Thread.currentThread().getName());

        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mobvoiApiClient.connect();
            }
        }, 5000);
    }

}