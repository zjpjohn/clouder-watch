package com.cms.android.wearable.internal;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;

import com.cms.android.common.api.Status;
import com.cms.android.wearable.service.impl.IBLECentralCallback;

public class WearableCallback extends IBLECentralCallback.Stub implements Parcelable {

	private Context context;
	
	public WearableCallback(Context context) {
		super();
		this.context = context;
	}

	public WearableCallback(Parcel parcel) {

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
	public void setSendMessageRsp(SendMessageResponse sendMessageResponse) throws RemoteException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setStatusRsp(Status status) throws RemoteException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setGetFdForAssetRsp(GetFdForAssetResponse getFdForAssetResponse) throws RemoteException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setDataHolderRsp(DataHolder dataHolder) throws RemoteException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setGetConnectedNodesRsp(GetConnectedNodesResponse getConnectedNodesResponse) throws RemoteException {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setGetLocalNodeRsp(GetLocalNodeResponse getLocalNodeResponse) throws RemoteException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setPutDataRsp(PutDataResponse response) throws RemoteException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {

	}

	@Override
	public String getPackageName() throws RemoteException {
		String packageName = null;
		if (context != null) {
			packageName = context.getPackageName();
		}
		return packageName;
	}

	@Override
	public void setAssetRsp() throws RemoteException {
		throw new UnsupportedOperationException();
		
	}

}
