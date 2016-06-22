package com.clouder.watch.guide;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.clouder.watch.common.ui.SwipeRightActivity;
import com.clouder.watch.common.utils.BluetoothHelper;
import com.github.yoojia.zxing.QRCodeEncode;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by yang_shoulai on 7/22/2015.
 */
public class GuidePairCodeActivity extends SwipeRightActivity {

    private static final String TAG = "GuidePairCodeActivity";

    public static final String PAIR_REQUEST_DEVICE = "pair_request_device";

    private ImageView imageView;

    private BluetoothHelper bluetoothHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCache.add(this);
        setContentView(R.layout.guide_pair_code);
        initView();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        registerReceiver(receiver, filter);
    }

    private void initView() {
        imageView = (ImageView) findViewById(R.id.image);
        bluetoothHelper = new BluetoothHelper();
        if (bluetoothHelper.isSupportBT()) {
            //bluetoothHelper.removeAllBondedDevices();
            String address = bluetoothHelper.getBlueToothAddress();
            String name = bluetoothHelper.getBlueToothName();
            JSONObject object = new JSONObject();
            try {
                object.put("address", address);
                object.put("name", name);
                Log.d(TAG, "Get device bluetooth address " + address);
            } catch (JSONException e) {
                Log.e(TAG, "JSONException", e);
            }
            imageView.setImageBitmap(encodeQRCode(object.toString()));

            if (!bluetoothHelper.isOn()) {
                bluetoothHelper.turnOn();
            }
        } else {
            Log.d(TAG, "Device does not support bluetooth.");
            //generate view for test
            imageView.setImageBitmap(encodeQRCode("{\"address\":\"00:43:A8:23:10:F0\",\"name\":\"Bluetooth Not Support\"}"));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

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


    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                int mType = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR);
                if (BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION == mType) {
                    final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Intent i = new Intent(GuidePairCodeActivity.this, GuidePairingActivity.class);
                    i.putExtra(PAIR_REQUEST_DEVICE, device);
                    startActivity(i);
                    overridePendingTransition(com.clouder.watch.common.R.anim.base_slide_right_in, com.clouder.watch.common.R.anim.base_slide_left_out);
                }
            }
        }
    };
}
