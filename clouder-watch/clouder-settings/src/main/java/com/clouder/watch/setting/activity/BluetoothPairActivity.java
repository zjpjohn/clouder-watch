package com.clouder.watch.setting.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.clouder.watch.common.ui.SwipeRightActivity;
import com.clouder.watch.common.utils.BluetoothHelper;
import com.clouder.watch.common.utils.Constant;
import com.clouder.watch.common.utils.SettingsKey;
import com.clouder.watch.common.utils.SystemSettingsUtils;
import com.clouder.watch.common.widget.WatchDialog;
import com.clouder.watch.common.widget.WatchToast;
import com.clouder.watch.setting.R;
import com.github.yoojia.zxing.QRCodeEncode;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by yang_shoulai on 8/26/2015.
 */
public class BluetoothPairActivity extends SwipeRightActivity {

    private static final String TAG = "BluetoothPairActivity";

    private BluetoothHelper helper;

    private ImageView imageView;

    private TextView airPlaneTip;

    private WatchDialog pairRequestDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_pair);
        initView();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, filter);
    }

    private void initView() {
        imageView = (ImageView) findViewById(R.id.qrCode);
        airPlaneTip = (TextView) findViewById(R.id.tip_airplane);
        boolean airPlaneMode = SystemSettingsUtils.isAirPlaneOn(this);
        if (airPlaneMode) {
            airPlaneTip.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.GONE);
        } else {
            airPlaneTip.setVisibility(View.GONE);
            imageView.setVisibility(View.VISIBLE);
        }
        helper = new BluetoothHelper();
        if (!helper.isSupportBT()) {
            WatchToast.make(this, getString(R.string.device_not_support_bluetooth), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Device does not support bluetooth, activity will finish.");
            finish();
        } else {
            String address = helper.getBlueToothAddress();
            String name = helper.getBlueToothName();
            JSONObject object = new JSONObject();
            try {
                object.put("address", address);
                object.put("name", name);
                Log.d(TAG, "Get device bluetooth address " + address);
            } catch (JSONException e) {
                Log.e(TAG, "JSONException", e);
            }
            imageView.setImageBitmap(encodeQRCode(object.toString()));

            if (!helper.isOn()) {
                Log.d(TAG, "蓝牙没有打开，将要打开蓝牙");
                helper.turnOn();
            } else {
                openDiscoverable();
            }

        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unregisterReceiver(bluetoothReceiver);
    }

    private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                if (state == BluetoothAdapter.STATE_OFF) {
                    Log.d(TAG, "蓝牙已经关闭");
                } else if (state == BluetoothAdapter.STATE_ON) {
                    Log.d(TAG, "蓝牙已经打开");
                    openDiscoverable();
                } else if (state == BluetoothAdapter.STATE_TURNING_OFF || state == BluetoothAdapter.STATE_TURNING_ON) {
                    if (state == BluetoothAdapter.STATE_TURNING_OFF) {
                        WatchToast.make(BluetoothPairActivity.this, getString(R.string.turning_off), Toast.LENGTH_SHORT).show();
                    } else {
                        WatchToast.make(BluetoothPairActivity.this, getString(R.string.turning_on), Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int mType = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR);
                if (mType == BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION) {
                    Log.d(TAG, "接收到蓝牙配对请求，设备名" + device.getName() + ", 配对方法为简单配对");
                } else if (mType == BluetoothDevice.PAIRING_VARIANT_PIN) {
                    Log.d(TAG, "接收到蓝牙配对请求，设备名" + device.getName() + ", 配对方法为PIN配对");
                }
                if (BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION == mType) {
                    if (pairRequestDialog == null || !pairRequestDialog.isShowing()) {
                        pairRequestDialog = buildDialog(device);
                        pairRequestDialog.show();
                    }
                } else {
                    WatchToast.make(BluetoothPairActivity.this, getString(R.string.pairing_method_not_support), Toast.LENGTH_SHORT).show();
                }
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                int preState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);
                Log.d(TAG, "state = " + state + ", pre state = " + preState);
                if (state == BluetoothDevice.BOND_NONE && preState == BluetoothDevice.BOND_BONDING) {
                    if (pairRequestDialog != null && pairRequestDialog.isShowing()) {
                        pairRequestDialog.dismiss();
                    }
                    WatchToast.make(BluetoothPairActivity.this, getString(R.string.pairing_failed), Toast.LENGTH_SHORT).show();
                }
                if (state == BluetoothDevice.BOND_BONDED && preState == BluetoothDevice.BOND_BONDING) {
                    WatchToast.make(BluetoothPairActivity.this, getString(R.string.pairing_success), Toast.LENGTH_SHORT).show();
                    Intent launcherIntent = new Intent();
                    launcherIntent.setComponent(new ComponentName(Constant.CLOUDER_LAUNCHER_PKG, Constant.CLOUDER_LAUNCHER_ACTIVITY));
                    startActivity(launcherIntent);

                    /**
                     * 通知CMS服务启动
                     */
                    Intent cmsService = new Intent();
                    cmsService.setAction("com.hoperun.ble.peripheral.service");
                    cmsService.putExtra("bluetooth_bond_address", device.getAddress());
                    cmsService.putExtra("bluetooth_bond_name", device.getName());
                    cmsService.setPackage("com.hoperun.message");
                    startService(cmsService);

                    String oldAddress = Settings.System.getString(getContentResolver(), SettingsKey.DEVICE_PAIRED_BT_ADDRESS);
                    Log.i(TAG, "Old Paired Device = " + oldAddress);
                    Log.i(TAG, "New Paired Device = " + device.getAddress());
                    if (!device.getAddress().equals(oldAddress)) {
                        //通知Clouder-Installer卸载已经安装的程序
                        try {
                            Intent installerService = new Intent();
                            ComponentName componentName = new ComponentName(Constant.CLOUDER_INSTALLER_PKG, Constant.CLOUDER_INSTALLER_SERVICE);
                            installerService.setComponent(componentName);
                            installerService.putExtra("paired_device_change", true);
                            startService(installerService);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        //通知服务切换
                        try {
                            Intent notificationService = new Intent();
                            ComponentName component = new ComponentName("com.clouder.watch.mobile", "com.clouder.watch.mobile.notification2.NotificationListenerService");
                            notificationService.setComponent(component);
                            notificationService.putExtra("paired_device_change", true);
                            startService(notificationService);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                    Settings.System.putString(getContentResolver(), SettingsKey.DEVICE_PAIRED_BT_ADDRESS, device.getAddress());
                    Settings.System.putString(getContentResolver(), SettingsKey.DEVICE_PAIRED_BT_NAME, device.getName());

                }
            }
        }
    };


    public Bitmap encodeQRCode(String content) {

        QRCodeEncode encoder = new QRCodeEncode.Builder()
                .setBackgroundColor(0xFFFFFF) // 指定背景颜色，默认为白色
                .setCodeColor(0xFF000000) // 指定编码块颜色，默认为黑色
                .setOutputBitmapWidth(300) // 生成图片宽度
                .setOutputBitmapHeight(300) // 生成图片高度
                .build();

        final Bitmap bitmap = encoder.encode(content);
        return bitmap;
    }

    private WatchDialog buildDialog(final BluetoothDevice device) {
        WatchDialog dialog = new WatchDialog(this, getString(R.string.bluetooth_pair_request), device.getName() + getString(R.string.request_pair));
        dialog.setCallbackListener(new WatchDialog.ICallbackListener() {
            @Override
            public void onNegativeClick(WatchDialog dialog) {
                dialog.dismiss();
                try {
                    BluetoothHelper.cancelPairingUserInput(device);
                } catch (Exception e) {
                    Log.e(TAG, "exception occurs when invoke method cancelPairingUserInput", e);
                }
            }

            @Override
            public void onPositiveClick(WatchDialog dialog) {
                dialog.dismiss();
                device.setPairingConfirmation(true);
            }
        });
        return dialog;
    }

    private void openDiscoverable() {
        boolean b = helper.setScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 3600);
        if (b) {
            Log.d(TAG, "蓝牙打开，打开可发现模式成功");
        } else {
            Log.d(TAG, "蓝牙打开，打开可发现模式失败");
        }
    }
}
