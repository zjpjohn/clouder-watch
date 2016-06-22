package com.clouder.watch.common.sync;

import com.clouder.watch.common.sync.message.CurrentTimeSyncMessage;
import com.clouder.watch.common.sync.message.CurrentWatchFaceSyncMessage;
import com.clouder.watch.common.sync.message.NotificationBlackListSyncMessage;
import com.clouder.watch.common.sync.message.NotificationSettingsSyncMessage;
import com.clouder.watch.common.sync.message.PhoneNumberSyncMessage;
import com.clouder.watch.common.sync.message.ScreenLockerSyncMessage;
import com.clouder.watch.common.sync.message.SearchPhoneSyncMessage;
import com.clouder.watch.common.sync.message.SocketConnectSyncMessage;
import com.clouder.watch.common.sync.message.UninstallApkSyncMessage;
import com.clouder.watch.common.sync.message.WatchFaceSyncMessage;
import com.clouder.watch.common.sync.message.WifiSyncMessage;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

/**
 * 同步消息解析,将字节数组反解成为同步消息的Java Bean
 * <p/>
 * Created by yang_shoulai on 8/17/2015.
 */
public class SyncMessageParser {

    private static Map<String, Class<? extends SyncMessage>> mapper = new HashMap<>();

    static {
        mapper.put(SyncMessagePathConfig.WATCH_FACES, WatchFaceSyncMessage.class);
        mapper.put(SyncMessagePathConfig.CURRENT_WATCH_FACE, CurrentWatchFaceSyncMessage.class);
        mapper.put(SyncMessagePathConfig.NOTIFICATION_SETTINGS, NotificationSettingsSyncMessage.class);
        mapper.put(SyncMessagePathConfig.WIFI_INFO, WifiSyncMessage.class);
        mapper.put(SyncMessagePathConfig.SCREEN_LOCKER_SETTINGS, ScreenLockerSyncMessage.class);
        mapper.put(SyncMessagePathConfig.NOTIFICATION_BLACK_LIST, NotificationBlackListSyncMessage.class);
        mapper.put(SyncMessagePathConfig.SEARCH_PHONE, SearchPhoneSyncMessage.class);
        mapper.put(SyncMessagePathConfig.SOCKET_CONNECTED, SocketConnectSyncMessage.class);
        mapper.put(SyncMessagePathConfig.UNINSTALL_APK, UninstallApkSyncMessage.class);
        mapper.put(SyncMessagePathConfig.CURRENT_TIME, CurrentTimeSyncMessage.class);
        mapper.put(SyncMessagePathConfig.SYNC_PHONE_NUMBER, PhoneNumberSyncMessage.class);
    }

    public static Class<? extends SyncMessage> getSyncMessageClass(String path) {
        return mapper.get(path);
    }

    public static boolean containsPath(String path) {
        return mapper.containsKey(path);
    }

    public static SyncMessage parse(String path, byte[] data) {
        if (!containsPath(path)) {
            return null;
        }
        String res = new String(data);
        SyncMessage message = new Gson().fromJson(res, getSyncMessageClass(path));
        return message;
    }

    public static SyncMessage parse(String path, String data) {
        if (!containsPath(path)) {
            return null;
        }
        SyncMessage message = new Gson().fromJson(data, getSyncMessageClass(path));
        return message;
    }
}
