/*****************************************************************************
 *
 *                      HOPERUN PROPRIETARY INFORMATION
 *
 *          The information contained herein is proprietary to HopeRun
 *           and shall not be reproduced or disclosed in whole or in part
 *                    or used for any design or manufacture
 *              without direct written authorization from HopeRun.
 *
 *            Copyright (c) 2014 by HopeRun.  All rights reserved.
 *
 *****************************************************************************/
package com.clouder.records.service;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;

import com.clouder.records.R;
import com.clouder.records.activity.IncomingActivity;
import com.cms.android.common.ConnectionResult;
import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.common.api.MobvoiApiClient.ConnectionCallbacks;
import com.cms.android.common.api.MobvoiApiClient.OnConnectionFailedListener;
import com.cms.android.wearable.MessageApi;
import com.cms.android.wearable.MessageEvent;
import com.cms.android.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * ClassName: MessageListenerService
 *
 * @description
 * @author xing_peng
 * @Date 2015-7-17
 * 
 */
public class MessageListenerService extends Service implements OnConnectionFailedListener, ConnectionCallbacks,
		MessageApi.MessageListener {

	private static String TAG = "MessageListenerService";

	private MobvoiApiClient mobvoiApiClient;
	private Context mContext;
	private ContentResolver resolver;

	private static String INCOMING_CALL = "/call/incoming";
	private static String DIAL_CALL = "/call/dial";
	private static String REJECT_CALL = "/call/reject";
	private static String ACCEPT_CALL = "/call/accept";
	private static String TERMINATE_CALL = "/call/terminate";

	public static String ACCEPT_ACTION = "com.hoperun.watch.AcceptCall";
	public static String TERMINATE_ACTION = "com.hoperun.watch.TerminateCall";
	public static String REJECT_ACTION = "com.hoperun.watch.RejectCall";

	@Override
	public void onCreate() {

		mContext = this;

		mobvoiApiClient = new MobvoiApiClient.Builder(this).addApi(Wearable.API).addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this).build();
		mobvoiApiClient.connect();
	}

	private void handleIncomingCall(Context context, String node, String path, byte[] data) {
		if (!inZenMode(context)) {

			if (DIAL_CALL.equals(path)) {
				// dial
				String number = new String(data);
				Log.d(TAG, "DIAL number = " + number);
				long contactId = getContactId(number);

				Bitmap img = getPhoto(contactId);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				img.compress(Bitmap.CompressFormat.PNG, 100, baos);

				Intent startIntent = new Intent(context, IncomingActivity.class);
				startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startIntent.putExtra("number", number);
				startIntent.putExtra("name", getPeople(number));
				startIntent.putExtra("command", "dialCall");
				startIntent.putExtra("img", baos.toByteArray());
				context.startActivity(startIntent);

			} else if (INCOMING_CALL.equals(path)) {
				// incoming
				String number = new String(data);
				Log.d(TAG, "INCOMING number = " + number);
				long contactId = getContactId(number);

				Bitmap img = getPhoto(contactId);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				img.compress(Bitmap.CompressFormat.PNG, 100, baos);

				Intent startIntent = new Intent(context, IncomingActivity.class);
				startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startIntent.putExtra("number", number);
				startIntent.putExtra("name", getPeople(number));
				startIntent.putExtra("command", "incomingCall");
				startIntent.putExtra("img", baos.toByteArray());
				context.startActivity(startIntent);

			} else if (ACCEPT_CALL.equals(path)) {
				// accept
				Log.d(TAG, "ACCEPT_CALL");

				Intent updateIntent = new Intent();
				updateIntent.setAction(ACCEPT_ACTION);
				context.sendBroadcast(updateIntent);

			} else if (TERMINATE_CALL.equals(path)) {
				// terminate
				Log.d(TAG, "TERMINATE_CALL");

				Intent updateIntent = new Intent();
				updateIntent.setAction(TERMINATE_ACTION);
				context.sendBroadcast(updateIntent);

			} else if (REJECT_CALL.equals(path)) {
				// reject
				Log.d(TAG, "REJECT_CALL");

				Intent rejectIntent = new Intent();
				rejectIntent.setAction(REJECT_ACTION);
				context.sendBroadcast(rejectIntent);
			}
		}
	}

	// Check whether the flight mode
	private static boolean inZenMode(Context paramContext) {
		boolean bool = false;
		try {
			if (Settings.Global.getInt(paramContext.getContentResolver(), "in_zen_mode") != 0)
				bool = true;
		} catch (Settings.SettingNotFoundException localSettingNotFoundException) {
			Log.e(TAG, "Setting not found");
		}
		Log.d(TAG, "inZenMode : " + bool);
		return bool;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Log.e(TAG, "CMS ConnectionFailed " + connectionResult.toString());
	}

	@Override
	public void onConnected(Bundle bundle) {
		Log.e(TAG, "CMS Connected");
		Wearable.MessageApi.addListener(mobvoiApiClient, this);
	}

	@Override
	public void onConnectionSuspended(int cause) {
		Log.e(TAG, "CMS ConnectionSuspended cause:" + cause);
		mobvoiApiClient.connect();
	}

	@Override
	public void onMessageReceived(MessageEvent messageEvent) {
		if (messageEvent.getPath().startsWith("/call")) {
			Log.d(TAG, "onMessageReceived path : " + messageEvent.getPath());
			handleIncomingCall(getApplicationContext(), messageEvent.getSourceNodeId(), messageEvent.getPath(),
					messageEvent.getData());
		}
	}

	private long getContactId(String phoneNumber) {
		resolver = mContext.getContentResolver();
		long contactId = 0;
		Uri uri = Uri.parse("content://com.android.contacts/" + "data/phones/filter/" + phoneNumber);
		Cursor cursor = resolver.query(uri, null, null, null, null);

		if (cursor.moveToFirst()) {
			contactId = cursor.getLong(cursor.getColumnIndex("contact_id"));
		}
		return contactId;
	}

	private Bitmap getPhoto(Long contactId) {

		Bitmap bitmap = null;
		resolver = mContext.getContentResolver();

		Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
		InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(resolver, uri);
		if (input != null) {
			bitmap = BitmapFactory.decodeStream(input);
		} else {
			bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.img_call_head);
		}

		return bitmap;
	}

	public String getPeople(String mNumber) {
		String name = mNumber;
		String[] projection = { ContactsContract.PhoneLookup.DISPLAY_NAME,
				ContactsContract.CommonDataKinds.Phone.NUMBER };

		Cursor cursor = this.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection,
				ContactsContract.CommonDataKinds.Phone.NUMBER + " = '" + mNumber + "'", null, null);
		if (cursor == null) {
			return name;
		}

		cursor.moveToPosition(0);
		int nameFieldColumnIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
		cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
		name = cursor.getString(nameFieldColumnIndex);
		Log.i(TAG, "lanjianlong" + name + " .... " + nameFieldColumnIndex);

		if (cursor != null) {
			cursor.close();
		}
		return name;
	}
}
