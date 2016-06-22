package com.clouder.watch.mobile.sync.app;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.clouder.watch.common.utils.StringUtils;
import com.clouder.watch.mobile.SyncService;

/**
 * Created by yang_shoulai on 8/31/2015.
 */
public class AppChangeReceiver extends BroadcastReceiver {

    private static final String TAG = "AppChangeReceiver";

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        String packageName = intent.getData().getSchemeSpecificPart();
        if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
            Log.d(TAG, "app " + packageName + " has been removed!");
            Intent syncService = new Intent(context, SyncService.class);
            syncService.putExtra("mobilePackage", packageName);
            syncService.putExtra("install",false);
            context.startService(syncService);

        } else {
            Log.d(TAG, "app " + packageName + " has been installed or replaced!");
            if (!StringUtils.isEmpty(packageName)) {
                Intent syncService = new Intent(context, SyncService.class);
                syncService.putExtra("mobilePackage", packageName);
                syncService.putExtra("install",true);
                context.startService(syncService);
            }

        }
    }


}
