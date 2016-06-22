package com.clouder.watch.mobile.handler;

import android.provider.Settings;
import android.util.Log;

import com.clouder.watch.common.sync.IHandler;
import com.clouder.watch.common.sync.IMessageListener;
import com.clouder.watch.common.sync.SyncMessage;
import com.clouder.watch.common.sync.message.NotificationSettingsSyncMessage;
import com.clouder.watch.common.utils.SettingsKey;
import com.clouder.watch.mobile.SyncService;

/**
 * 通知设置同步
 * Created by yang_shoulai on 8/17/2015.
 */
public class NotificationSettingsHandler implements IHandler<NotificationSettingsSyncMessage>, IMessageListener {

    private static final String TAG = "NotiSettingsHandler";

    private SyncService syncService;

    public NotificationSettingsHandler(SyncService syncService) {
        this.syncService = syncService;
    }


    @Override
    public void handle(String path, NotificationSettingsSyncMessage message) {
        SyncMessage.Method method = message.getMethod();
        if (method == SyncMessage.Method.Set) {
            boolean enable = message.isEnable();
            boolean shock = message.isShock();
            Log.i(TAG, String.format("Receive notification settings message form handle device, notification enable [%s] and shock [%s]", enable, shock));
            Settings.System.putInt(syncService.getContentResolver(), SettingsKey.NOTIFICATION_PUSH_ENABLE, enable ? 1 : 0);
            Settings.System.putInt(syncService.getContentResolver(), SettingsKey.NOTIFICATION_SHOCK_ENABLE, shock ? 1 : 0);
        } else if (method == SyncMessage.Method.Get) {
            message.setEnable(Settings.System.getInt(syncService.getContentResolver(), SettingsKey.NOTIFICATION_PUSH_ENABLE, 1) == 1);
            message.setShock(Settings.System.getInt(syncService.getContentResolver(), SettingsKey.NOTIFICATION_SHOCK_ENABLE, 1) == 1);
            syncService.sendMessage(message);
        }
    }

    @Override
    public void onMessageReceived(String path, SyncMessage message) {
        handle(path, (NotificationSettingsSyncMessage) message);
    }
}
