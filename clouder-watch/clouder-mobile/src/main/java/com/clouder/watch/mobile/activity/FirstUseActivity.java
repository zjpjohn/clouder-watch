package com.clouder.watch.mobile.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.clouder.watch.mobile.BaseActivity;
import com.clouder.watch.mobile.ClouderApplication;
import com.clouder.watch.mobile.R;
import com.clouder.watch.mobile.utils.BluetoothUtils;
import com.clouder.watch.mobile.utils.StringUtils;
import com.clouder.watch.mobile.utils.ToastUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 如果设备第一次运行或者手机尚未绑定手表设备
 * 则会向用户展示此activity以提醒用户绑定手表设备
 * <p/>
 * Created by yang_shoulai on 7/24/2015.
 */
public class FirstUseActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "FirstUseActivity";

    private static final int SCAN_REQUEST_CODE = 1;

    private Button btnPair;

    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        setContentView(R.layout.activity_first_use);
        setActionBarTitle(R.string.app_title);
        hideActionBarBack();
        btnPair = (Button) findViewById(R.id.btn_start_pair);
        btnPair.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        startActivityForResult(new Intent(this, ScanActivity.class), SCAN_REQUEST_CODE);
    }

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
                        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
                        int boundState = device.getBondState();
                        if (boundState == BluetoothDevice.BOND_BONDED) {
                            Log.d(TAG, "bluetooth device already bond");
                            BluetoothUtils.removeBond(device);
                        }
                        Intent intent = new Intent(this, PairResultActivity.class);
                        intent.putExtra(ClouderApplication.BIND_BT_ADDRESS, address);
                        intent.putExtra(ClouderApplication.BIND_BT_NAME, name);
                        startActivity(intent);
                        finish();
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "parse json failed!", e);
                    ToastUtils.show(this, getString(R.string.scan_result_parse_failed));
                }
            }
        }
    }


}
