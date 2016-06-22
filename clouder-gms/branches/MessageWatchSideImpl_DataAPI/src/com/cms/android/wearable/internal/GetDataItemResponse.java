package com.cms.android.wearable.internal;

import android.os.Parcel;
import android.os.Parcelable;

import com.cms.android.common.internal.safeparcel.SafeParcelable;

public class GetDataItemResponse implements SafeParcelable {
	public static final Parcelable.Creator<GetDataItemResponse> CREATOR = new GetDataItemResponseCreator();
	public final DataItemParcelable dataItem;
	public final int status;
	public final int version;

	public GetDataItemResponse(int version, int status, DataItemParcelable dataItem) {
		this.version = version;
		this.status = status;
		this.dataItem = dataItem;
	}

	private GetDataItemResponse(Parcel in) {
		this.dataItem = in.readParcelable(DataItemParcelable.class.getClassLoader());
		this.version = in.readInt();
		this.status = in.readInt();
	}

	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flag) {
		dest.writeParcelable(this.dataItem, flag);
		dest.writeInt(this.version);
		dest.writeInt(this.status);
	}

	public static class GetDataItemResponseCreator implements Parcelable.Creator<GetDataItemResponse> {

		public GetDataItemResponse createFromParcel(Parcel in) {
			return new GetDataItemResponse(in);
		}

		public GetDataItemResponse[] newArray(int size) {
			return new GetDataItemResponse[size];
		}
	}
}