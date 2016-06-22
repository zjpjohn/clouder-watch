package com.cms.android.wearable.service.codec;

import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.cms.android.wearable.Asset;

public class ChildAsset extends Asset {

	private String uuid;

	private long size;

	private int index;

	private long assetSize;

	ChildAsset(int versionCode, byte[] data, String digest, ParcelFileDescriptor fd, Uri uri) {
		super(versionCode, data, digest, fd, uri);
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public void setDigest(String digest) {
		this.digest = digest;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public long getAssetSize() {
		return assetSize;
	}

	public void setAssetSize(long assetSize) {
		this.assetSize = assetSize;
	}

}
