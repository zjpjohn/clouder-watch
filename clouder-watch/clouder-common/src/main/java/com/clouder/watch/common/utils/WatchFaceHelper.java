package com.clouder.watch.common.utils;

import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.service.wallpaper.WallpaperService;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by yang_shoulai on 2015/8/16.
 */
public class WatchFaceHelper {

    private static final String TAG = "WatchFaceHelper";

    /**
     * 设置表盘壁纸
     */
    public static void setWatchFace(Context context, LiveWallpaperInfo watchFacePreview) {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
        Class<WallpaperManager> c = WallpaperManager.class;
        try {
            Method method = c.getMethod("getIWallpaperManager");
            Object manager = method.invoke(wallpaperManager);
            Method m = manager.getClass().getMethod("setWallpaperComponent", ComponentName.class);
            m.invoke(manager, watchFacePreview.intent.getComponent());
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "NoSuchMethodException", e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "InvocationTargetException", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "IllegalAccessException", e);
        }


    }

    /**
     * 加载全部已安装的表盘
     *
     * @param context
     * @return
     */
    public static List<LiveWallpaperInfo> loadWatchFaces(Context context) {
        Intent intentFilter = new Intent(WallpaperService.SERVICE_INTERFACE);
        intentFilter.addCategory("com.google.android.wearable.watchface.category.WATCH_FACE");
        List<ResolveInfo> list = context.getPackageManager().queryIntentServices(intentFilter, PackageManager.GET_META_DATA);
        if (list != null && !list.isEmpty()) {
            return generateWatchFacePreview(context, list);
        }
        return new ArrayList<>();
    }


    private static List<LiveWallpaperInfo> generateWatchFacePreview(Context context, List<ResolveInfo> list) {
        final PackageManager packageManager = context.getPackageManager();
        List<LiveWallpaperInfo> watchFaces = new ArrayList<>(list.size());
        Collections.sort(list, new Comparator<ResolveInfo>() {
            final Collator mCollator;

            {
                mCollator = Collator.getInstance();
            }

            public int compare(ResolveInfo info1, ResolveInfo info2) {
                return mCollator.compare(info1.loadLabel(packageManager), info2.loadLabel(packageManager));
            }
        });
        for (ResolveInfo resolveInfo : list) {
            WallpaperInfo info;
            try {
                info = new WallpaperInfo(context, resolveInfo);
            } catch (XmlPullParserException e) {
                Log.w(TAG, "Skipping wallpaper " + resolveInfo.serviceInfo, e);
                continue;
            } catch (IOException e) {
                Log.w(TAG, "Skipping wallpaper " + resolveInfo.serviceInfo, e);
                continue;
            }
            LiveWallpaperInfo wallpaper = new LiveWallpaperInfo();
            wallpaper.name = resolveInfo.loadLabel(packageManager).toString();
            wallpaper.intent = new Intent(WallpaperService.SERVICE_INTERFACE);
            wallpaper.intent.setClassName(info.getPackageName(), info.getServiceName());
            wallpaper.info = info;
            Bundle metaData = resolveInfo.serviceInfo.metaData;
            int previewId = metaData.getInt("com.google.android.wearable.watchface.preview");
            int circlePreviewId = metaData.getInt("com.google.android.wearable.watchface.preview_circular");
            int clockPreviewId = metaData.getInt("com.google.android.clockwork.home.preview");
            int circleClockPreviewId = metaData.getInt("com.google.android.clockwork.home.preview_circular");
            try {
                Context c = context.createPackageContext(resolveInfo.serviceInfo.packageName, Context.CONTEXT_IGNORE_SECURITY);
                Drawable drawable = null;
                if (previewId != 0) {
                    drawable = c.getResources().getDrawable(previewId);
                } else if (circlePreviewId != 0) {
                    drawable = c.getResources().getDrawable(circlePreviewId);
                } else if (clockPreviewId != 0) {
                    drawable = c.getResources().getDrawable(clockPreviewId);
                } else if (circleClockPreviewId != 0) {
                    drawable = c.getResources().getDrawable(circleClockPreviewId);
                }
                wallpaper.thumbnail = drawable;
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Can not create package context, package name is " + resolveInfo.activityInfo.packageName, e);
            }

            String configuration = metaData.getString("com.google.android.wearable.watchface.wearableConfigurationAction");
            if (configuration != null && configuration.trim().length() > 0) {
                Intent intent = new Intent(configuration);
                intent.addCategory("com.google.android.wearable.watchface.category.WEARABLE_CONFIGURATION");
                intent.addCategory("android.intent.category.DEFAULT");
                wallpaper.configIntent = intent;
            }
            watchFaces.add(wallpaper);
        }
        return watchFaces;
    }


    /**
     * 判断当前系统是否已经有表盘在使用
     *
     * @return
     */
    public static boolean hasWatchFace(Context context) {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
        WallpaperInfo wallpaperInfo = wallpaperManager.getWallpaperInfo();
        return !(wallpaperInfo == null);
    }


    public static class LiveWallpaperInfo {
        public String name;
        public Drawable thumbnail;
        public WallpaperInfo info;
        public Intent intent;
        public Intent configIntent;
    }
}
