package com.cms.android.wearable.internal;

import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class DataHolder implements Parcelable {
	public static final Parcelable.Creator<DataHolder> CREATOR = new DataHolderCreator();

	private int statusCode;

	private List<DataItemParcelable> dataItems = new ArrayList<DataItemParcelable>();

	private List<DataEventParcelable> dataEvents = new ArrayList<DataEventParcelable>();

	public DataHolder(int statusCode, List<DataItemParcelable> dataItems, List<DataEventParcelable> dataEvents) {
		this.statusCode = statusCode;
		this.dataItems = dataItems;
		this.dataEvents = dataEvents;
	}

	public DataHolder(Parcel in) {
		this.statusCode = in.readInt();
		in.readList(this.dataItems, DataItemParcelable.class.getClassLoader());
		in.readList(this.dataEvents, DataEventParcelable.class.getClassLoader());
	}

	public int describeContents() {
		return 0;
	}

	public List<DataEventParcelable> getDataEvents() {
		return this.dataEvents;
	}

	public List<DataItemParcelable> getDataItems() {
		return this.dataItems;
	}

	public int getStatusCode() {
		return this.statusCode;
	}

	@Override
	public void writeToParcel(Parcel dest, int flag) {
		dest.writeInt(this.statusCode);
		dest.writeList(this.dataItems);
		dest.writeList(this.dataEvents);
	}

	public static class DataHolderCreator implements Parcelable.Creator<DataHolder> {

		public DataHolder createFromParcel(Parcel in) {
			return new DataHolder(in);
		}

		public DataHolder[] newArray(int size) {
			return new DataHolder[size];
		}
	}
}