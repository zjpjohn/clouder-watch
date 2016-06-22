package com.clouder.watch.mobile.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.clouder.watch.mobile.BaseActivity;
import com.clouder.watch.mobile.ClouderApplication;
import com.clouder.watch.mobile.R;
import com.clouder.watch.mobile.utils.BluetoothUtils;
import com.clouder.watch.mobile.utils.StringUtils;
import com.clouder.watch.mobile.utils.ToastUtils;
import com.cms.android.wearable.service.common.Utils;
import com.cms.android.wearable.service.impl.BLECentralService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

/**
 * 手机助手绑定手表设备
 * 在此activity中实际操作绑定事件
 * 绑定过程以动画显示，并且在绑定成功后跳转至{@link com.clouder.watch.mobile.activity.MainActivity}
 * Created by yang_shoulai on 7/24/2015.
 */
public class PairResultActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "PairResultActivity";

    private static final int MESSAGE_BOND_SUCCESS = 1;

    private static final int MESSAGE_BOND_FAILED = 2;

    private static final int SCAN_REQUEST_CODE = 1;

    private TextView pairState;

    private Button btnRepair;

    private boolean pairing;

    private String address;

    private String name;

    private ImageView icon1;

    private ImageView icon2;

    private ImageView icon3;

    private int[] icons = new int[]{R.drawable.icon_pairing_circle_1, R.drawable.icon_pairing_circle_2, R.drawable.icon_pairing_circle_3};

    private BluetoothAdapter bluetoothAdapter;


    private BluetoothDevice device;

    private boolean bleConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        setContentView(R.layout.activity_pair_result);
        setActionBarTitle(R.string.app_title);
        hideActionBarBack();
        pairState = (TextView) findViewById(R.id.pair_state);
        btnRepair = (Button) findViewById(R.id.btn_repair);
        btnRepair.setOnClickListener(this);
        btnRepair.setVisibility(View.GONE);
        icon1 = (ImageView) findViewById(R.id.pairing_icon_1);
        icon2 = (ImageView) findViewById(R.id.pairing_icon_2);
        icon3 = (ImageView) findViewById(R.id.pairing_icon_3);

        address = getIntent().getStringExtra(ClouderApplication.BIND_BT_ADDRESS);
        name = getIntent().getStringExtra(ClouderApplication.BIND_BT_NAME);
        device = bluetoothAdapter.getRemoteDevice(address);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(receiver, filter);


        IntentFilter bleIntentFilter = new IntentFilter();
        filter.addAction("com.clouder.watch.BLECentralService.ACTION_BLE_STATE_CHANGE");
        registerReceiver(bleBroadCastReceiver, bleIntentFilter);


        startPairingAnimation();
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        } else {
            BluetoothUtils.createBond(device);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        unregisterReceiver(bleBroadCastReceiver);
    }

    @Override
    public void onClick(View v) {
        startActivityForResult(new Intent(this, ScanActivity.class), SCAN_REQUEST_CODE);
    }


    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            if (what == MESSAGE_BOND_SUCCESS) {
                /**
                 * 启动CMS服务
                 */
                Log.d(TAG, "配对成功，对方蓝牙名称: " + name + ", 蓝牙地址:" + address);
                Log.d(TAG, "对方蓝牙信息写入SharedPreference");
                application.setBindBtName(name);
                application.setBindBtAddress(address);
                application.setDeviceFirstStart(true);

                /*Intent intent = new Intent(PairResultActivity.this, BLECentralService.class);
                intent.putExtra("bluetooth_bond_address", address);
                intent.putExtra("bluetooth_bond_name", name);
                startService(intent);*/
                Log.d(TAG, "启动相应的服务:[CMS服务],[同步服务],[联系人同步服务],[电话监听服务],[通知同步服务]");
                ClouderApplication.startServices(PairResultActivity.this, address, name);
                ClouderApplication.getInstance().registerTimeTickReceiver();
                ClouderApplication.getInstance().addPairedBluetooth(new ClouderApplication.PairedBluetooth(name, address));
                Utils.saveSharedBondToggle(PairResultActivity.this, true);
                pairState.setText("配对成功,正在连接...");
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!bleConnected) {
                            stopPairingAnimation();
                            pairState.setText("连接成功");
                            btnRepair.setVisibility(View.GONE);
                            Intent main = new Intent(PairResultActivity.this, MainActivity.class);
                            main.putExtra("FIRST_START", true);
                            startActivity(main);
                            finish();
                        }
                    }
                }, 5000);
            } else if (what == MESSAGE_BOND_FAILED) {
                stopPairingAnimation();
                pairState.setText("配对失败，请重新扫描配对");
                btnRepair.setVisibility(View.VISIBLE);
            }
        }
    };


    /**
     * 开始配对动画
     */
    private void startPairingAnimation() {
        Log.d(TAG, "Start pairing animation!");
        pairing = true;
        pairState.setText("正在配对...");
        btnRepair.setVisibility(View.GONE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                while (pairing) {
                    final int time = i;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            icon1.setImageResource(icons[time % 3]);
                            icon2.setImageResource(icons[(time + 1) % 3]);
                            icon3.setImageResource(icons[(time + 2) % 3]);
                        }
                    });
                    try {
                        Thread.sleep(100);
                        i++;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }


    /**
     * 停止配对动画
     */
    private void stopPairingAnimation() {
        pairing = false;
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                //蓝牙状态改变
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                if (BluetoothAdapter.STATE_TURNING_ON == state) {
                    pairState.setText("正在打开蓝牙");
                } else if (BluetoothAdapter.STATE_ON == state) {
                    pairState.setText("正在配对");
                    BluetoothUtils.createBond(device);
                }
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                //蓝牙绑定状态改变
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                int preState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);
                if (address.equals(device.getAddress())) {
                    if (state == BluetoothDevice.BOND_BONDED && preState == BluetoothDevice.BOND_BONDING) {
                        handler.sendEmptyMessageDelayed(MESSAGE_BOND_SUCCESS, 1000);
                    } else if (state == BluetoothDevice.BOND_NONE && preState == BluetoothDevice.BOND_BONDING) {
                        handler.sendEmptyMessage(MESSAGE_BOND_FAILED);
                    }
                }
            }
        }
    };


    private BroadcastReceiver bleBroadCastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //BluetoothDevice device = intent.getParcelableExtra("com.clouder.watch.BLECentralService.EXTRA_DEVICE");
            int state = intent.getIntExtra("com.clouder.watch.BLECentralService.EXTRA_STATE",-1);
            if (state == BluetoothProfile.STATE_CONNECTED) {
                bleConnected = true;
                stopPairingAnimation();
                pairState.setText("连接成功");
                btnRepair.setVisibility(View.GONE);
                Intent main = new Intent(PairResultActivity.this, MainActivity.class);
                main.putExtra("FIRST_START", true);
                startActivity(main);
                finish();
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SCAN_REQUEST_CODE && resultCode == RESULT_OK) {
            String result = data.getStringExtra(ScanActivity.KEY_SCAN_RESULT);
            Log.d(TAG, "Scan result : " + result);
            if (StringUtils.isEmpty(result)) {
                Log.e(TAG, "Scan result is empty!");
                ToastUtils.show(this, getString(R.string.scan_result_parse_failed));
            } else {
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    String address = jsonObject.getString("address");
                    String name = jsonObject.getString("name");
                    if (StringUtils.isEmpty(address) || StringUtils.isEmpty(name) || !StringUtils.isValidBluetoothAddress(address)) {
                        ToastUtils.show(this, getString(R.string.scan_result_parse_failed));
                    } else {
                        device = bluetoothAdapter.getRemoteDevice(address);
                        int boundState = device.getBondState();
                        if (boundState == BluetoothDevice.BOND_BONDED) {
                            Log.d(TAG, "bluetooth device already bond");
                            BluetoothUtils.removeBond(device);
                        }
                        startPairingAnimation();
                        BluetoothUtils.createBond(device);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "parse json failed!", e);
                    ToastUtils.show(this, getString(R.string.scan_result_parse_failed));
                }
            }
        }
    }
}
