package com.clouder.watch.mobile;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.util.Log;

public class ContactContentObservers extends ContentObserver {

	private static String TAG = "ContentObserver";
	private int CONTACT_CHANGE = 1;
	private Handler mHandler;

	public ContactContentObservers(Context context, Handler handler) {
		super(handler);
		mHandler = handler;
	}

	@Override
	public void onChange(boolean selfChange) {
		Log.v(TAG, "the contacts has changed");
		mHandler.obtainMessage(CONTACT_CHANGE, "change").sendToTarget();
	}

}
