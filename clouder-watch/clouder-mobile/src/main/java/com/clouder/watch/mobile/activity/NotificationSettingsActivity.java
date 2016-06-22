package com.clouder.watch.mobile.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.clouder.watch.common.sync.SyncMessage;
import com.clouder.watch.common.sync.SyncMessagePathConfig;
import com.clouder.watch.common.sync.message.NotificationSettingsSyncMessage;
import com.clouder.watch.common.utils.Constant;
import com.clouder.watch.common.widget.SwitchButton;
import com.clouder.watch.mobile.ClouderApplication;
import com.clouder.watch.mobile.R;
import com.clouder.watch.mobile.SyncActivity;

import java.util.Arrays;
import java.util.List;

/**
 * Created by yang_shoulai on 7/27/2015.
 */
public class NotificationSettingsActivity extends SyncActivity implements View.OnClickListener, SwitchButton.OnStatusChangeListener {


    private static final String TAG = "NotiSettingActivity";

    private SwitchButton switchPush;

    private SwitchButton switchShock;

    private ImageButton btnAddMore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_center);
        initView();
    }

    /**
     * 初始化
     */
    private void initView() {
        switchPush = (SwitchButton) findViewById(R.id.switch_push);
        switchShock = (SwitchButton) findViewById(R.id.switch_shock);
        btnAddMore = (ImageButton) findViewById(R.id.btn_add_black);
        boolean push = ClouderApplication.getInstance().isNotificationPushEnable();
        boolean shock = ClouderApplication.getInstance().isNotificationShockEnable();
        switchPush.setChecked(push);
        switchShock.setChecked(shock);
        switchPush.setOnStatusChangeListener(this);
        switchShock.setOnStatusChangeListener(this);
        btnAddMore.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btn_add_black:
                startActivity(new Intent(this, NotificationBlackListActivity.class));
                break;
            default:break;
        }
    }

    @Override
    public void onChange(SwitchButton button, boolean isChecked) {
        Log.d(TAG, "onChange isChecked = " + isChecked);
        /*int id = button.getId();
        NotificationSettingsSyncMessage message = new NotificationSettingsSyncMessage(Constant.CLOUDER_SETTINGS_PKG, SyncMessagePathConfig.NOTIFICATION_SETTINGS);
        message.setMethod(SyncMessage.Method.Set);
        message.setEnable(switchPush.isChecked());
        message.setShock(switchShock.isChecked());
        if (R.id.switch_push == id || R.id.switch_shock == id) {
            sendMessage(message);
        }*/
        int id = button.getId();
        Log.d(TAG, "push enable + " + switchPush.isChecked() + ", shock enable " + switchShock.isChecked());
        if (R.id.switch_push == id) {
            ClouderApplication.getInstance().setNotificationPushEnable(switchPush.isChecked());
        }else if(R.id.switch_shock == id){
            ClouderApplication.getInstance().setNotificationShockEnable(switchShock.isChecked());
        }

    }


    @Override
    public void onConnectSyncServiceSuccess() {
        /*NotificationSettingsSyncMessage message = new NotificationSettingsSyncMessage(Constant.CLOUDER_SETTINGS_PKG, SyncMessagePathConfig.NOTIFICATION_SETTINGS);
        message.setMethod(SyncMessage.Method.Get);
        sendMessage(message);*/
    }

    @Override
    public void onMessageReceived(String path, SyncMessage syncMessage) {
        /*if (syncMessage instanceof NotificationSettingsSyncMessage) {
            NotificationSettingsSyncMessage message = (NotificationSettingsSyncMessage) syncMessage;
            ClouderApplication.getInstance().setNotificationPushEnable(message.isEnable());
            ClouderApplication.getInstance().setNotificationShockEnable(message.isShock());
            switchPush.setChecked(message.isEnable());
            switchShock.setChecked(message.isShock());
        }*/
    }

    @Override
    public void onSendSuccess(SyncMessage syncMessage) {
        super.onSendSuccess(syncMessage);
        /*if (syncMessage instanceof NotificationSettingsSyncMessage) {
            NotificationSettingsSyncMessage message = (NotificationSettingsSyncMessage) syncMessage;
            ClouderApplication.getInstance().setNotificationPushEnable(message.isEnable());
            ClouderApplication.getInstance().setNotificationShockEnable(message.isShock());
        }*/
    }

    @Override
    public void onSendFailed(SyncMessage syncMessage,int errorCode) {
        super.onSendFailed(syncMessage,errorCode);
        /*if (syncMessage instanceof NotificationSettingsSyncMessage) {
            switchPush.setChecked(ClouderApplication.getInstance().isNotificationPushEnable());
            switchShock.setChecked(ClouderApplication.getInstance().isNotificationShockEnable());
        }*/
    }

    @Override
    public List<String> messageListenerPaths() {
       /* return Arrays.asList(SyncMessagePathConfig.NOTIFICATION_SETTINGS);*/
        return null;
    }
}
