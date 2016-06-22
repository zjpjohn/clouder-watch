package com.cms.android.wearable.internal;

import java.util.ArrayList;
import java.util.List;

import com.cms.android.common.internal.safeparcel.SafeParcelable;

import android.os.Parcel;
import android.os.Parcelable;

public class DataHolder implements SafeParcelable {
	public static final Parcelable.Creator<DataHolder> CREATOR = new DataHolderCreator();

	private int statusCode;
	
	private String packageName;

	private List<DataItemParcelable> dataItems = new ArrayList<DataItemParcelable>();

	private List<DataEventParcelable> dataEvents = new ArrayList<DataEventParcelable>();

	public DataHolder(int statusCode, String packageName, List<DataItemParcelable> dataItems, List<DataEventParcelable> dataEvents) {
		this.statusCode = statusCode;
		this.dataItems = dataItems;
		this.dataEvents = dataEvents;
		this.packageName = packageName;
	}

	public DataHolder(Parcel in) {
		this.statusCode = in.readInt();
		in.readList(this.dataItems, DataItemParcelable.class.getClassLoader());
		in.readList(this.dataEvents, DataEventParcelable.class.getClassLoader());
		this.packageName = in.readString();
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
	
	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	@Override
	public void writeToParcel(Parcel dest, int flag) {
		dest.writeInt(this.statusCode);
		dest.writeList(this.dataItems);
		dest.writeList(this.dataEvents);
		dest.writeString(this.packageName);
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