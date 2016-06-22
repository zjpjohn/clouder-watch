package com.clouder.watch.setting.utils;

import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by yang_shoulai on 7/30/2015.
 * <p/>
 * 反射调用 android.os.ServiceManager的方法
 */
public class ServiceManagerUtils {

    private static final String TAG = "ServiceManagerUtils";


    public static String[] listServices() {
        try {
            Class<?> clz = Class.forName("android.os.ServiceManager");
            Method method = clz.getMethod("listServices");
            return (String[]) method.invoke(null);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "ClassNotFoundException", e);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "NoSuchMethodException", e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "InvocationTargetException", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "IllegalAccessException", e);
        }
        return new String[]{};
    }


    public static IBinder checkService(String name) {
        try {
            Class<?> clz = Class.forName("android.os.ServiceManager");
            Method method = clz.getMethod("checkService", String.class);
            return (IBinder) method.invoke(null, new Object[]{name});
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "ClassNotFoundException", e);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "NoSuchMethodException", e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "InvocationTargetException", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "IllegalAccessException", e);
        }
        return null;
    }
}
