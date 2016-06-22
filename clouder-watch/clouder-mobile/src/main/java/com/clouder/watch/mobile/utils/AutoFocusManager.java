//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//


package com.clouder.watch.mobile.utils;

import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.util.Log;

import com.github.yoojia.zxing.camera.AutoFocusListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.RejectedExecutionException;

final class AutoFocusManager implements Camera.AutoFocusCallback {
    private static final String TAG = AutoFocusManager.class.getSimpleName();
    private static final long AUTO_FOCUS_INTERVAL_MS = 1200L;
    private static final Collection<String> FOCUS_MODES_CALLING_AF = new ArrayList(2);
    private boolean stopped;
    private boolean focusing;
    private final boolean useAutoFocus;
    private final Camera camera;
    private AsyncTask<?, ?, ?> outstandingTask;
    private AutoFocusListener mAutoFocusListener;

    AutoFocusManager(Camera camera) {
        this.camera = camera;
        String currentFocusMode = camera.getParameters().getFocusMode();
        this.useAutoFocus = FOCUS_MODES_CALLING_AF.contains(currentFocusMode);
        this.start();
    }

    public synchronized void onAutoFocus(boolean success, Camera theCamera) {
        this.mAutoFocusListener.onFocus(success);
        this.focusing = false;
        this.autoFocusAgainLater();
    }

    public void setAutoFocusListener(AutoFocusListener autoFocusListener) {
        this.mAutoFocusListener = autoFocusListener;
    }

    private synchronized void autoFocusAgainLater() {
        if (!this.stopped && this.outstandingTask == null) {
            AutoFocusManager.AutoFocusTask newTask = new AutoFocusManager.AutoFocusTask();

            try {
                newTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Object[0]);
                this.outstandingTask = newTask;
            } catch (RejectedExecutionException var3) {
                Log.w(TAG, "Could not request auto focus", var3);
            }
        }

    }

    private synchronized void start() {
        if (this.useAutoFocus) {
            this.outstandingTask = null;
            if (!this.stopped && !this.focusing) {
                try {
                    this.camera.autoFocus(this);
                    this.focusing = true;
                } catch (RuntimeException var2) {
                    Log.w(TAG, "Unexpected exception while focusing", var2);
                    this.autoFocusAgainLater();
                }
            }
        }

    }

    private synchronized void cancelOutstandingTask() {
        if (this.outstandingTask != null) {
            if (this.outstandingTask.getStatus() != Status.FINISHED) {
                this.outstandingTask.cancel(true);
            }

            this.outstandingTask = null;
        }

    }

    synchronized void stop() {
        this.stopped = true;
        if (this.useAutoFocus) {
            this.cancelOutstandingTask();

            try {
                this.camera.cancelAutoFocus();
            } catch (RuntimeException var2) {
                Log.w(TAG, "Unexpected exception while cancelling focusing", var2);
            }
        }

    }

    static {
        FOCUS_MODES_CALLING_AF.add("auto");
        FOCUS_MODES_CALLING_AF.add("macro");
    }

    private final class AutoFocusTask extends AsyncTask<Object, Object, Object> {
        private AutoFocusTask() {
        }

        protected Object doInBackground(Object... voids) {
            try {
                Thread.sleep(1200L);
            } catch (InterruptedException var3) {
                ;
            }

            AutoFocusManager.this.start();
            return null;
        }
    }
}
