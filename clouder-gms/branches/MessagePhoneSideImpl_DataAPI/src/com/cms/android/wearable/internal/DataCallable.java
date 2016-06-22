package com.cms.android.wearable.internal;

import java.io.IOException;
import java.util.concurrent.Callable;

import android.os.ParcelFileDescriptor;
import android.util.Log;

public class DataCallable implements Callable<Boolean> {

	private static final String TAG = "WearableAdapter";

	private ParcelFileDescriptor pfd;

	private byte[] data;

	DataCallable(ParcelFileDescriptor pfd, byte[] data) {
		this.pfd = pfd;
		this.data = data;
	}

	@Override
	public Boolean call() throws Exception {
		Log.d(TAG, "process assets: write data to FD : " + this.pfd);
		ParcelFileDescriptor.AutoCloseOutputStream autoCloseOutputStream = new ParcelFileDescriptor.AutoCloseOutputStream(
				this.pfd);
		try {
			autoCloseOutputStream.write(this.data);
			autoCloseOutputStream.flush();
			Log.d(TAG, "process assets: wrote bytes length " + this.data.length);
			Boolean result = Boolean.valueOf(true);
			return result;
		} catch (IOException e) {
			Log.d(TAG, "process assets: write data failed " + this.pfd);
		} finally {
			try {
				Log.d(TAG, "process assets: close " + this.pfd);
				autoCloseOutputStream.close();
			} catch (IOException e) {
			}
		}
		return Boolean.valueOf(false);
	}
}
