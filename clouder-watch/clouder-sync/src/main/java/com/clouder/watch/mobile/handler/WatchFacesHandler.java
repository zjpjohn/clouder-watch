package com.clouder.watch.mobile.handler;

import android.util.Log;

import com.clouder.watch.common.sync.IHandler;
import com.clouder.watch.common.sync.IMessageListener;
import com.clouder.watch.common.sync.SyncMessage;
import com.clouder.watch.common.sync.SyncMessagePathConfig;
import com.clouder.watch.common.sync.SyncUtils;
import com.clouder.watch.common.sync.message.WatchFaceSyncMessage;
import com.clouder.watch.common.utils.Constant;
import com.clouder.watch.common.utils.WatchFaceHelper;
import com.clouder.watch.mobile.SyncService;

import java.util.ArrayList;
import java.util.List;

/**
 * 处理表盘信息同步
 * Created by yang_shoulai on 8/17/2015.
 */
public class WatchFacesHandler implements IHandler<WatchFaceSyncMessage>, IMessageListener {

    public static final String TAG = "WatchFacesHandler";

    public SyncService syncService;

    public WatchFacesHandler(SyncService syncService) {
        this.syncService = syncService;
    }

    @Override
    public void handle(String path, WatchFaceSyncMessage message) {
        Log.i(TAG, "Receive watch face sync request and will start to push watch faces to handle device.");
        boolean needSync = message.isNeedSync();
        List<WatchFaceHelper.LiveWallpaperInfo> watchFaces = WatchFaceHelper.loadWatchFaces(syncService);
        if (!needSync) {
            List<WatchFaceSyncMessage.WatchFace> list1 = new ArrayList<>();
            if (watchFaces != null && !watchFaces.isEmpty()) {
                Log.d(TAG, String.format("Load [%s] watch faces.", watchFaces.size()));
                for (WatchFaceHelper.LiveWallpaperInfo watchFace : watchFaces) {
                    WatchFaceSyncMessage.WatchFace wf1 = new WatchFaceSyncMessage.WatchFace();
                    wf1.setName(watchFace.name);
                    wf1.setPackageName(watchFace.info.getPackageName());
                    wf1.setServiceName(watchFace.info.getServiceName());
                    list1.add(wf1);
                }
            } else {
                Log.e(TAG, "NO WATCH FACE FOUND!");
            }
            message.setWatchFaces(list1);
            syncService.sendMessage(message);

        } else {
            List<WatchFaceSyncMessage.WatchFace> list = new ArrayList<>();
            if (watchFaces != null && !watchFaces.isEmpty()) {
                Log.d(TAG, String.format("Load [%s] watch faces.", watchFaces.size()));
                for (WatchFaceHelper.LiveWallpaperInfo watchFace : watchFaces) {
                    WatchFaceSyncMessage.WatchFace wf = new WatchFaceSyncMessage.WatchFace();
                    wf.setName(watchFace.name);
                    wf.setPackageName(watchFace.info.getPackageName());
                    wf.setServiceName(watchFace.info.getServiceName());
                    wf.setThumbnail(SyncUtils.bitmap2Bytes(SyncUtils.drawableToBitmap(watchFace.thumbnail)));
                    list.add(wf);
                }

            } else {
                Log.e(TAG, "NO WATCH FACE FOUND!");
            }
            if (!list.isEmpty()) {
                syncService.syncWatchFaces(list);
            }

        }
    }

    @Override
    public void onMessageReceived(String path, SyncMessage message) {
        handle(path, (WatchFaceSyncMessage) message);
    }
}
