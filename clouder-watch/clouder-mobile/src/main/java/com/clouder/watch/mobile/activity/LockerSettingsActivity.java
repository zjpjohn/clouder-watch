package com.clouder.watch.mobile.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.clouder.watch.common.widget.SwitchButton;
import com.clouder.watch.mobile.R;
import com.clouder.watch.mobile.SyncActivity;
import com.clouder.watch.mobile.utils.ToastUtils;
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

/**
 * Created by yang_shoulai on 7/27/2015.
 */
public class LockerSettingsActivity extends SyncActivity implements View.OnClickListener, SwitchButton.OnStatusChangeListener, MobvoiApiClient.OnConnectionFailedListener, MobvoiApiClient.ConnectionCallbacks, DataApi.DataListener {

    private static final String TAG = "LockerSettingsActivity";
    private SwitchButton switcher;
    private View changePwd;
    private MobvoiApiClient mobvoiApiClient;
    private ProgressDialog progressDialog;
    private String mPassWord;
    private static final String PATH = "/path_phone_lock";
    private static final String PATHNEW = "/path_phone_new";
    private static final String PATHSTATUS = "/path_phone_status";
    private final static int REQUEST_CODE = 1;
    private String status;
    public static boolean SWITCH_STATUS = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locker);
        setActionBarTitle(R.string.locker_settings_title);
        SharedPreferences sharedPreferences = getSharedPreferences("SWITCH_STATUS", 0);
        boolean psw = sharedPreferences.getBoolean("SWITCH_STATUS", false);
        switcher = (SwitchButton) findViewById(R.id.switcher);
        changePwd = findViewById(R.id.btn_changePwd);
        switcher.setOnStatusChangeListener(this);
        changePwd.setOnClickListener(this);
        switcher.setChecked(psw);
        mobvoiApiClient = new MobvoiApiClient.Builder(this).addApi(Wearable.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();

        Intent i = getIntent();
        status = i.getStringExtra("bleStatus");
        Log.d(TAG, "收到设备连接状态：" + status);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mobvoiApiClient.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Wearable.DataApi.removeListener(mobvoiApiClient, this);
        mobvoiApiClient.disconnect();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_changePwd) {
            if (status.equals("设备已连接")) {
                putDataItem();
                progressDialog = new ProgressDialog(this);
                progressDialog.setCancelable(true);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setCanceledOnTouchOutside(true);
                progressDialog.setMessage("同步手表端密码...");
                progressDialog.show();
            } else {
                Toast.makeText(this, "请先连接手表.....", Toast.LENGTH_SHORT).show();
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (progressDialog != null && progressDialog.isShowing()) {
                                ToastUtils.show(LockerSettingsActivity.this, "获取密码失败，请稍候再试。");
                                progressDialog.dismiss();
                            }
                        }
                    });
                }
            }).start();
        }


    }

    @Override
    public void onChange(SwitchButton button, boolean checked) {
        Log.d(TAG, "STATUS:" + checked);
        SWITCH_STATUS = checked;
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATHSTATUS);
        DataMap dataMap = putDataMapRequest.getDataMap();
        dataMap.putString("switch_status", "" + checked);
        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(mobvoiApiClient, request).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                if (dataItemResult.getStatus().isSuccess()) {
                    Log.d(TAG, "成功发送是否开启锁屏的状态");
                } else {
                    Log.d(TAG, "发送锁屏状态失败失败！");
                }
            }
        });

        SharedPreferences sharedPreferences = getSharedPreferences("SWITCH_STATUS", 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("SWITCH_STATUS", checked);
        editor.commit();
    }

    private void putDataItem() {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH);
        DataMap dataMap = putDataMapRequest.getDataMap();
        dataMap.putString("change_password_lock", "change_password_lock");
        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(mobvoiApiClient, request).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                if (dataItemResult.getStatus().isSuccess()) {
                    Log.d(TAG, "发送请求密码消息成功！");
                } else {
                    Log.d(TAG, "发送请求密码消息失败！");
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }

                }
            }
        });
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
        Log.d(TAG, "onDataChanged...");
        for (DataEvent event : dataEventBuffer) {
            Log.d(TAG, "" + event.getType() + " URI = " + event.getDataItem().getUri() + " path = "
                    + event.getDataItem().getUri().getPath());
            if (event.getDataItem().getUri().getPath().equals("/path_watch_password")) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                Log.d(TAG, "收到手表端密码：" + dataMapItem.getDataMap().getString("password"));
                if (dataMapItem.getDataMap().getString("password").equals("null")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ToastUtils.show(LockerSettingsActivity.this, "请先在手表端设置密码");
                            if (progressDialog != null && progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                        }
                    });

                } else {
                    mPassWord = dataMapItem.getDataMap().getString("password");
                    Log.d(TAG, "收到手表端密码：" + mPassWord);
                    progressDialog.dismiss();
                    Intent intent = new Intent(LockerSettingsActivity.this, ChangePasswordActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("passWord", mPassWord);
                    intent.putExtras(bundle);
                    startActivityForResult(intent, REQUEST_CODE);
                }
                Log.d(TAG, "执行回调方法 onDataChanged");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == REQUEST_CODE) {
            Bundle bundle = data.getExtras();
            String str = bundle.getString("password");
            Log.d(TAG, "onActivityResult......修改后密码：" + str);
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATHNEW);
            DataMap dataMap = putDataMapRequest.getDataMap();
            dataMap.putString("new_password", str);
            PutDataRequest request = putDataMapRequest.asPutDataRequest();
            Wearable.DataApi.putDataItem(mobvoiApiClient, request).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                @Override
                public void onResult(DataApi.DataItemResult dataItemResult) {
                    if (dataItemResult.getStatus().isSuccess()) {
                        Log.d(TAG, "成功发送修改后密码！");
                    } else {
                        Log.d(TAG, "发送修改后密码失败！");
                    }
                }
            });

        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "手机端：CMS服务连接失败或者断开连接 " + connectionResult.toString());
    }
}
