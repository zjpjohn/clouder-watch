package com.clouder.watch.mobile.utils;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.clouder.watch.mobile.activity.LockerSettingsActivity;
import com.cms.android.common.ConnectionResult;
import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.common.api.ResultCallback;
import com.cms.android.common.api.Status;
import com.cms.android.wearable.DataApi;
import com.cms.android.wearable.DataEvent;
import com.cms.android.wearable.DataEventBuffer;
import com.cms.android.wearable.DataMapItem;
import com.cms.android.wearable.Wearable;

/**
 * Created by zhou_wenchong on 10/20/2015.
 */
public class LockStatusService extends Service implements MobvoiApiClient.OnConnectionFailedListener, MobvoiApiClient.ConnectionCallbacks, DataApi.DataListener {

    private static final String TAG = "LockStatusService";
    private MobvoiApiClient mobvoiApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        mobvoiApiClient = new MobvoiApiClient.Builder(this).addApi(Wearable.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        mobvoiApiClient.connect();
        super.onStart(intent, startId);
    }

    @Override
    public void onDestroy() {
        Wearable.DataApi.removeListener(mobvoiApiClient, this);
        mobvoiApiClient.disconnect();
        super.onDestroy();
    }

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
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "手机端：CMS服务挂起 cause:" + i);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        Log.d(TAG, "执行回调方法 onDataChanged");
        for (DataEvent event : dataEventBuffer) {
            Log.d(TAG, "" + event.getType() + " URI = " + event.getDataItem().getUri() + " path = "
                    + event.getDataItem().getUri().getPath());

            if (event.getDataItem().getUri().getPath().equals("/path_watch_lock_status")) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                Log.d(TAG, "收到手表端锁屏状态：" + dataMapItem.getDataMap().getString("path_watch_lock_status"));
                if (dataMapItem.getDataMap().getString("path_watch_lock_status").equals("0")) {
                    LockerSettingsActivity.SWITCH_STATUS = false;

                    SharedPreferences sharedPreferences = getSharedPreferences("SWITCH_STATUS", 0);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("SWITCH_STATUS", false);
                    editor.commit();
                    Log.d(TAG, "保存锁屏状态成功：" + false);
                }
                if (dataMapItem.getDataMap().getString("path_watch_lock_status").equals("1")) {
                    LockerSettingsActivity.SWITCH_STATUS = true;
                    SharedPreferences sharedPreferences = getSharedPreferences("SWITCH_STATUS", 0);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("SWITCH_STATUS", true);
                    editor.commit();
                    Log.d(TAG, "保存锁屏状态成功：" + true);
                }
                Log.d(TAG, "执行回调方法 onDataChanged");

            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "手机端：CMS服务连接失败或者断开连接 " + connectionResult.toString());
    }
}
