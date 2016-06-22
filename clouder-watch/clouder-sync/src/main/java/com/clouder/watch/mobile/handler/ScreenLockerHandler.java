package com.clouder.watch.mobile.handler;

import android.provider.Settings;
import android.util.Log;

import com.clouder.watch.common.sync.IHandler;
import com.clouder.watch.common.sync.IMessageListener;
import com.clouder.watch.common.sync.SyncMessage;
import com.clouder.watch.common.sync.message.ScreenLockerSyncMessage;
import com.clouder.watch.common.utils.SettingsKey;
import com.clouder.watch.mobile.SyncService;

/**
 * 屏幕锁设置同步处理
 * Created by yang_shoulai on 8/17/2015.
 */
public class ScreenLockerHandler implements IHandler<ScreenLockerSyncMessage>, IMessageListener {

    private static final String TAG = "ScreenLockerHandler";

    private SyncService syncService;

    public ScreenLockerHandler(SyncService syncService) {
        this.syncService = syncService;
    }


    @Override
    public void handle(String path, ScreenLockerSyncMessage message) {
        SyncMessage.Method method = message.getMethod();
        if (method == SyncMessage.Method.Set) {
            boolean enable = message.isEnable();
            String password = message.getPassword();
            Log.i(TAG, String.format("Receive screen locker settings message! screen locker enable [%s] and password [%s]", enable, password));
            Settings.System.putInt(syncService.getContentResolver(), SettingsKey.SCREEN_LOCKER_ENABLE, enable ? 1 : 0);
            SettingsKey.setLockPassword(syncService, password);
        } else if (method == SyncMessage.Method.Get) {
            message.setPassword(SettingsKey.getLockPassword(syncService));
            message.setEnable(Settings.System.getInt(syncService.getContentResolver(), SettingsKey.SCREEN_LOCKER_ENABLE, 0) == 1);
            syncService.sendMessage(message);
        }

    }

    @Override
    public void onMessageReceived(String path, SyncMessage message) {
        handle(path, (ScreenLockerSyncMessage) message);
    }
}
