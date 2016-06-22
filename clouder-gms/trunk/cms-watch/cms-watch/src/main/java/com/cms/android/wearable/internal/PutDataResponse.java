package com.cms.android.wearable.internal;

import android.os.Parcel;
import android.os.Parcelable;

import com.cms.android.common.internal.safeparcel.SafeParcelable;

public class PutDataResponse implements SafeParcelable {

	public static final Creator<PutDataResponse> CREATOR = new PutDataResponseCreator();

	public final int versionCode;
	public final int status;
	public final DataItemParcelable dataItem;

	public PutDataResponse(int versionCode, int status, DataItemParcelable dataItem) {
		this.versionCode = versionCode;
		this.status = status;
		this.dataItem = dataItem;
	}

	private PutDataResponse(Parcel in) {
		this.versionCode = in.readInt();
		this.status = in.readInt();
		this.dataItem = in.readParcelable(DataItemParcelable.class.getClassLoader());
	}

	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flag) {
		dest.writeInt(this.versionCode);
		dest.writeInt(this.status);
		dest.writeParcelable(this.dataItem, flag);
	}

	public static class PutDataResponseCreator implements Creator<PutDataResponse> {

		@Override
		public PutDataResponse createFromParcel(Parcel in) {
			return new PutDataResponse(in);
		}

		@Override
		public PutDataResponse[] newArray(int size) {
			return new PutDataResponse[size];
		}
	}
}
