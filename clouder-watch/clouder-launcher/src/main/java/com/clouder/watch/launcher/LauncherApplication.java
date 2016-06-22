package com.clouder.watch.launcher;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.widget.Toast;

import com.clouder.watch.common.sync.SyncServiceHelper;
import com.clouder.watch.common.utils.BluetoothHelper;
import com.clouder.watch.common.utils.SettingsKey;
import com.clouder.watch.common.widget.WatchToast;

import java.util.List;

/**
 * Created by yang_shoulai on 6/25/2015.
 */
public class LauncherApplication extends Application {

    private static final String TAG = "LauncherApplication";


    @Override
    public void onCreate() {
        super.onCreate();

        if (Settings.System.getInt(getContentResolver(), SettingsKey.DEVICE_PAIRED, 0) == 1) {
            Log.d(TAG, "Device has bonded with handle device, Sync service will start!");
            Intent syncService = new Intent();
            syncService.setComponent(new ComponentName(SyncServiceHelper.CLOUDER_SYNC_SERVICE_PKG, SyncServiceHelper.CLOUDER_SYNC_SERVICE));
            startService(syncService);
        }
        //启动同步服务应该是设备已经完成配对之后
        //这里只是用来开发测试
        //真实环境时需要使用上面注释掉的代码
        /*Intent syncService = new Intent();
        syncService.setComponent(new ComponentName(SyncServiceHelper.CLOUDER_SYNC_SERVICE_PKG, SyncServiceHelper.CLOUDER_SYNC_SERVICE));
        startService(syncService);*/
    }

}
