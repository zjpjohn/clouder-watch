package com.clouder.watch.mobile.utils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

import com.github.yoojia.zxing.QRCodeDecode;
import com.github.yoojia.zxing.QRCodeDecodeTask;
import com.github.yoojia.zxing.camera.AutoFocusListener;
import com.github.yoojia.zxing.camera.CameraSurfaceCallback;

import java.io.IOException;

/**
 * Created by yang_shoulai on 11/18/2015.
 */
public class QRCodeScanSupport {
    public static final String TAG = QRCodeScanSupport.class.getSimpleName();
    private final CameraManager mCameraManager;
    private final SurfaceView mSurfaceView;
    private final QRCodeDecode mQRCodeDecode;
    private ImageView mCapturePreview;
    private QRCodeScanSupport.OnScanResultListener mOnScanResultListener;
    private final CameraSurfaceCallback mCallback;
    private final Camera.PreviewCallback mPreviewCallback;
    private final AutoFocusListener mAutoFocusListener;

    public void setOnScanResultListener(QRCodeScanSupport.OnScanResultListener onScanResultListener) {
        this.mOnScanResultListener = onScanResultListener;
    }

    public void setCapturePreview(ImageView capturePreview) {
        this.mCapturePreview = capturePreview;
    }

    public QRCodeScanSupport(SurfaceView surfaceView, FinderView finderView) {
        this(surfaceView, finderView, (QRCodeScanSupport.OnScanResultListener)null);
    }

    public QRCodeScanSupport(SurfaceView surfaceView, FinderView finderView, QRCodeScanSupport.OnScanResultListener listener) {
        this.mQRCodeDecode = (new QRCodeDecode.Builder()).build();
        this.mCapturePreview = null;
        this.mCallback = new CameraSurfaceCallback() {
            public void surfaceCreated(SurfaceHolder holder) {
                QRCodeScanSupport.this.initCamera(holder);
            }
        };
        this.mPreviewCallback = new Camera.PreviewCallback() {
            private QRCodeScanSupport.PreviewQRCodeDecodeTask mDecodeTask;

            public void onPreviewFrame(byte[] data, Camera camera) {
                if(this.mDecodeTask != null) {
                    this.mDecodeTask.cancel(true);
                }

                this.mDecodeTask = QRCodeScanSupport.this.new PreviewQRCodeDecodeTask(QRCodeScanSupport.this.mQRCodeDecode);
                QRCodeDecodeTask.CameraPreview preview = new QRCodeDecodeTask.CameraPreview(data, camera);
                this.mDecodeTask.execute(new QRCodeDecodeTask.CameraPreview[]{preview});
            }
        };
        this.mAutoFocusListener = new AutoFocusListener() {
            public void onFocus(boolean focusSuccess) {
                if(focusSuccess) {
                    QRCodeScanSupport.this.mCameraManager.requestPreview(QRCodeScanSupport.this.mPreviewCallback);
                }

            }
        };
        this.mCameraManager = new CameraManager(surfaceView.getContext().getApplicationContext());
        finderView.setCameraManager(this.mCameraManager);
        this.mSurfaceView = surfaceView;
        this.mOnScanResultListener = listener;
    }

    public void onResume(Activity activity) {
        SurfaceHolder surfaceHolder = this.mSurfaceView.getHolder();
        surfaceHolder.addCallback(this.mCallback);
    }

    public void onPause(Activity activity) {
        SurfaceHolder surfaceHolder = this.mSurfaceView.getHolder();
        surfaceHolder.removeCallback(this.mCallback);
        this.mCameraManager.stopPreview();
        this.mCameraManager.closeDriver();
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if(!this.mCameraManager.isOpen()) {
            try {
                this.mCameraManager.openDriver(surfaceHolder);
                this.mCameraManager.requestPreview(this.mPreviewCallback);
                this.mCameraManager.startPreview(this.mAutoFocusListener);
            } catch (IOException var3) {
                Log.w(TAG, var3);
            }

        }
    }

    public interface OnScanResultListener {
        void onScanResult(String var1);
    }

    private class PreviewQRCodeDecodeTask extends QRCodeDecodeTask {
        public PreviewQRCodeDecodeTask(QRCodeDecode qrCodeDecode) {
            super(qrCodeDecode);
        }

        protected void onPostDecoded(String result) {
            if(QRCodeScanSupport.this.mOnScanResultListener == null) {
                Log.w(QRCodeScanSupport.TAG, "WARNING ! QRCode result ignored !");
            } else {
                QRCodeScanSupport.this.mOnScanResultListener.onScanResult(result);
            }

        }

        protected void onDecodeProgress(Bitmap capture) {
            if(QRCodeScanSupport.this.mCapturePreview != null) {
                QRCodeScanSupport.this.mCapturePreview.setImageBitmap(capture);
            }

        }
    }
}
