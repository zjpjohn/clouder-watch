package com.cms.android.wearable;

import com.cms.android.common.data.Freezable;

public abstract interface DataItemAsset extends Freezable<DataItemAsset> {
	public abstract String getDataItemKey();

	public abstract String getId();
}