package com.cms.android.wearable;

import java.util.Map;

import android.net.Uri;

import com.cms.android.common.data.Freezable;

public abstract interface DataItem extends Freezable<DataItem> {
	public abstract Map<String, DataItemAsset> getAssets();

	public abstract byte[] getData();

	public abstract Uri getUri();
	
	public abstract DataItem setData(byte[] data);
	
}