package com.cms.android.wearable.internal;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;

import com.cms.android.wearable.MessageApi;
import com.cms.android.wearable.NodeApi;
import com.cms.android.wearable.service.impl.IWearableListener;

public class WearableListener extends IWearableListener.Stub implements Parcelable {

	private static final String TAG = "WearableListener";

	private MessageApi.MessageListener mMessageListener;
	
	private NodeApi.NodeListener nodeListener;

	public WearableListener(Parcel parcel) {
		Log.i(TAG, "哈哈哈，WearableListener 构造");
	}

	public WearableListener(MessageApi.MessageListener messageListener) {
		this.mMessageListener = messageListener;
	}
	
	public WearableListener(NodeApi.NodeListener nodeListener) {
		this.nodeListener = nodeListener;
	}

	public static final Parcelable.Creator<WearableCallback> CREATOR = new Parcelable.Creator<WearableCallback>() {
		public WearableCallback createFromParcel(Parcel parcel) {
			return new WearableCallback(parcel);
		}

		public WearableCallback[] newArray(int size) {
			return new WearableCallback[size];
		}
	};

	// @Override
	// public void onMessageReceived(MessageEventHolder messageEventHolder)
	// throws RemoteException {
	// Log.i(TAG, "哈哈哈，onMessageReceived");
	// // Log.d(TAG, "on message received, event = " + messageEventHolder +
	// ", listener = " + this.mMessageListener);
	// // if (this.mMessageListener != null)
	// // this.mMessageListener.onMessageReceived(messageEventHolder);
	// }

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		Log.i(TAG, "哈哈哈，writeToParcel");
	}

	@Override
	public void onMessageReceived(MessageEventHolder messageEventHolder) throws RemoteException {
		Log.d(TAG, "on message received, event = " + messageEventHolder + ", listener = " + this.mMessageListener);
		if (this.mMessageListener != null) {
			this.mMessageListener.onMessageReceived(messageEventHolder);
		}

	}

	@Override
	public void onPeerConnected(NodeHolder nodeHolder) throws RemoteException {
		Log.d(TAG, "onPeerConnected, nodeHolder = " + nodeHolder + ", listener = " + this.nodeListener);
		if (this.nodeListener != null) {
			this.nodeListener.onPeerConnected(nodeHolder);
		}
		
	}

	@Override
	public void onPeerDisconnected(NodeHolder nodeHolder) throws RemoteException {
		Log.d(TAG, "onPeerConnected, nodeHolder = " + nodeHolder + ", listener = " + this.nodeListener);
		if (this.nodeListener != null) {
			this.nodeListener.onPeerDisconnected(nodeHolder);
		}
		
	}

}