package com.clouder.watch.mobile.utils;
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.github.yoojia.zxing.camera.AutoFocusListener;
import com.github.yoojia.zxing.camera.OpenCameraInterface;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.client.android.camera.CameraConfigurationUtils;

import java.io.IOException;


public final class CameraManager {
    private static final String TAG = CameraManager.class.getSimpleName();
    private static final int MIN_FRAME_WIDTH = 240;
    private static final int MIN_FRAME_HEIGHT = 240;
    private static final int MAX_FRAME_WIDTH = 1200;
    private static final int MAX_FRAME_HEIGHT = 675;
    private final Context context;
    private Camera camera;
    private AutoFocusManager autoFocusManager;
    private Rect framingRect;
    private Rect framingRectInPreview;
    private boolean initialized;
    private boolean previewing;
    private int requestedFramingRectWidth;
    private int requestedFramingRectHeight;
    private Point screenResolution;
    private Point cameraResolution;

    public CameraManager(Context context) {
        this.context = context;
    }

    public void requestPreview(Camera.PreviewCallback callback) {
        this.camera.setOneShotPreviewCallback(callback);
    }

    public synchronized void openDriver(SurfaceHolder holder) throws IOException {
        Camera theCamera = this.camera;
        if (theCamera == null) {
            theCamera = OpenCameraInterface.open(-1);
            if (theCamera == null) {
                throw new IOException();
            }

            this.camera = theCamera;
        }

        theCamera.setPreviewDisplay(holder);
        theCamera.setDisplayOrientation(90);
        if (!this.initialized) {
            this.initialized = true;
            this.initFromCameraParameters(theCamera);
            if (this.requestedFramingRectWidth > 0 && this.requestedFramingRectHeight > 0) {
                this.setManualFramingRect(this.requestedFramingRectWidth, this.requestedFramingRectHeight);
                this.requestedFramingRectWidth = 0;
                this.requestedFramingRectHeight = 0;
            }
        }

        Camera.Parameters parameters = theCamera.getParameters();
        String parametersFlattened = parameters.flatten();

        try {
            this.setDesiredCameraParameters(theCamera, false);
        } catch (RuntimeException var8) {
            Log.e(TAG, "Camera rejected parameters. Setting only minimal safe-mode parameters");
            Log.e(TAG, "Resetting to saved camera params: " + parametersFlattened);
            parameters = theCamera.getParameters();
            parameters.unflatten(parametersFlattened);

            try {
                theCamera.setParameters(parameters);
                this.setDesiredCameraParameters(theCamera, true);
            } catch (RuntimeException var7) {
                Log.e(TAG, "> Camera rejected even safe-mode parameters! No configuration");
            }
        }

    }

    public synchronized boolean isOpen() {
        return this.camera != null;
    }

    public synchronized void closeDriver() {
        if (this.camera != null) {
            this.camera.release();
            this.camera = null;
            this.framingRect = null;
            this.framingRectInPreview = null;
        }

    }

    public synchronized void startPreview(AutoFocusListener autoFocusListener) {
        Camera theCamera = this.camera;
        if (theCamera != null && !this.previewing) {
            theCamera.startPreview();
            this.previewing = true;
            this.autoFocusManager = new AutoFocusManager(this.camera);
            this.autoFocusManager.setAutoFocusListener(autoFocusListener);
        }

    }

    public synchronized void stopPreview() {
        if (this.autoFocusManager != null) {
            this.autoFocusManager.stop();
            this.autoFocusManager = null;
        }

        if (this.camera != null && this.previewing) {
            this.camera.stopPreview();
            this.previewing = false;
        }

    }

    public synchronized Rect getFramingRect() {
        if (this.framingRect == null) {
            if (this.camera == null) {
                return null;
            }

            if (this.screenResolution == null) {
                return null;
            }

            int width = findDesiredDimensionInRange(this.screenResolution.x, 240, 1200);
            int height = findDesiredDimensionInRange(this.screenResolution.y, 240, 675);

            int min = Math.min(width, height);
            width = min;
            height = min;
            int leftOffset = (this.screenResolution.x - width) / 2;
            int topOffset = (this.screenResolution.y - height) / 2 - 100;
            this.framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
            Log.d(TAG, "Calculated framing rect: " + this.framingRect);
        }

        return this.framingRect;
    }

    public synchronized Rect getFramingRectInPreview() {
        if (this.framingRectInPreview == null) {
            Rect framingRect = this.getFramingRect();
            if (framingRect == null) {
                return null;
            }

            Rect rect = new Rect(framingRect);
            if (this.cameraResolution == null || this.screenResolution == null) {
                return null;
            }

            rect.left = rect.left * this.cameraResolution.x / this.screenResolution.x;
            rect.right = rect.right * this.cameraResolution.x / this.screenResolution.x;
            rect.top = rect.top * this.cameraResolution.y / this.screenResolution.y;
            rect.bottom = rect.bottom * this.cameraResolution.y / this.screenResolution.y;
            this.framingRectInPreview = rect;
        }

        return this.framingRectInPreview;
    }

    public synchronized void setManualFramingRect(int width, int height) {
        if (this.initialized) {
            if (width > this.screenResolution.x) {
                width = this.screenResolution.x;
            }

            if (height > this.screenResolution.y) {
                height = this.screenResolution.y;
            }

            int leftOffset = (this.screenResolution.x - width) / 2;
            int topOffset = (this.screenResolution.y - height) / 2;
            this.framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
            Log.d(TAG, "Calculated manual framing rect: " + this.framingRect);
            this.framingRectInPreview = null;
        } else {
            this.requestedFramingRectWidth = width;
            this.requestedFramingRectHeight = height;
        }

    }

    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        Rect rect = this.getFramingRectInPreview();
        return rect == null ? null : new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top, rect.width(), rect.height(), false);
    }

    private void initFromCameraParameters(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        WindowManager manager = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point theScreenResolution = new Point();
        display.getSize(theScreenResolution);
        this.screenResolution = theScreenResolution;
        Log.i(TAG, "Screen resolution: " + this.screenResolution);
        this.cameraResolution = CameraConfigurationUtils.findBestPreviewSizeValue(parameters, this.screenResolution);
        Log.i(TAG, "Camera resolution: " + this.cameraResolution);
    }

    private void setDesiredCameraParameters(Camera camera, boolean safeMode) {
        Camera.Parameters parameters = camera.getParameters();
        CameraConfigurationUtils.setFocus(parameters, true, true, safeMode);
        parameters.setPreviewSize(this.cameraResolution.x, this.cameraResolution.y);
        camera.setParameters(parameters);
        Camera.Parameters afterParameters = camera.getParameters();
        Camera.Size afterSize = afterParameters.getPreviewSize();
        if (afterSize != null && (this.cameraResolution.x != afterSize.width || this.cameraResolution.y != afterSize.height)) {
            Log.w(TAG, "Camera said it supported preview size " + this.cameraResolution.x + 'x' + this.cameraResolution.y + ", but after setting it, preview size is " + afterSize.width + 'x' + afterSize.height);
            this.cameraResolution.x = afterSize.width;
            this.cameraResolution.y = afterSize.height;
        }

    }

    private static int findDesiredDimensionInRange(int resolution, int hardMin, int hardMax) {
        int dim = 5 * resolution / 8;
        return dim < hardMin ? hardMin : (dim > hardMax ? hardMax : dim);
    }
}
