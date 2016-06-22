package com.clouder.watch.common.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by yang_shoulai on 7/10/2015.
 */
public class BluetoothHelper {

    private static final String TAG = "BluetoothHelper";

    private BluetoothAdapter mAdapter;

    public BluetoothHelper() {
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * 判断设备是否支持蓝牙
     *
     * @return
     */
    public boolean isSupportBT() {
        return mAdapter != null;
    }

    /**
     * 判断设备蓝牙是否开启
     *
     * @return
     */
    public boolean isOn() {
        return mAdapter.isEnabled();
    }

    /**
     * 打开蓝牙
     */
    public void turnOn() {
        mAdapter.enable();
    }

    /**
     * 关闭蓝牙
     */
    public void turnOff() {
        mAdapter.disable();
    }

    /**
     * 取的设备蓝牙地址
     *
     * @return
     */
    public String getBlueToothAddress() {
        return mAdapter.getAddress();
    }

    /**
     * 返回蓝牙状态
     *
     * @return
     */
    public int getBluetoothState() {
        return mAdapter.getState();
    }


    public boolean setScanMode(int mode, int duration) {
        try {
            Method setScanMode = mAdapter.getClass().getMethod("setScanMode", int.class, int.class);
            return (Boolean) setScanMode.invoke(mAdapter, mode, duration);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "反射调用setScanMode方法异常", e);
        }
        return false;
    }

    public String getBlueToothName() {
        return mAdapter.getName();
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
     * 取消用户输入
     *
     * @param device
     * @return
     * @throws Exception
     */
    public static boolean cancelPairingUserInput(BluetoothDevice device) throws Exception {

        Method createBondMethod = device.getClass().getMethod("cancelPairingUserInput");
        Boolean returnValue = (Boolean) createBondMethod.invoke(device);
        return returnValue.booleanValue();

    }

    /**
     * 清空所有和手表绑定的蓝牙设备
     */
    public void removeAllBondedDevices() {
        Set<BluetoothDevice> set = mAdapter.getBondedDevices();
        if (set != null && !set.isEmpty()) {
            Iterator<BluetoothDevice> iterator = set.iterator();
            while (iterator.hasNext()) {
                removeBond(iterator.next());
            }
        }
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
