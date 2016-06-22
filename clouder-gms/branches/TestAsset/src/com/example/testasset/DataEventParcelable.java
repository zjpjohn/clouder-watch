package com.example.testasset;

import android.os.Parcel;
import android.os.Parcelable;

public class DataEventParcelable implements Parcelable, DataEvent {

	public static final Parcelable.Creator<DataEventParcelable> CREATOR = new DataEventCreator();

	private int type;

	private DataItemParcelable dataItem;

	public DataEventParcelable(int type, DataItemParcelable dataItem) {
		this.type = type;
		this.dataItem = dataItem;
	}

	public DataEventParcelable(Parcel in) {
		// TODO check
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

	public static class DataEventCreator implements Parcelable.Creator<DataEventParcelable> {

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