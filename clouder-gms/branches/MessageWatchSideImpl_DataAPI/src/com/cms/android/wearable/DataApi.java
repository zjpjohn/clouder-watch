package com.cms.android.wearable;

import java.io.InputStream;

import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.common.api.PendingResult;
import com.cms.android.common.api.Result;
import com.cms.android.common.api.Status;

public abstract interface DataApi {
	
	public abstract PendingResult<DataItemResult> putDataItem(MobvoiApiClient mobvoiApiClient, PutDataRequest putDataRequest);
	
	public abstract PendingResult<DataItemBuffer> getDataItems(MobvoiApiClient mobvoiApiClient);

	public abstract PendingResult<GetFdForAssetResult> getFdForAsset(MobvoiApiClient mobvoiApiClient,
			Asset asset);
	
	PendingResult<Status> addListener(MobvoiApiClient mobvoiApiClient, DataApi.DataListener listener);

    PendingResult<Status> removeListener(MobvoiApiClient mobvoiApiClient, DataApi.DataListener listener);

	public static abstract interface DataItemResult extends Result {
	}

	public static abstract interface DataListener {
		public abstract void onDataChanged(DataEventBuffer dataEventBuffer);
	}

	public static abstract interface DeleteDataItemsResult extends Result {
	}

	public static abstract interface GetFdForAssetResult extends Result {
		public abstract InputStream getInputStream();
	}
}