package com.example.testasset;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

public class PutDataRequest implements Parcelable {
	public static final String WEAR_URI_SCHEME = "wear";
	public static final Parcelable.Creator<PutDataRequest> CREATOR = new PutDataRequestCreator();
	@SuppressLint("TrulyRandom")
	private static final Random b = new SecureRandom();
	private int versionCode;
	private Uri mUri;
	private Bundle assets;
	private byte[] data;

	PutDataRequest(int versionCode, Uri mUri, Bundle assets, byte[] data) {
		this.versionCode = versionCode;
		this.mUri = mUri;
		this.assets = assets;
		this.data = data;
	}

	private PutDataRequest(Parcel in) {
		this.versionCode = in.readInt();
		this.mUri = in.readParcelable(Uri.class.getClassLoader());
		this.assets = in.readBundle(Asset.class.getClassLoader());
		int size = in.readInt();
		if (size > 0) {
			Log.d("spencer", "size > 0");
			this.data = new byte[size];
			in.readByteArray(this.data);
		} else {
			Log.d("spencer", "000000");
			this.data = new byte[0];
		}
	}

	public static PutDataRequest createFromUri(Uri uri) {
		return new PutDataRequest(1, uri, new Bundle(), null);
	}

	public static PutDataRequest create(String uri) {
		return createFromUri(parse(uri));
	}

	public static PutDataRequest createFromDataItem(DataItem dataItem) {
		PutDataRequest request = createFromUri(dataItem.getUri());
		Iterator<Entry<String, DataItemAsset>> iterator = dataItem.getAssets().entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, DataItemAsset> localEntry = (Entry<String, DataItemAsset>) iterator.next();
			if (localEntry.getValue().getId() == null)
				throw new IllegalStateException("asset id must exist, key = " + (String) localEntry.getKey());
			request.putAsset((String) localEntry.getKey(),
					Asset.createFromRef(((DataItemAsset) localEntry.getValue()).getId()));
		}
		request.setData(dataItem.getData());
		return request;
	}

	public static PutDataRequest createWithAutoAppendedId(String appendedId) {
		StringBuilder sb = new StringBuilder(appendedId);
		if (!appendedId.endsWith("/"))
			sb.append("/");
		sb.append("PN").append(b.nextLong());
		return createFromUri(parse(sb.toString()));
	}

	private static Uri parse(String path) {
		if (TextUtils.isEmpty(path))
			throw new IllegalArgumentException("path is null or empty.");
		if ((!path.startsWith("/")) || (path.startsWith("//")))
			throw new IllegalArgumentException("path must start with a single / .");
		return new Uri.Builder().scheme("wear").path(path).build();
	}

	public String toString() {
		return "PutDataRequest[@ Uri:" + this.mUri + ", DataSz: " + (this.data == null ? 0 : this.data.length)
				+ ", assetNum: " + this.assets.size() + "]";
	}

	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flag) {
		dest.writeInt(this.versionCode);
		dest.writeParcelable(this.mUri, flag);
		dest.writeBundle(this.assets);
		dest.writeInt(this.data.length);
		dest.writeByteArray(this.data);
	}

	public Map<String, Asset> getAssets() {
		HashMap<String, Asset> map = new HashMap<String, Asset>();
		Iterator<String> iterator = this.assets.keySet().iterator();
		while (iterator.hasNext()) {
			String str = (String) iterator.next();
			map.put(str, (Asset) this.assets.getParcelable(str));
		}
		return Collections.unmodifiableMap(map);
	}

	public Uri getUri() {
		return this.mUri;
	}

	public byte[] getData() {
		return this.data;
	}

	public PutDataRequest setData(byte[] data) {
		this.data = data;
		return this;
	}

	public boolean hasAsset(String key) {
		return this.assets.containsKey(key);
	}

	public Asset getAsset(String key) {
		return (Asset) this.assets.get(key);
	}

	public PutDataRequest putAsset(String key, Asset asset) {
		if ((key == null) || (asset == null))
			throw new NullPointerException("parameters is null, key = " + key + ", asset = " + asset);
		if ((asset.getData() == null) && (asset.getDigest() == null))
			throw new IllegalArgumentException("asset is empty");
		this.assets.putParcelable(key, asset);
		return this;
	}

	public PutDataRequest removeAsset(String key) {
		this.assets.remove(key);
		return this;
	}

	public int getVersionCode() {
		return this.versionCode;
	}

	public Bundle getBundle() {
		return this.assets;
	}

	public static class PutDataRequestCreator implements Parcelable.Creator<PutDataRequest> {

		@Override
		public PutDataRequest createFromParcel(Parcel in) {
			return new PutDataRequest(in);
		}

		@Override
		public PutDataRequest[] newArray(int size) {
			return new PutDataRequest[size];
		}
	}
}