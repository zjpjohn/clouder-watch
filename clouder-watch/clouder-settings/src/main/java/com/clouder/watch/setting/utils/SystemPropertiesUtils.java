package com.clouder.watch.setting.utils;

import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by yang_shoulai on 7/29/2015.
 */
public class SystemPropertiesUtils {

    private static final String TAG = "SystemPropertiesUtils";

    private static final int SYSPROPS_TRANSACTION = ('_' << 24) | ('S' << 16) | ('P' << 8) | 'R';

    /**
     * 反射调用android.os.SystemProperties#get(String,String)方法
     *
     * @param key
     * @param def
     * @return
     */
    public static String getString(String key, String def) {
        try {
            Class<?> clz = Class.forName("android.os.SystemProperties");
            Method method = clz.getMethod("get", String.class, String.class);
            Object object = method.invoke(null, new Object[]{key, def});
            return object.toString();
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "ClassNotFoundException", e);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "NoSuchMethodException", e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "InvocationTargetException", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "IllegalAccessException", e);
        }
        return def;
    }

    /**
     * 反射调用android.os.SystemProperties#set(String,String)方法
     *
     * @param key
     * @param val
     */
    public static void set(String key, String val) {
        try {
            Class<?> clz = Class.forName("android.os.SystemProperties");
            Method method = clz.getMethod("set", String.class, String.class);
            method.invoke(null, new Object[]{key, val});
            new SystemPropPoker().execute();
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "ClassNotFoundException", e);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "NoSuchMethodException", e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "InvocationTargetException", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "IllegalAccessException", e);
        }
    }

    static class SystemPropPoker extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            String[] services = ServiceManagerUtils.listServices();
            for (String service : services) {
                IBinder obj = ServiceManagerUtils.checkService(service);
                if (obj != null) {
                    Parcel data = Parcel.obtain();
                    try {
                        obj.transact(SYSPROPS_TRANSACTION, data, null, 0);
                    } catch (RemoteException e) {
                    } catch (Exception e) {
                        Log.i(TAG, "Someone wrote a bad service '" + service
                                + "' that doesn't like to be poked: " + e);
                    }
                    data.recycle();
                }
            }
            return null;
        }
    }
}
