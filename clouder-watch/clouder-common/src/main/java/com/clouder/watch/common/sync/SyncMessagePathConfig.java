package com.clouder.watch.common.sync;

/**
 * 调用CMS Message Api时的 path
 * Created by yang_shoulai on 8/3/2015.
 */
public class SyncMessagePathConfig {

    /**
     * 同步所有已经安装的表盘
     */
    public static final String WATCH_FACES = "/watch_faces";

    /**
     * 同步当前已经在使用的表盘或者将要设置为使用的表盘
     */
    public static final String CURRENT_WATCH_FACE = "/current_watch_face";

    /**
     * 同步所有的wearable apk
     */
    public static final String WEARABLE_APK = "/wearable_apk";

    /**
     * 同步通知设置
     * 包含是否启用全局通知设置和通知到达时是否震动
     */
    public static final String NOTIFICATION_SETTINGS = "/notification_settings";

    /**
     * 同步wifi设置信息，包含手机当前正在使用的wifi连接信息，wifi名和密码等
     */
    public static final String WIFI_INFO = "/wifi_info";

    /**
     * 同步所有的锁屏设置信息，包含是否启用锁屏和锁屏密码
     */
    public static final String SCREEN_LOCKER_SETTINGS = "/screen_locker_settings";

    /**
     * 同步所有处于通知黑名单中的应用
     */
    public static final String NOTIFICATION_BLACK_LIST = "/notification_black_list";

    /**
     * 查找手机
     */
    public static final String SEARCH_PHONE = "/search_phone";


    public static final String SOCKET_CONNECTED = "/socket_connected";

    /**
     * 同步卸载app
     */
    public static final String UNINSTALL_APK = "/uninstall_apk";


    /**
     * 当前时间同步
     */
    public static final String CURRENT_TIME = "/current_time";

    /**
     * 同步手机号码
     */
    public static final String SYNC_PHONE_NUMBER = "/phone_number";
}
