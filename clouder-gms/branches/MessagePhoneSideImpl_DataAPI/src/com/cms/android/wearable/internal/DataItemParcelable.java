package com.cms.android.wearable.internal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.cms.android.common.internal.safeparcel.SafeParcelable;
import com.cms.android.wearable.DataItem;
import com.cms.android.wearable.DataItemAsset;

public class DataItemParcelable implements SafeParcelable, DataItem {
	public static final Parcelable.Creator<DataItemParcelable> CREATOR = new DataItemCreator();

	private int versionCode;

	private Uri uri;

	private byte[] data;

	private Map<String, DataItemAsset> assets = new HashMap<String, DataItemAsset>();

	public DataItemParcelable(int versionCode, Uri uri, Bundle bundle, byte[] data) {
		this.versionCode = versionCode;
		this.uri = uri;
		this.data = data;
		if (bundle != null) {
			bundle.setClassLoader(DataItemAssetParcelable.class.getClassLoader());
			Iterator<String> iterator = bundle.keySet().iterator();
			while (iterator.hasNext()) {
				String key = iterator.next();
				this.assets.put(key, (DataItemAssetParcelable) bundle.getParcelable(key));
			}
		}
	}

	private DataItemParcelable(Parcel in) {
		this.versionCode = in.readInt();
		this.uri = in.readParcelable(Uri.class.getClassLoader());
		int size = in.readInt();
		if (size > 0) {
			this.data = new byte[size];
			in.readByteArray(this.data);
		} else {
			this.data = new byte[0];
		}
		Bundle bundle = in.readBundle(DataItemAssetParcelable.class.getClassLoader());
		if (bundle != null) {
			Iterator<String> iterator = bundle.keySet().iterator();
			while (iterator.hasNext()) {
				String key = iterator.next();
				this.assets.put(key, (DataItemAsset) bundle.getParcelable(key));
			}
		}

	}

	public DataItemParcelable(Uri uri, Bundle bundle, byte[] bytes) {
		this(1, uri, bundle, bytes);
	}

	public int describeContents() {
		return 0;
	}

	public DataItem freeze() {
		return this;
	}

	public Map<String, DataItemAsset> getAssets() {
		return this.assets;
	}

	public Bundle getBundle() {
		Bundle bundle = new Bundle();
		bundle.setClassLoader(DataItemAssetParcelable.class.getClassLoader());
		Iterator<String> iterator = this.assets.keySet().iterator();
		while (iterator.hasNext()) {
			String key = (String) iterator.next();
			bundle.putParcelable(key, new DataItemAssetParcelable((DataItemAsset) this.assets.get(key)));
		}
		return bundle;
	}

	public byte[] getData() {
		return this.data;
	}

	public Uri getUri() {
		return this.uri;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("DataItemParcelable[");
		sb.append("@");
		sb.append(Integer.toHexString(hashCode()));
		sb.append(",dataSz=");
		Object localObject = null;
		if (this.data == null) {
			localObject = "null";
		} else {
			localObject = Integer.valueOf(this.data.length);
		}
		sb.append(localObject);
		sb.append(", numAssets=" + this.assets.size());
		sb.append(", uri=" + this.uri);
		sb.append("]\n  assets: ");
		Iterator<String> localIterator = this.assets.keySet().iterator();
		while (localIterator.hasNext()) {
			String str = (String) localIterator.next();
			sb.append("\n    " + str + ": " + this.assets.get(str));
		}
		sb.append("\n  ]");
		return sb.toString();
	}

	public void writeToParcel(Parcel dest, int flag) {
		dest.writeInt(this.versionCode);
		dest.writeParcelable(this.uri, flag);
		dest.writeInt(this.data == null ? 0 : this.data.length);
		if (this.data != null && this.data.length != 0) {
			dest.writeByteArray(this.data);
		}
		dest.writeBundle(getBundle());
	}

	public static class DataItemCreator implements Parcelable.Creator<DataItemParcelable> {
		public DataItemParcelable createFromParcel(Parcel in) {
			return new DataItemParcelable(in);
		}

		public DataItemParcelable[] newArray(int size) {
			return new DataItemParcelable[size];
		}
	}

	@Override
	public DataItem setData(byte[] data) {
		// TODO Auto-generated method stub
		return null;
	}
}