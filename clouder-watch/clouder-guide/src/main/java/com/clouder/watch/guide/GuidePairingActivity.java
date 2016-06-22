package com.clouder.watch.guide;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.clouder.watch.common.ui.SwipeRightActivity;
import com.clouder.watch.common.utils.BluetoothHelper;
import com.clouder.watch.common.utils.SettingsKey;
import com.clouder.watch.common.widget.WatchDialog;

/**
 * Created by yang_shoulai on 7/22/2015.
 */
public class GuidePairingActivity extends SwipeRightActivity {

    private static final String TAG = "GuidePairingActivity";

    private boolean pairing = true;

    private int[] indicators = new int[]{R.id.icon_pairing_circle_1, R.id.icon_pairing_circle_2, R.id.icon_pairing_circle_3};

    private int[] drawables = new int[]{R.drawable.icon_pairing_circle_1, R.drawable.icon_pairing_circle_2, R.drawable.icon_pairing_circle_3};

    private Handler mHandler = new Handler();

    private TextView stateTextView;

    private BluetoothDevice bluetoothDevice;

    private WatchDialog dialog;


    private ImageView iconWatch;

    private boolean bleConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guide_pairing);
        initView();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(receiver, filter);

        IntentFilter bleIntentFilter = new IntentFilter();
        filter.addAction("com.clouder.watch.BLEPeripheralService.ACTION_BLE_STATE_CHANGE");
        registerReceiver(bleBroadCastReceiver, bleIntentFilter);
    }

    private void initView() {
        iconWatch = (ImageView) findViewById(R.id.icon_pairing_watch);
        stateTextView = (TextView) findViewById(R.id.pair_state);
        startPairingDrawable();
        bluetoothDevice = getIntent().getParcelableExtra(GuidePairCodeActivity.PAIR_REQUEST_DEVICE);
        if (bluetoothDevice == null) {
            //add onclickListener for test
            iconWatch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stopPairingDrawable();
                    stateTextView.setText(R.string.device_pair_success);
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startActivity(new Intent(GuidePairingActivity.this, GuideScreenLockerActivity.class));
                            overridePendingTransition(com.clouder.watch.common.R.anim.base_slide_right_in, com.clouder.watch.common.R.anim.base_slide_left_out);
                            finish();
                        }
                    }, 500);
                }
            });
        } else {
            dialog = buildDialog();
            dialog.show();

        }
    }

    /**
     * 处理蓝牙配对请求
     */
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
                int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                int preState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getAddress().equals(bluetoothDevice.getAddress())) {
                    stopPairingDrawable();
                    if (state == BluetoothDevice.BOND_BONDED && preState == BluetoothDevice.BOND_BONDING) {
                        stateTextView.setText(R.string.device_pair_success);
                        Settings.System.putString(getContentResolver(), SettingsKey.DEVICE_PAIRED_BT_ADDRESS, bluetoothDevice.getAddress());
                        Settings.System.putString(getContentResolver(), SettingsKey.DEVICE_PAIRED_BT_NAME, bluetoothDevice.getName());

                        /**
                         * 启动CMS服务
                         */
                        Intent cmsService = new Intent();
                        cmsService.setAction("com.hoperun.ble.peripheral.service");
                        cmsService.putExtra("bluetooth_bond_address", bluetoothDevice.getAddress());
                        cmsService.putExtra("bluetooth_bond_name", bluetoothDevice.getName());
                        cmsService.setPackage("com.hoperun.message");
                        startService(cmsService);
                        stateTextView.setText(R.string.linking);
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!bleConnected) {
                                    startActivity(new Intent(GuidePairingActivity.this, GuideScreenLockerActivity.class));
                                    overridePendingTransition(com.clouder.watch.common.R.anim.base_slide_right_in, com.clouder.watch.common.R.anim.base_slide_left_out);
                                    finish();
                                }

                            }
                        }, 5000);

                    } else if (state == BluetoothDevice.BOND_NONE && preState == BluetoothDevice.BOND_BONDING) {
                        if (dialog != null && dialog.isShowing()) {
                            dialog.dismiss();
                        }
                        stateTextView.setText(R.string.device_pair_failed);
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                startActivity(new Intent(GuidePairingActivity.this, GuidePairCodeActivity.class));
                                overridePendingTransition(com.clouder.watch.common.R.anim.base_slide_left_in, com.clouder.watch.common.R.anim.base_slide_right_out);
                                finish();
                            }
                        }, 500);

                    }
                }
            }
        }
    };


    private BroadcastReceiver bleBroadCastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
           // BluetoothDevice device = intent.getParcelableExtra("com.clouder.watch.BLEPeripheralService.EXTRA_DEVICE");
            int state = intent.getIntExtra("com.clouder.watch.BLEPeripheralService.EXTRA_STATE",-1);
            if (state == BluetoothProfile.STATE_CONNECTED) {
                bleConnected = true;
                startActivity(new Intent(GuidePairingActivity.this, GuideScreenLockerActivity.class));
                overridePendingTransition(com.clouder.watch.common.R.anim.base_slide_right_in, com.clouder.watch.common.R.anim.base_slide_left_out);
                finish();
            }
        }
    };

    /**
     * 创建蓝牙配对请求确认框
     */
    private WatchDialog buildDialog() {
        dialog = new WatchDialog(GuidePairingActivity.this, getString(R.string.bluetooth_pair_request), bluetoothDevice.getName() + getString(R.string.request_pair));
        dialog.setCallbackListener(new WatchDialog.ICallbackListener() {
            @Override
            public void onNegativeClick(WatchDialog dialog) {
                dialog.dismiss();
                pairing = false;
                try {
                    BluetoothHelper.cancelPairingUserInput(bluetoothDevice);
                } catch (Exception e) {
                    Log.e(TAG, "exception occurs when invoke method cancelPairingUserInput", e);
                }

            }

            @Override
            public void onPositiveClick(WatchDialog dialog) {
                dialog.dismiss();
                bluetoothDevice.setPairingConfirmation(true);
            }
        });

        return dialog;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        unregisterReceiver(bleBroadCastReceiver);
    }

    /**
     * 显示pairing图片动画
     */
    private void startPairingDrawable() {
        pairing = true;
        final ImageView first = (ImageView) findViewById(indicators[0]);
        final ImageView second = (ImageView) findViewById(indicators[1]);
        final ImageView third = (ImageView) findViewById(indicators[2]);
        new Thread(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                while (pairing) {
                    final int temp = i;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            first.setImageResource(drawables[temp % 3]);
                            second.setImageResource(drawables[(temp + 1) % 3]);
                            third.setImageResource(drawables[(temp + 2) % 3]);
                        }
                    });

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "InterruptedException", e);
                    }
                    i++;
                }
            }
        }).start();
    }

    /**
     * 停止动画
     */
    private void stopPairingDrawable() {
        pairing = false;
    }

}
