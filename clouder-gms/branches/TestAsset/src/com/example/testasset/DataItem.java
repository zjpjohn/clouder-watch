package com.example.testasset;

import java.util.Map;

import android.net.Uri;

public abstract interface DataItem extends Freezable<DataItem> {
	public abstract Map<String, DataItemAsset> getAssets();

	public abstract byte[] getData();

	public abstract Uri getUri();
}