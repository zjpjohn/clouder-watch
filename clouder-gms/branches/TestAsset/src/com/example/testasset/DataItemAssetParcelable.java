package com.example.testasset;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class DataItemAssetParcelable implements Parcelable, DataItemAsset {
	public static final Parcelable.Creator<DataItemAssetParcelable> CREATOR = new DataItemAssetCreator();

	private final String id;
	private final String key;
	final int versionCode;

	public DataItemAssetParcelable(int versionCode, String id, String key) {
		this.versionCode = versionCode;
		this.id = id;
		this.key = key;
	}

	public DataItemAssetParcelable(DataItemAsset dataItemAsset) {
		this.versionCode = 1;
		this.id = dataItemAsset.getId();
		this.key = dataItemAsset.getDataItemKey();
	}

	public DataItemAssetParcelable(String id, String key) {
		this(1, id, key);
	}

	private DataItemAssetParcelable(Parcel in) {
		this.id = in.readString();
		this.key = in.readString();
		this.versionCode = in.readInt();
	}

	public int describeContents() {
		return 0;
	}

	public boolean equals(Object obj) {
		DataItemAsset localDataItemAsset;
		if ((obj instanceof DataItemAsset)) {
			localDataItemAsset = (DataItemAsset) obj;
			return TextUtils.equals(this.id, localDataItemAsset.getId());
		}
		return false;
	}

	public DataItemAsset freeze() {
		return this;
	}

	public String getDataItemKey() {
		return this.key;
	}

	public String getId() {
		return this.id;
	}

	public String toString() {
		StringBuilder localStringBuilder = new StringBuilder();
		localStringBuilder.append("DataItemAssetParcelable[");
		localStringBuilder.append("@");
		localStringBuilder.append(Integer.toHexString(hashCode()));
		if (this.id == null) {
			localStringBuilder.append(",noid");
		} else {
			localStringBuilder.append(",");
			localStringBuilder.append(this.id);
		}
		localStringBuilder.append(", key=");
		localStringBuilder.append(this.key);
		localStringBuilder.append("]");
		return localStringBuilder.toString();

	}

	public void writeToParcel(Parcel dest, int flag) {
		dest.writeString(this.id);
		dest.writeString(this.key);
		dest.writeInt(this.versionCode);
	}

	public static class DataItemAssetCreator implements Parcelable.Creator<DataItemAssetParcelable> {

		public DataItemAssetParcelable createFromParcel(Parcel in) {
			return new DataItemAssetParcelable(in);
		}

		public DataItemAssetParcelable[] newArray(int size) {
			return new DataItemAssetParcelable[size];
		}
	}
}