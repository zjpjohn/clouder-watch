package com.clouder.watch.mobile.activity;

import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import com.clouder.watch.mobile.BaseActivity;
import com.clouder.watch.mobile.R;
import com.clouder.watch.mobile.utils.FinderView;
import com.clouder.watch.mobile.utils.QRCodeScanSupport;
import com.clouder.watch.mobile.utils.ToastUtils;

/**
 * 手表设备二维码扫描功能
 * 提供扫描二维码和解码的功能
 * 用户在{@link com.clouder.watch.mobile.activity.FirstUseActivity}点击“开始配对按钮后”
 * 会跳转至该activity，并返回结果
 * Created by yang_shoulai on 7/24/2015.
 */
public class ScanActivity extends BaseActivity {

    private static final String TAG = "ScanActivity";
    public static final String KEY_SCAN_RESULT = "key_scan_result";

    private QRCodeScanSupport mQRCodeScanSupport;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_scan);
        //ImageView capturePreview = (ImageView) findViewById(R.id.decode_preview);
        final FinderView finderView = (FinderView) findViewById(R.id.capture_viewfinder_view);
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.capture_preview_view);
        try {
            Camera camera = Camera.open();
            camera.release();
            Thread.sleep(100);
            mQRCodeScanSupport = new QRCodeScanSupport(surfaceView, finderView);
            //mQRCodeScanSupport.setCapturePreview(capturePreview);
            mQRCodeScanSupport.setOnScanResultListener(new QRCodeScanSupport.OnScanResultListener() {
                @Override
                public void onScanResult(String notNullResult) {
                    Log.d(TAG, "scan for pairing, result : " + notNullResult);
                    Intent intent = new Intent();
                    intent.putExtra(KEY_SCAN_RESULT, notNullResult);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            });
        } catch (Exception e) {
            ToastUtils.show(this, "打开摄像头失败，请检查摄像头权限！");
            finish();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        try {
            mQRCodeScanSupport.onResume(this);
        } catch (Exception e) {
            ToastUtils.show(this, "扫描出错！");
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            mQRCodeScanSupport.onPause(this);
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
    }
}
