package com.clouder.watch.mobile.handler;

import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;

import com.clouder.watch.common.sync.IHandler;
import com.clouder.watch.common.sync.IMessageListener;
import com.clouder.watch.common.sync.SyncMessage;
import com.clouder.watch.common.sync.message.UninstallApkSyncMessage;
import com.clouder.watch.common.utils.Constant;
import com.clouder.watch.common.utils.StringUtils;
import com.clouder.watch.mobile.SyncService;

/**
 * Created by yang_shoulai on 10/26/2015.
 */
public class UninstallApkHandler implements IHandler<UninstallApkSyncMessage>, IMessageListener {

    private static final String TAG = "UninstallApkHandler";

    public SyncService syncService;

    public UninstallApkHandler(SyncService syncService) {
        this.syncService = syncService;
    }

    @Override
    public void handle(String path, UninstallApkSyncMessage message) {
        Log.d(TAG, message.toString());
        String packageName = message.getPackageName();
        if (!StringUtils.isEmpty(packageName)) {
            Intent installService = new Intent();
            installService.setComponent(new ComponentName(Constant.CLOUDER_INSTALLER_PKG, Constant.CLOUDER_INSTALLER_SERVICE));
            installService.putExtra("package", packageName);
            installService.putExtra("install", false);
            try {
                syncService.startService(installService);
            } catch (Exception e) {
                Log.e(TAG, "Error when start installer service!", e);
            }
        }

    }

    @Override
    public void onMessageReceived(String path, SyncMessage message) {
        handle(path, (UninstallApkSyncMessage) message);
    }
}
