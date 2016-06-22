package com.example.testasset;


public abstract interface DataItemAsset extends Freezable<DataItemAsset> {
	public abstract String getDataItemKey();

	public abstract String getId();
}