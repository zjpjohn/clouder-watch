/*****************************************************************************
 *
 *                      HOPERUN PROPRIETARY INFORMATION
 *
 *          The information contained herein is proprietary to HopeRun
 *           and shall not be reproduced or disclosed in whole or in part
 *                    or used for any design or manufacture
 *              without direct written authorization from HopeRun.
 *
 *            Copyright (coffee) 2015 by HopeRun.  All rights reserved.
 *
 *****************************************************************************/
package com.cms.android.wearable.api.impl;

import android.os.RemoteException;
import android.util.Log;

import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.common.api.PendingResult;
import com.cms.android.common.api.Status;
import com.cms.android.wearable.MessageApi;
import com.cms.android.wearable.internal.WearableAdapter;
import com.cms.android.wearable.internal.WearableListener;
import com.cms.android.wearable.internal.WearableResult;

/**
 * ClassName: MessageApiImpl
 * 
 * @description MessageApiImpl
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public class MessageApiImpl implements MessageApi {

	private static final String TAG = "MessageApiImpl";

	public PendingResult<MessageApi.SendMessageResult> sendMessage(MobvoiApiClient mobvoiApiClient, final String node,
			final String path, final byte[] data) {
		return mobvoiApiClient.setResult(new WearableResult<MessageApi.SendMessageResult>() {

			@Override
			protected void connect(WearableAdapter adapter) throws RemoteException {
				Log.i(TAG, String.format("WearableAdapter sendMessage node : %s path : %s and data's length: %d", node,
						path, (data == null ? 0 : data.length)));
				adapter.sendMessage(this, node, path, data);
			}

			@Override
			protected SendMessageResult create(Status status) {
				Log.d(TAG, "create SendMessageResult : " + status.toString());
				return new MessageApiImpl.SendMessageResultImpl(status, 1);
			}

		});
	}

	@Override
	public PendingResult<Status> addListener(MobvoiApiClient mobvoiApiClient, MessageListener listener) {

		final WearableListener wearableListener = new WearableListener(listener);

		return mobvoiApiClient.setResult(new WearableResult<Status>() {

			@Override
			protected void connect(WearableAdapter adapter) throws RemoteException {
				Log.d(TAG, "WearableAdapter add WearableListener");
				adapter.addListener(this, wearableListener);
			}

			@Override
			protected Status create(Status status) {
				Log.d(TAG, "create addListener status: " + status.toString());
				return status;
			}
		});

	}

	@Override
	public PendingResult<Status> removeListener(MobvoiApiClient mobvoiApiClient, MessageListener listener) {
		final WearableListener wearableListener = new WearableListener(listener);

		return mobvoiApiClient.setResult(new WearableResult<Status>() {

			@Override
			protected void connect(WearableAdapter adapter) throws RemoteException {
				Log.d(TAG, "WearableAdapter remove WearableListener");
				adapter.removeListener(this, wearableListener);
			}

			@Override
			protected Status create(Status status) {
				Log.d(TAG, "create removeListener status: " + status.toString());
				return status;
			}
		});
	}

	public static final class SendMessageResultImpl implements MessageApi.SendMessageResult {

		private final int mRequestId;

		private final Status mStatus;

		public SendMessageResultImpl(Status status, int requestId) {
			this.mStatus = status;
			this.mRequestId = requestId;
		}

		@Override
		public Status getStatus() {
			return this.mStatus;
		}

		public int getRequestId() {
			return this.mRequestId;
		}
	}

}