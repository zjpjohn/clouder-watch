package com.clouder.watch.mobile.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.clouder.watch.mobile.ClouderApplication;
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
import com.cms.android.wearable.service.common.LogTool;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by zhou_wenchong on 8/31/2015.
 */
public class NotificationService extends NotificationListenerService implements MobvoiApiClient.OnConnectionFailedListener, MobvoiApiClient.ConnectionCallbacks, DataApi.DataListener {
    private static final String TAG = "NotificationService";
    private static final String PATH = "/path_phone_notification";
    private String[] mData;
    private MobvoiApiClient mobvoiApiClient;
    private String time, date;
    private Handler handler;
    private Bitmap notificationLargeIcon;
    private PendingIntent i;
    private Map<String, PendingIntent> pendingIntent = new HashMap<>();
    private boolean blankListStatus = false;
    private int onHour, onMin, onSec, offHour, offMin, offSec, timeIndex;
    private static final String[] APP_WHITE_LIST = new String[]{"com.tencent.mm", "com.tencent.mobileqq", "com.android.mms","com.android.phone"};

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        mobvoiApiClient = new MobvoiApiClient.Builder(this).addApi(Wearable.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 110:
                        putDataItem();
                        break;
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mobvoiApiClient.connect();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            Log.d(TAG, " id =" + sbn.getId() + ", userId = " + sbn.getUserId() + ", tag =" + sbn.getTag());
            Calendar c = Calendar.getInstance();
            onHour = c.get(Calendar.HOUR);
            onMin = c.get(Calendar.MINUTE);
            onSec = c.get(Calendar.SECOND);
            Log.d(TAG, "来通知了，" + System.currentTimeMillis());
            String packageName = sbn.getPackageName();
            String id = sbn.getId() + "";
            List<String> packageList = ClouderApplication.getInstance().getNotificationBlackList();
            if (ClouderApplication.getInstance().isNotificationPushEnable() == true) {
                Log.d(TAG, "消息推送：" + ClouderApplication.getInstance().isNotificationPushEnable() + "");
                Log.d(TAG, "黑名单：" + packageList.toString() + "");
                if (packageList.toString().equals("[]") == false) {
                    Log.d(TAG, "blankList===!!!!==null");
                    for (int i = 0; i < packageList.size(); i++) {
                        Log.d(TAG, "黑名单：" + packageList.get(i));
                        if (packageName.equals(packageList.get(i))) {
                            Log.d(TAG, "禁止接收这个包的通知：" + packageList.get(i));
                            blankListStatus = true;
                            Log.d(TAG, "禁止接收这个包的通知:blankListStatus:" + blankListStatus);
                        }
                    }
                }
                if (packageList.toString().equals("[]")) {
                    blankListStatus = false;
                    Log.d(TAG, "blankList=====null:" + blankListStatus);
                }
            } else {
                blankListStatus = true;
                Log.d(TAG, "没有开启通知：" + blankListStatus);
            }

            Log.d(TAG, "blankListStatus:" + blankListStatus);
            Log.d(TAG, "PackageName:" + sbn.getPackageName());
            Bundle extras = sbn.getNotification().extras;
            final String notificationTitle = extras.getString(Notification.EXTRA_TITLE);
            final Object notificationObj = extras.get(Notification.EXTRA_TEXT);
            String notificationText = notificationObj == null ? "" : notificationObj.toString();
            if (packageName.equals("com.android.phone")) {
                notificationLargeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.phone);
            } else {
                notificationLargeIcon = getAppIcon(packageName);
            }
            Log.d(TAG, "notificationTitle:" + notificationTitle);
            Log.d(TAG, "notificationText:" + notificationText);
            SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy/MM/dd  hh:mm");
            time = sDateFormat.format(new Date());
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm");
            date = sdf.format(new Date());
            if (sbn.getPackageName().equals("android") || sbn.getPackageName().equals("com.android.incallui") || (extras.get(Notification.EXTRA_PROGRESS) !=
                    null && extras.get(Notification.EXTRA_PROGRESS).equals("android.progress"))) {
                Log.d(TAG, "电话情况or系统通知or通知中有progressbar，不发送至手表");
                blankListStatus = true;
            }
            if (TimeIndex() <= 2 && mData != null) {
                if (mData[0].equals(notificationTitle) && mData[1].equals(notificationText)) {
                    blankListStatus = true;
                    Log.d(TAG, "有重复消息，不发送。");
                }
            }
            if (!blankListStatus && notificationTitle != null && notificationText != null && isWhite(packageName)) {
                i = sbn.getNotification().contentIntent;
                String uuid = java.util.UUID.randomUUID().toString();
                pendingIntent.put(uuid, i);
                boolean shock = ClouderApplication.getInstance().isNotificationShockEnable();
                if (notificationLargeIcon == null) {
                    mData = new String[]{notificationTitle, notificationText, date, time, packageName + "", "" + shock, "noLargeIcon", uuid, id};
                    Message msg = handler.obtainMessage();
                    msg.what = 110;
                    handler.sendMessage(msg);
                } else {
                    mData = new String[]{notificationTitle, notificationText, date, time, packageName + "", "" + shock, "hasLargeIcon", uuid, id};
                    Message msg = handler.obtainMessage();
                    msg.what = 110;
                    handler.sendMessage(msg);
                }
                Calendar cc = Calendar.getInstance();
                offHour = cc.get(Calendar.HOUR);
                offMin = cc.get(Calendar.MINUTE);
                offSec = cc.get(Calendar.SECOND);
                Log.d(TAG, "通知数据添加完毕了," + System.currentTimeMillis());
                extras.clear();
            } else {
                Log.d(TAG, "白名单之外的通知，不予添加：" + packageName);
            }
            blankListStatus = false;

        } catch (Exception e) {
            LogTool.d(TAG, "需要重启通知权限才能正常收到通知" + e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {

    }

    private boolean isWhite(String packageName) {
        for (String white : APP_WHITE_LIST) {
            if (white.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private void putDataItem() {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH);
        DataMap dataMap = putDataMapRequest.getDataMap();
        if (notificationLargeIcon != null) {
            dataMap.putByteArray("largeIcon", Bitmap2Bytes(notificationLargeIcon));
            dataMap.putStringArray("data", mData);
            PutDataRequest request = putDataMapRequest.asPutDataRequest();
            Wearable.DataApi.putDataItem(mobvoiApiClient, request).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                @Override
                public void onResult(DataApi.DataItemResult dataItemResult) {
                    if (dataItemResult.getStatus().isSuccess()) {
                        Log.d(TAG, "发送消息成功！");
                    } else {
                        Log.d(TAG, "发送消息失败");
                    }
                }
            });
            Log.d(TAG, "putStringArray：" + mData.length);
        } else {
            dataMap.putStringArray("data", mData);
            PutDataRequest request = putDataMapRequest.asPutDataRequest();

            Wearable.DataApi.putDataItem(mobvoiApiClient, request).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                @Override
                public void onResult(DataApi.DataItemResult dataItemResult) {
                    if (dataItemResult.getStatus().isSuccess()) {
                        Log.d(TAG, "发送消息成功！");
                    } else {
                        Log.d(TAG, "发送消息失败");
                    }
                }
            });
            Log.d(TAG, "putStringArray：" + mData.length);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Wearable.DataApi.removeListener(mobvoiApiClient, this);
        mobvoiApiClient.disconnect();
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
        // mobvoiApiClient.connect();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        Log.d(TAG, "onDataChanged...");
        for (DataEvent event : dataEventBuffer) {
            Log.d(TAG, "" + event.getType() + " URI = " + event.getDataItem().getUri() + " path = "
                    + event.getDataItem().getUri().getPath());
            if (event.getDataItem().getUri().getPath().equals("/path_watch")) {
                Log.d(TAG, "[onDataChanged] handle uri(/path_watch)");
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                String packageIndex = dataMapItem.getDataMap().getString("packageName");
                try {
                    if (pendingIntent.get(packageIndex) != null) {
                        Class pendingIntentClass = pendingIntent.get(packageIndex).getClass();
                        Method method = pendingIntentClass.getMethod("getIntent");
                        Intent intent = ( Intent ) method.invoke(pendingIntent.get(packageIndex));
                        Log.d(TAG, "Intent.toString = " + intent.toString());
                        if(intent.getComponent() == null || !"com.android.mms".equals(intent.getComponent().getPackageName())){
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        } else{
                            pendingIntent.get(packageIndex).send();
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Mobile view page",e);
                }
            }

            if (event.getDataItem().getUri().getPath().equals("/path_watch_delete")) {
                Log.d(TAG, "[onDataChanged] handle uri(/path_watch_delete)");
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                String removeIndex = dataMapItem.getDataMap().getString("removePackage");
                if (pendingIntent.get(removeIndex) != null) {
                    pendingIntent.remove(removeIndex);
                }
                Log.d(TAG, "执行回调方法 onDataChanged");
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "手机端：CMS服务连接失败或者断开连接 " + connectionResult.toString());
        // Wearable.DataApi.removeListener(mobvoiApiClient, this);
    }

    //图片转换为字节数组
    public byte[] Bitmap2Bytes(Bitmap bm) {
        if (bm == null) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    /*
  * 获取程序 图标
  */
    public Bitmap getAppIcon(String packname) {
        PackageManager pm = getPackageManager();
        ApplicationInfo info = null;
        Bitmap bm = null;
        try {
            info = pm.getApplicationInfo(packname, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            Log.d(TAG, "未获取到应用包名");

        }
        if (info != null) {
            BitmapDrawable bd = (BitmapDrawable) info.loadIcon(pm);
            bm = bd.getBitmap();
        }
        return bm;
    }

    private int TimeIndex() {

        if (onHour == 0 && onMin == 0 && onSec == 0) {
            timeIndex = 1;
        } else {
            timeIndex = (onHour - offHour) * 60 * 60 + (onMin - offMin) * 60 + (onSec - offSec);
        }
        return timeIndex;
    }
}