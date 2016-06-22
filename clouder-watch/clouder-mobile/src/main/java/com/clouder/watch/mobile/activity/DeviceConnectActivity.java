package com.clouder.watch.mobile.activity;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.clouder.watch.common.sync.SyncMessage;
import com.clouder.watch.common.sync.SyncMessagePathConfig;
import com.clouder.watch.mobile.ClouderApplication;
import com.clouder.watch.mobile.R;
import com.clouder.watch.mobile.SyncActivity;
import com.clouder.watch.mobile.utils.BluetoothUtils;
import com.clouder.watch.mobile.utils.StringUtils;
import com.clouder.watch.mobile.utils.ToastUtils;
import com.cms.android.wearable.service.common.Utils;
import com.cms.android.wearable.service.impl.BLECentralService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

/**
 * Created by yang_shoulai on 7/27/2015.
 */
public class DeviceConnectActivity extends SyncActivity implements View.OnClickListener {

    private static final String TAG = "DeviceConnectActivity";

    private View switchPush;

    private ImageButton btnMore;

    private TextView bindDeviceName;

    private TextView bindDeviceState;

    private BluetoothAdapter bluetoothAdapter;

    private BluetoothDevice device;

    private ProgressDialog progressDialog;

    private Handler mainHandler = new Handler();

    private boolean deviceConnect = false;


    private ProgressDialog connectDialog;

