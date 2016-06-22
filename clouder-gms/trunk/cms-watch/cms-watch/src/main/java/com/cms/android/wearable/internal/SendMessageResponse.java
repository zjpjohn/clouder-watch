package com.cms.android.wearable.internal;

import android.os.Parcel;
import android.os.Parcelable;

import com.cms.android.common.internal.safeparcel.SafeParcelable;

public class SendMessageResponse implements SafeParcelable {

	public static final Creator<SendMessageResponse> CREATOR = new Creator<SendMessageResponse>() {
		public SendMessageResponse createFromParcel(Parcel parcel) {
			return new SendMessageResponse(parcel);
		}

		public SendMessageResponse[] newArray(int size) {
			return new SendMessageResponse[size];
		}
	};
	public int requestId;
	public int statusCode;
	public int versionCode = 1;
	
	public SendMessageResponse() {
		super();
	}

	public SendMessageResponse(Parcel parcel) {
		this.requestId = parcel.readInt();
		this.statusCode = parcel.readInt();
		this.versionCode = parcel.readInt();
	}

	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(this.requestId);
		dest.writeInt(this.statusCode);
		dest.writeInt(this.versionCode);
		
	}
	
	public int getRequestId() {
		return requestId;
	}

	public void setRequestId(int requestId) {
		this.requestId = requestId;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	public int getVersionCode() {
		return versionCode;
	}

	public void setVersionCode(int versionCode) {
		this.versionCode = versionCode;
	}

	@Override
	public String toString() {
		return "SendMessageResponse [requestId=" + requestId + ", statusCode=" + statusCode + ", versionCode="
				+ versionCode + "]";
	}

}