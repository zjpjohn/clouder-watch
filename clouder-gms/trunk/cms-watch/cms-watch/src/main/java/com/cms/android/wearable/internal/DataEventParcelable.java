package com.cms.android.wearable.internal;

import android.os.Parcel;
import android.os.Parcelable;

import com.cms.android.common.internal.safeparcel.SafeParcelable;
import com.cms.android.wearable.DataEvent;
import com.cms.android.wearable.DataItem;

public class DataEventParcelable implements SafeParcelable, DataEvent {

	public static final Creator<DataEventParcelable> CREATOR = new DataEventCreator();

	private int type;

	private DataItemParcelable dataItem;

	public DataEventParcelable(int type, DataItemParcelable dataItem) {
		this.type = type;
		this.dataItem = dataItem;
	}

	public DataEventParcelable(Parcel in) {
		this.type = in.readInt();
		this.dataItem = in.readParcelable(DataItemParcelable.class.getClassLoader());
	}

	public int describeContents() {
		return 0;
	}

	public DataEvent freeze() {
		this.dataItem.freeze();
		return this;
	}

	public DataItem getDataItem() {
		return this.dataItem;
	}

	public void writeToParcel(Parcel dest, int flag) {
		dest.writeInt(this.type);
		dest.writeParcelable(this.dataItem, flag);
	}

	public static class DataEventCreator implements Creator<DataEventParcelable> {

		public DataEventParcelable createFromParcel(Parcel in) {
			return new DataEventParcelable(in);
		}

		public DataEventParcelable[] newArray(int size) {
			return new DataEventParcelable[size];
		}
	}

	@Override
	public int getType() {
		return this.type;
	}
}