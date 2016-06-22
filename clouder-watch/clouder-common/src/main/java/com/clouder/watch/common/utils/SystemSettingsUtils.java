package com.clouder.watch.common.utils;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by yang_shoulai on 6/30/2015.
 */
public class SystemSettingsUtils {


    private static final String TAG = "SystemLightnessUtils";

    /**
     * 屏幕处于非长亮模式的失效时间
     */
    public static final int SCREEN_TIEMOUT = 30000;


    /**
     * determine whether system lightness is adjust automatic
     *
     * @param context
     * @return
     */
    public static boolean isAutomaticBrightness(Context context) {
        try {
            boolean autoLight = Settings.System.getInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
            return autoLight;


        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "SettingNotFoundException", e);
        }
        return false;
    }

    /**
     * get system lightness,from 0 to 255
     *
     * @param context
     * @return
     */
    public static int getLightness(Context context) {
        return Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, -1);
    }

    /**
     * 判断当前屏幕亮度的等级
     *
     * @param context
     * @param brightness
     * @return -1，自动
     */
    public static int judgeBrightnessLevel(Context context, int brightness) {

        if (isAutomaticBrightness(context)) {
            return -1;
        }
        if (brightness <= 50) {
            return 1;
        } else if (brightness <= 100) {
            return 2;
        } else if (brightness <= 150) {
            return 3;
        } else if (brightness <= 200) {
            return 4;
        } else if (brightness <= 255) {
            return 5;
        }
        return -1;
    }

    /**
     * stop automatic adjust system lightness
     *
     * @param context
     */
    public static void stopAutoBrightness(Context context) {
        Settings.System.putInt(context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
    }

    /**
     * start automatic adjust system lightness
     *
     * @param context
     */
    public static void startAutoBrightness(Context context) {
        Settings.System.putInt(context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
    }

    public static void setBrightness(Context context, int value) {
        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, value);

    }


    /**
     * 取回当前系统的情景模式
     *
     * @param context
     * @return 0 静音模式， 1 震动模式， 2 正常模式
     */
    public static int getSystemSceneMode(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return audioManager.getRingerMode();
    }


    /**
     * 设置系统的情景模式
     */
    public static void setSystemSceneMode(Context context, int mode) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setRingerMode(mode);
    }

    /**
     * 判断当前系统是否处于飞行模式
     *
     * @param context
     * @return
     */
    public static boolean isAirPlaneOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
    }

    /**
     * 设置飞行模式
     * broadcast air plane mode change event so other apps can receive it
     * require permission[android.permission.WRITE_SECURE_SETTINGS]
     *
     * @param context
     */
    public static void setAirPlaneOn(Context context, boolean on) {
        Settings.Global.putInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, on ? 1 : 0);
        Intent intent = new Intent("clouder.watch.action.AIRPLANE_MODE_CHANGE");
        intent.putExtra("state", on);
        context.sendBroadcast(intent);
    }


    public static void main(String[] args) throws ClassNotFoundException {
        Class<?> userHandleClass = Class.forName("android.os.UserHandle");
        try {
            Method[] fields = userHandleClass.getMethods();
            System.out.println(fields.length);
            if (fields != null && fields.length > 0) {
                for (Method field : fields) {
                    System.out.println(field);
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断系统是否处于长亮模式
     *
     * @param context
     * @return
     */
    public static boolean isAlwaysWakeOn(Context context) {
        int timeout = 0;
        try {
            timeout = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return timeout < 0 || timeout == Integer.MAX_VALUE;
    }

    /**
     * 设置屏幕休眠时间
     * Integer.MAX_VALUE即是长亮模式
     *
     * @param context
     * @param always
     */
    public static void setScreenAlwaysWakeOn(Context context, boolean always) {

        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, always ? Integer.MAX_VALUE : SCREEN_TIEMOUT);
    }


    /**
     * reboot device, require permission[android.permission.REBOOT]
     */
    public static void reboot(Context context, String reason) {
        PowerManager pManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        pManager.reboot(reason);
    }

    /**
     * shutdown device, require permission[android.permission.SHUTDOWN]
     */
    public static void shutdown() {
        try {
            //获得ServiceManager类
            Class<?> ServiceManager = Class
                    .forName("android.os.ServiceManager");
            //获得ServiceManager的getService方法
            Method getService = ServiceManager.getMethod("getService", String.class);
            //调用getService获取RemoteService
            Object oRemoteService = getService.invoke(null, Context.POWER_SERVICE);
            //获得IPowerManager.Stub类
            Class<?> cStub = Class.forName("android.os.IPowerManager$Stub");
            //获得asInterface方法
            Method asInterface = cStub.getMethod("asInterface", android.os.IBinder.class);
            //调用asInterface方法获取IPowerManager对象
            Object oIPowerManager = asInterface.invoke(null, oRemoteService);
            //获得shutdown()方法
            Method shutdown = oIPowerManager.getClass().getMethod("shutdown", boolean.class, boolean.class);
            //调用shutdown()方法
            shutdown.invoke(oIPowerManager, false, true);
        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
        }
    }
}
