package com.clouder.watch.mobile.handler;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.provider.Settings;
import android.util.Log;

import com.clouder.watch.common.sync.IHandler;
import com.clouder.watch.common.sync.IMessageListener;
import com.clouder.watch.common.sync.SyncMessage;
import com.clouder.watch.common.sync.message.CurrentWatchFaceSyncMessage;
import com.clouder.watch.common.utils.SettingsKey;
import com.clouder.watch.common.utils.WatchFaceHelper;
import com.clouder.watch.mobile.SyncService;

import java.util.List;

/**
 * 处理当前正在使用的表盘信息同步
 * Created by yang_shoulai on 8/17/2015.
 */
public class CurrentWatchFaceHandler implements IHandler<CurrentWatchFaceSyncMessage>, IMessageListener {
    private static final String TAG = "CurrentWatchFaceHandler";

    private SyncService syncService;

    private ContentResolver resolver;


    public CurrentWatchFaceHandler(SyncService context) {
        this.syncService = context;
        this.resolver = context.getContentResolver();
    }

    @Override
    public void onMessageReceived(String path, SyncMessage message) {
        handle(path, (CurrentWatchFaceSyncMessage) message);
    }

    @Override
    public void handle(String path, CurrentWatchFaceSyncMessage message) {
        String packageName = message.getPackageName();
        String serviceName = message.getServiceName();
        SyncMessage.Method method = message.getMethod();
        if (method == SyncMessage.Method.Get) {
            message.setPackageName(Settings.System.getString(resolver, SettingsKey.WATCH_FACE_PKG));
            message.setServiceName(Settings.System.getString(resolver, SettingsKey.WATCH_FACE_SERVICE_NAME));
            syncService.sendMessage(message);
        } else if (method == SyncMessage.Method.Set) {
            List<WatchFaceHelper.LiveWallpaperInfo> watchFaces = WatchFaceHelper.loadWatchFaces(syncService);
            if (watchFaces != null && !watchFaces.isEmpty()) {
                for (WatchFaceHelper.LiveWallpaperInfo watchFace : watchFaces) {
                    ComponentName componentName = watchFace.intent.getComponent();
                    if (componentName.getPackageName().equals(packageName) && componentName.getClassName().equals(serviceName)) {
                        WatchFaceHelper.setWatchFace(syncService, watchFace);
                        Settings.System.putString(resolver, SettingsKey.WATCH_FACE_PKG, packageName);
                        Settings.System.putString(resolver, SettingsKey.WATCH_FACE_SERVICE_NAME, serviceName);

                        message.setPackageName(packageName);
                        message.setServiceName(serviceName);
                        syncService.sendMessage(message);
                        Log.d(TAG, String.format("Set watch face with package name [%s] and service name [%s].", packageName, serviceName));
                        return;
                    }
                }
            }
            Log.e(TAG, String.format("Failed to set watch face with package name [%s] and service name [%s] as no watch face app found.", packageName, serviceName));
        }

    }
}
