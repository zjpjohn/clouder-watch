package com.cms.android.wearable.internal;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.cms.android.common.internal.safeparcel.SafeParcelable;
import com.cms.android.wearable.DataItemAsset;

public class DataItemAssetParcelable implements SafeParcelable, DataItemAsset {
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
		DataItemAsset dataItemAsset;
		if ((obj instanceof DataItemAsset)) {
			dataItemAsset = (DataItemAsset) obj;
			return TextUtils.equals(this.id, dataItemAsset.getId());
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

	public int hashCode() {
		return 23273 + 37 * this.id.hashCode();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("DataItemAssetParcelable[");
		sb.append("@");
		sb.append(Integer.toHexString(hashCode()));
		if (this.id == null) {
			sb.append(",noid");
		} else {
			sb.append(",");
			sb.append(this.id);
		}
		sb.append(", key=");
		sb.append(this.key);
		sb.append("]");
		return sb.toString();

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