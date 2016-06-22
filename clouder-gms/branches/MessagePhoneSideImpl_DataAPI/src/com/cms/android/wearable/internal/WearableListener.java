package com.cms.android.wearable.internal;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;

import com.cms.android.wearable.DataApi;
import com.cms.android.wearable.DataEvent;
import com.cms.android.wearable.DataEventBuffer;
import com.cms.android.wearable.MessageApi;
import com.cms.android.wearable.NodeApi;
import com.cms.android.wearable.service.impl.IWearableListener;

public class WearableListener extends IWearableListener.Stub implements Parcelable {

	private static final String TAG = "WearableListener";

	private MessageApi.MessageListener mMessageListener;

	private DataApi.DataListener mDataListener;
	
	private NodeApi.NodeListener mNodeListener;

	public WearableListener(Parcel parcel) {
	}

	public WearableListener(MessageApi.MessageListener messageListener) {
		this.mMessageListener = messageListener;
	}

	public WearableListener(DataApi.DataListener dataListener) {
		this.mDataListener = dataListener;
	}
	
	public WearableListener(NodeApi.NodeListener nodeListener) {
		this.mNodeListener = nodeListener;
	}

	public static final Parcelable.Creator<WearableCallback> CREATOR = new Parcelable.Creator<WearableCallback>() {

		public WearableCallback createFromParcel(Parcel parcel) {
			return new WearableCallback(parcel);
		}

		public WearableCallback[] newArray(int size) {
			return new WearableCallback[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
	}

	@Override
	public void onMessageReceived(MessageEventHolder messageEventHolder) throws RemoteException {
		Log.d(TAG, "on message received, event = " + messageEventHolder + ", listener = " + this.mMessageListener);
		if (this.mMessageListener != null) {
			this.mMessageListener.onMessageReceived(messageEventHolder);
		}
	}

	@Override
	public void onDataChanged(DataHolder dataHolder) throws RemoteException {
		Log.d(TAG, "on data changed, event = " + dataHolder + ", listener = " + this.mDataListener);
		if (this.mDataListener != null) {
			Log.e(TAG, "type = " + dataHolder.getDataEvents().get(0).getType());
			DataEventBuffer dataEventBuffer = new DataEventBuffer(dataHolder);
			for (DataEvent event : dataEventBuffer) {
				Log.e(TAG, "handle onDataChanged->" + event.getType() + " " + event.getDataItem().getUri());
			}
			this.mDataListener.onDataChanged(dataEventBuffer);
		}
	}

	@Override
	public String id() throws RemoteException {
		
		if(mMessageListener!= null){
			return mMessageListener.toString();
		}
		if(mDataListener!=null){
			return mDataListener.toString();
		}
		if(mNodeListener != null){
			return mNodeListener.toString();
		}
		return null;
	}
	
	@Override
	public void onPeerConnected(NodeHolder nodeHolder) throws RemoteException {
		Log.d(TAG, "onPeerConnected, nodeHolder = " + nodeHolder + ", listener = " + this.mNodeListener);
		if (this.mNodeListener != null) {
			this.mNodeListener.onPeerConnected(nodeHolder);
		}
		
	}

	@Override
	public void onPeerDisconnected(NodeHolder nodeHolder) throws RemoteException {
		Log.d(TAG, "onPeerDisconnected, nodeHolder = " + nodeHolder + ", listener = " + this.mNodeListener);
		if (this.mNodeListener != null) {
			this.mNodeListener.onPeerDisconnected(nodeHolder);
		}
		
	}

}