    private boolean optionMenuClickable = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        setContentView(R.layout.activity_device_connect);
        setActionBarTitle(R.string.connect_center_title);
        switchPush = findViewById(R.id.connected_device);
        btnMore = (ImageButton) findViewById(R.id.add_more_device);
        bindDeviceName = (TextView) findViewById(R.id.paired_device_name);
        bindDeviceState = (TextView) findViewById(R.id.paired_device_state);
        switchPush.setOnClickListener(this);
        btnMore.setOnClickListener(this);
        //listView.setAdapter(adapter);
        bindDeviceName.setText(ClouderApplication.getInstance().getBindBtName());

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(receiver, filter);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.switch_push) {

        } else if (id == R.id.add_more_device) {
            startActivityForResult(new Intent(this, ScanActivity.class), 1);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
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
                        if (device == null) {
                            ToastUtils.show(this, "设备不存在");
                            return;
                        }


                        int boundState = device.getBondState();
                        Log.d(TAG, "boundState = " + boundState);
                        Log.d(TAG, "boundState = " + (boundState == BluetoothDevice.BOND_BONDED));
                       /* if (ClouderApplication.getInstance().getBindBtAddress().equals(device.getAddress())
                                && boundState == BluetoothDevice.BOND_BONDED) {
                            if (bindDeviceState.getText().toString().equals("设备未连接")) {
                                ToastUtils.show(this, "设备已经存在，正在尝试连接，请稍等");
                            } else {
                                ToastUtils.show(this, "设备已经存在");
                            }
                            return;
                        }*/
                        if (boundState == BluetoothDevice.BOND_BONDED) {
                            Log.d(TAG, "bluetooth device already bond");
                            BluetoothUtils.removeBond(device);
                        }
                        if (bluetoothAdapter.isEnabled()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (progressDialog == null) {
                                        progressDialog = new ProgressDialog(DeviceConnectActivity.this);
                                        progressDialog.setCancelable(true);
                                        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                                        progressDialog.setCanceledOnTouchOutside(true);
                                        progressDialog.setMessage("Pairing...");
                                    }
                                    progressDialog.show();
                                }
                            });
                            Log.d(TAG, "bluetooth device start bond!");
                            BluetoothUtils.createBond(device);
                        } else {
                            ToastUtils.show(this, "蓝牙未打开");
                        }

                    }
                } catch (JSONException e) {
                    Log.e(TAG, "parse json failed!", e);
                    ToastUtils.show(this, getString(R.string.scan_result_parse_failed));
                }
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                //蓝牙绑定状态改变
                BluetoothDevice d = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                int preState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);
                Log.d(TAG, "State = " + state + ", Pre state = " + preState);
                if (device == null || d == null) {
                    ToastUtils.show(DeviceConnectActivity.this, "配对出错，设备不存在！");
                    return;
                }
                if (device.getAddress() == null || d.getAddress() == null) {
                    ToastUtils.show(DeviceConnectActivity.this, "配对出错，设备地址为空！");
                    return;
                }
                if (device.getAddress().equals(d.getAddress())) {
                    if (state == BluetoothDevice.BOND_BONDED && preState == BluetoothDevice.BOND_BONDING) {
                        mainHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (progressDialog != null && progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }
                                Utils.setShardBondDisconnect(DeviceConnectActivity.this, false);
                                Intent intent = new Intent(DeviceConnectActivity.this, BLECentralService.class);
                                intent.putExtra("bluetooth_bond_address", device.getAddress());
                                intent.putExtra("bluetooth_bond_name", device.getName());
                                startService(intent);
                                if (!device.getAddress().equals(ClouderApplication.getInstance().getBindBtAddress())) {
                                    application.setDeviceFirstStart(true);
                                }
                                ClouderApplication.getInstance().setBindBtAddress(device.getAddress());
                                ClouderApplication.getInstance().setBindBtName(device.getName());
                                bindDeviceName.setText(device.getName());
                                device = null;
                            }
                        }, 1000);
                    } else if (state == BluetoothDevice.BOND_NONE && preState == BluetoothDevice.BOND_BONDING) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (progressDialog != null && progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }
                                ToastUtils.show(DeviceConnectActivity.this, "配对失败！");
                            }
                        });

                    }
                }
            }
        }
    };


    @Override
    public void onConnectSyncServiceSuccess() {
        super.onConnectSyncServiceSuccess();
        bindDeviceState.setText("设备未连接");
        //sendMessage(new SocketConnectSyncMessage(Constant.CLOUDER_SETTINGS_PKG, SyncMessagePathConfig.SOCKET_CONNECTED));
        checkNodesConnected(this);
    }

    @Override
    public void onConnectSyncServiceFailed() {
        super.onConnectSyncServiceFailed();
        bindDeviceState.setText("设备未连接");
        //ToastUtils.show(this, "设备未连接");
        ToastUtils.show(this, "设备断开连接");
        deviceConnect = false;
    }


    @Override
    public void onHasNodesConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (connectDialog != null && connectDialog.isShowing()) {
                    connectDialog.setTitle("设备已连接");
                    connectDialog.dismiss();
                }
                bindDeviceState.setText("设备已连接");
                //ToastUtils.show(DeviceConnectActivity.this, "设备已连接");
                deviceConnect = true;
                optionMenuClickable = true;
            }
        });

    }

    @Override
    public void onNoNodeConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bindDeviceState.setText("设备未连接");
                optionMenuClickable = true;
            }
        });

    }

    @Override
    public List<String> messageListenerPaths() {
        return Arrays.asList(SyncMessagePathConfig.SOCKET_CONNECTED);
    }

    @Override
    public void onMessageReceived(String path, SyncMessage message) {
        super.onMessageReceived(path, message);
        Log.d(TAG, "onMessageReceived path = " + path + "");
        if (path.equals(SyncMessagePathConfig.SOCKET_CONNECTED)) {
            bindDeviceState.setText("设备已连接");
        }
    }

    @Override
    public void onSendFailed(final SyncMessage syncMessage, int errorCode) {
        Log.d(TAG, "send failed, message = " + syncMessage.toString());
        super.onSendFailed(syncMessage, errorCode);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (syncMessage.getPath().equals(SyncMessagePathConfig.SOCKET_CONNECTED)) {
                    bindDeviceState.setText("设备未连接");
                }
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.device_connection, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.start:
                if (Utils.getShardBondDisconnect(DeviceConnectActivity.this)) {
                    ToastUtils.show(DeviceConnectActivity.this, "设备连接失效，请重新扫描配对！");
                    break;
                }

                if (connectDialog == null) {
                    connectDialog = new ProgressDialog(this);
                    connectDialog.setCancelable(false);
                    connectDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    connectDialog.setCanceledOnTouchOutside(false);
                    connectDialog.setMessage("设备正在连接...");
                }
                connectDialog.show();
                optionMenuClickable = false;
                Intent startIntent = new Intent(DeviceConnectActivity.this, BLECentralService.class);
                startIntent.putExtra("bluetooth_bond_toggle", true);
                startIntent.putExtra("bluetooth_bond_address", ClouderApplication.getInstance().getBindBtAddress());
                startIntent.putExtra("bluetooth_bond_name", ClouderApplication.getInstance().getBindBtName());
                startService(startIntent);
                bluetoothAdapter.disable();
                //ToastUtils.show(this, "设备正在连接...");
                mainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!deviceConnect) {
                            optionMenuClickable = true;
                            ToastUtils.show(DeviceConnectActivity.this, "设备连接失败");
                            if (connectDialog != null && connectDialog.isShowing()) {
                                connectDialog.dismiss();
                            }
                        }
                    }
                }, 30000);
                break;
            case R.id.stop:
                if (Utils.getShardBondDisconnect(DeviceConnectActivity.this)) {
                    ToastUtils.show(DeviceConnectActivity.this, "设备连接失效，请重新扫描配对！");
                    break;
                }
                Intent stopIntent = new Intent(DeviceConnectActivity.this, BLECentralService.class);
                stopIntent.putExtra("bluetooth_bond_toggle", false);
                stopIntent.putExtra("bluetooth_bond_address", ClouderApplication.getInstance().getBindBtAddress());
                stopIntent.putExtra("bluetooth_bond_name", ClouderApplication.getInstance().getBindBtName());
                startService(stopIntent);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem startItem = menu.findItem(R.id.start);
        MenuItem stopItem = menu.findItem(R.id.stop);
        if (bindDeviceState.getText().toString().equals("设备未连接")) {
            stopItem.setVisible(false);
            startItem.setVisible(true);
        } else {
            stopItem.setVisible(true);
            startItem.setVisible(false);
        }
        startItem.setEnabled(optionMenuClickable);
        return super.onPrepareOptionsMenu(menu);
    }
}
