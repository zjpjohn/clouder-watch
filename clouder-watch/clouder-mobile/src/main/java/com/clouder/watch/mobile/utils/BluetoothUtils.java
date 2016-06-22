package com.clouder.watch.mobile.utils;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by yang_shoulai on 7/28/2015.
 */
public class BluetoothUtils {

    private static final String TAG = "BluetoothUtils";


    /**
     * 取消用户输入
     *
     * @param device
     * @return
     */
    public static boolean cancelPairingUserInput(BluetoothDevice device) {
        try {
            Method createBondMethod = device.getClass().getMethod("cancelPairingUserInput");
            Boolean returnValue = (Boolean) createBondMethod.invoke(device);
            return returnValue.booleanValue();
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "NoSuchMethodException", e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "InvocationTargetException", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "IllegalAccessException", e);
        }
        return false;
    }


    public static byte[] convertPinToBytes(String pin) {
        if (pin == null) {
            return null;
        }
        byte[] pinBytes;
        try {
            pinBytes = pin.getBytes("UTF-8");
        } catch (UnsupportedEncodingException uee) {
            Log.e("BluetoothUtils", "UTF-8 not supported?!?");  // this should not happen
            return null;
        }
        if (pinBytes.length <= 0 || pinBytes.length > 16) {
            return null;
        }
        return pinBytes;
    }


    /**
     * 反射调用BluetoothDevice的removeBond()方法
     */
    public static void removeBond(BluetoothDevice device) {
        Log.d(TAG, "remove bond!");
        try {
            Method removeBond = device.getClass().getMethod("removeBond");
            removeBond.invoke(device);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "NoSuchMethodException", e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "InvocationTargetException", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "IllegalAccessException", e);
        }
    }

    /**
     * 反射调用BluetoothDevice的createBond方法
     */
    public static void createBond(BluetoothDevice device) {
        Log.d(TAG, "start pairing to watch");
        try {
            Method createBond = device.getClass().getMethod("createBond");
            createBond.invoke(device);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "NoSuchMethodException", e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "InvocationTargetException", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "IllegalAccessException", e);
        }
    }
}
