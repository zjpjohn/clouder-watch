package com.clouder.watch.common.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

/**
 * Created by yang_shoulai on 7/16/2015.
 */
public class SettingsKey {


    /**
     * 设备是否配对过 1,已经配对;0,未配对
     */
    public static String DEVICE_PAIRED = "com.clouder.watch.settings.device_paired";

    /**
     * 设备配对手机的蓝牙MAC地址
     */
    public static String DEVICE_PAIRED_BT_ADDRESS = "com.clouder.watch.settings.device_paired_bt_address";

    /**
     * 绑定的手机蓝牙名称
     */
    public static final String DEVICE_PAIRED_BT_NAME = "com.clouder.watch.settings.device_paired_bt_name";

    /**
     * 选择的手表表盘包名
     */
    public static final String WATCH_FACE_PKG = "com.clouder.watch.settings.watch_face_pkg";

    /**
     * 选择的手表表盘Service名称
     */
    public static final String WATCH_FACE_SERVICE_NAME = "com.clouder.watch.settings.watch_face_service";


    /**
     * 手表亮度 -1表示自动
     */
    public static final String BRIGHTNESS_LEVEL = "com.clouder.watch.settings.brightness_level";

    /**
     * 手表是否设置为长亮模式
     */
    public static final String WAKE_ON = "com.clouder.watch.settings.wake_on";

    /**
     * 省电选项，抬手是否唤醒屏幕
     */
    public static final String AWAKEN_ON_HANDS_UP = "com.clouder.watch.settings.awaken_on_hands_up";

    /**
     * 省电选项，充电时是否长亮
     */
    public static final String AWAKEN_ON_CHARGING = "com.clouder.watch.settings.awaken_on_charging";

    /**
     * 热词搜索是否唤醒语音搜索
     */
    public static final String AWAKEN_VOICE_SEARCH_ON_HOT_WORD = "com.clouder.watch.settings.awaken_voice_search_on_hot_word";

    /**
     * 手势是否唤醒语音搜索
     */
    public static final String AWAKEN_VOICE_SEARCH_ON_GESTURE = "com.clouder.watch.settings.awaken_voice_search_on_gesture";

    /**
     * 是否启用锁屏
     * 0,不启用
     * 1,启用
     */
    public static final String SCREEN_LOCKER_ENABLE = "com.clouder.watch.settings.screen_locker_enable";


    /**
     * 屏幕锁密码
     */
    public static final String SCREEN_LOCKER_PWD = "com.clouder.watch.settings.screen_locker_pwd";

    /**
     * 是否启用通知推送
     * 1，启用，表示应用可以弹出通知
     * 0，不启用，表示应用无法弹出通知
     */
    public static final String NOTIFICATION_PUSH_ENABLE = "com.clouder.watch.settings.notification_push_enable";

    /**
     * 应用弹出通知时是否震动
     * 1，震动
     * 2，不震动
     */
    public static final String NOTIFICATION_SHOCK_ENABLE = "com.clouder.watch.settings.notification_shock_enable";

    /**
     * 弹出通知应用的黑名单
     * 处于黑名单的应用即使在通知开关打开的情况下仍然无法弹出通知
     * 以","分隔
     */
    public static final String NOTIFICATION_BLACK_LIST = "com.clouder.watch.settings.notification_black_list";

    /**
     * 蓝牙调试
     */
    public static final String BLUETOOTH_DEBUG_KEY = "com.clouder.watch.setting.bluetooth_debug";

    /**
     * 配对设备的手机号码
     */
    public static final String PAIRED_DEVICE_PHONENUMBER = "com.clouder.watch.settings.paired_device_phonenumber";

    /**
     * 取得屏幕锁的密码
     *
     * @return
     */
    public static String getLockPassword(Context context) {
        try {
            Context cont = context.createPackageContext(Constant.CLOUDER_LOCKER_PKG, Context.CONTEXT_IGNORE_SECURITY);
            SharedPreferences sharedPreferences = cont.getSharedPreferences("configuration", Context.MODE_WORLD_WRITEABLE);
            return sharedPreferences.getString("PassWord", null);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 设置屏幕锁的密码
     *
     * @param password
     */
    public static void setLockPassword(Context context, String password) {
        try {
            Context cont = context.createPackageContext(Constant.CLOUDER_LOCKER_PKG, Context.CONTEXT_IGNORE_SECURITY);
            SharedPreferences sharedPreferences = cont.getSharedPreferences("configuration", 0);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("PassWord", password);
            editor.commit();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}
