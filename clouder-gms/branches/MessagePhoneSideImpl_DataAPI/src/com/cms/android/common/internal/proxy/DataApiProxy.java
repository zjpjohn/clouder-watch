package com.cms.android.common.internal.proxy;

import android.util.Log;

import com.cms.android.common.MobvoiApiManager;
import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.common.api.PendingResult;
import com.cms.android.common.api.Status;
import com.cms.android.wearable.Asset;
import com.cms.android.wearable.DataApi;
import com.cms.android.wearable.DataItemBuffer;
import com.cms.android.wearable.PutDataRequest;
import com.cms.android.wearable.api.impl.DataApiImpl;

public class DataApiProxy implements Loadable, DataApi {
	private DataApi instance;

	public DataApiProxy() {
		MobvoiApiManager.getInstance().registerProxy(this);
		load();
	}

	@Override
	public PendingResult<DataItemResult> putDataItem(MobvoiApiClient mobvoiApiClient, PutDataRequest putDataRequest) {
		return this.instance.putDataItem(mobvoiApiClient, putDataRequest);
	}

	public PendingResult<DataItemBuffer> getDataItems(MobvoiApiClient mobvoiApiClient) {
		Log.d("MobvoiApiManager", "DataApiProxy#getDataItems()");
		return this.instance.getDataItems(mobvoiApiClient);
	}

	public PendingResult<DataApi.GetFdForAssetResult> getFdForAsset(MobvoiApiClient mobvoiApiClient, Asset asset) {
		Log.d("MobvoiApiManager", "DataApiProxy#getFdForAsset()");
		return this.instance.getFdForAsset(mobvoiApiClient, asset);
	}

	@Override
	public PendingResult<Status> addListener(MobvoiApiClient mobvoiApiClient, DataListener listener) {
		return this.instance.addListener(mobvoiApiClient, listener);
	}

	@Override
	public PendingResult<Status> removeListener(MobvoiApiClient mobvoiApiClient, DataListener listener) {
		return this.instance.removeListener(mobvoiApiClient, listener);
	}

	public void load() {
		if (MobvoiApiManager.getInstance().getGroup() == MobvoiApiManager.ApiGroup.CMS) {
			this.instance = new DataApiImpl();
		}

		// if (MobvoiApiManager.getInstance().getGroup() !=
		// MobvoiApiManager.ApiGroup.GMS) {
		// this.instance = new DataApiGoogleImpl();
		// }

		Log.d("MobvoiApiManager", "load data api success.");
	}

}