package com.clouder.watch.setting;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.clouder.watch.common.utils.BluetoothHelper;
import com.clouder.watch.common.utils.WifiHelper;

/**
 * Created by yang_shoulai on 11/25/2015.
 */
public class AirPlaneModeChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("clouder.watch.action.AIRPLANE_MODE_CHANGE".equals(intent.getAction())) {
            boolean on = intent.getBooleanExtra("state", false);
            if (on) {
                Log.d("AirPlaneModeChange", "检测到飞行模式已经打开，关闭蓝牙和WI-FI");
                BluetoothHelper bluetoothHelper = new BluetoothHelper();
                bluetoothHelper.turnOff();

                WifiHelper wifiHelper = new WifiHelper(context);
                wifiHelper.close();
            }
        } else {
            BluetoothHelper bluetoothHelper = new BluetoothHelper();
            bluetoothHelper.turnOn();

            WifiHelper wifiHelper = new WifiHelper(context);
            wifiHelper.open();
        }
    }
}
