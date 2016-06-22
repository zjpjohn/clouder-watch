package com.cms.android.wearable.api.impl;

import java.io.InputStream;

import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.common.api.PendingResult;
import com.cms.android.common.api.Status;
import com.cms.android.wearable.Asset;
import com.cms.android.wearable.DataApi;
import com.cms.android.wearable.DataItem;
import com.cms.android.wearable.DataItemBuffer;
import com.cms.android.wearable.PutDataRequest;
import com.cms.android.wearable.internal.WearableAdapter;
import com.cms.android.wearable.internal.WearableListener;
import com.cms.android.wearable.internal.WearableResult;

public class DataApiImpl implements DataApi {

	private static final String TAG = "DataApiImpl";

	@Override
	public PendingResult<DataItemResult> putDataItem(MobvoiApiClient mobvoiApiClient,
			final PutDataRequest putDataRequest) {
		return mobvoiApiClient.setResult(new WearableResult<DataItemResult>() {

			@Override
			protected void connect(WearableAdapter adapter) throws RemoteException {
				Log.d(TAG, "putDataItem connect...");
				adapter.putDataItem(this, putDataRequest);
			}

			@Override
			protected DataItemResult create(Status status) {
				Log.d(TAG, "create DataItemResult : " + status);
				return new DataItemResultImpl(status, null);
			}
		});
	}

	public PendingResult<DataItemBuffer> getDataItems(MobvoiApiClient mobvoiApiClient) {
		return mobvoiApiClient.setResult(new WearableResult<DataItemBuffer>() {

			@Override
			protected void connect(WearableAdapter adapter) throws RemoteException {
				Log.d(TAG, "connect getDataItems");
				// adapter.getDataItems(this);
			}

			@Override
			protected DataItemBuffer create(Status status) {
				Log.d(TAG, "create DataItemBuffer : " + status);
				return new DataItemBuffer(status);
			}

		});
	}

	public PendingResult<DataApi.GetFdForAssetResult> getFdForAsset(MobvoiApiClient mobvoiApiClient, final Asset asset) {
		if (asset == null)
			throw new IllegalArgumentException("asset is null");
		if ((asset.getDigest() == null) || (asset.getData() != null))
			throw new IllegalArgumentException("invalid asset, digest = " + asset.getDigest() + ", data = "
					+ asset.getData());
		return mobvoiApiClient.setResult(new WearableResult<GetFdForAssetResult>() {
			protected void connect(WearableAdapter adapter) throws RemoteException {
				adapter.getFdForAsset(this, asset);
			}

			protected DataApi.GetFdForAssetResult create(Status status) {
				return new DataApiImpl.GetFdForAssetResultImpl(status, null);
			}
		});
	}

	@Override
	public PendingResult<Status> addListener(MobvoiApiClient mobvoiApiClient, DataListener listener) {
		final WearableListener wearableListener = new WearableListener(listener);

		return mobvoiApiClient.setResult(new WearableResult<Status>() {

			@Override
			protected void connect(WearableAdapter adapter) throws RemoteException {
				Log.d(TAG, "WearableAdapter add WearableListener");
				adapter.addDataListener(this, wearableListener);
			}

			@Override
			protected Status create(Status status) {
				Log.d(TAG, "create addListener status: " + status);
				return status;
			}
		});
	}

	@Override
	public PendingResult<Status> removeListener(MobvoiApiClient mobvoiApiClient, DataListener listener) {
		final WearableListener wearableListener = new WearableListener(listener);

		return mobvoiApiClient.setResult(new WearableResult<Status>() {

			@Override
			protected void connect(WearableAdapter adapter) throws RemoteException {
				Log.d(TAG, "WearableAdapter remove WearableListener");
				adapter.removeDataListener(this, wearableListener);
			}

			@Override
			protected Status create(Status status) {
				Log.d(TAG, "create removeListener status: " + status);
				return status;
			}
		});

	}

	public static class DataItemResultImpl implements DataApi.DataItemResult {

		private DataItem dataItem;

		private Status status;

		public DataItemResultImpl(Status status, DataItem dataItem) {
			this.status = status;
			this.dataItem = dataItem;
		}

		@Override
		public Status getStatus() {
			return this.status;
		}

		public DataItem getDataItem() {
			return dataItem;
		}

	}

	public static class DeleteDataItemsResultImpl implements DataApi.DeleteDataItemsResult {
		private int numDeleted;
		private Status status;

		public DeleteDataItemsResultImpl(Status status, int numDeleted) {
			this.status = status;
			this.numDeleted = numDeleted;
		}

		public Status getStatus() {
			return this.status;
		}

		public int getNumDeleted() {
			return numDeleted;
		}
	}

	public static class GetFdForAssetResultImpl implements DataApi.GetFdForAssetResult {
		private ParcelFileDescriptor fd;
		private Status status;

		public GetFdForAssetResultImpl(Status status, ParcelFileDescriptor fd) {
			this.status = status;
			this.fd = fd;
		}

		public InputStream getInputStream() {
			Log.d(TAG, "getInputStream...");
			if (this.fd == null) {
				return null;
			}
			ParcelFileDescriptor.AutoCloseInputStream autoCloseInputStream = new ParcelFileDescriptor.AutoCloseInputStream(
					this.fd);
			return autoCloseInputStream;
		}

		public Status getStatus() {
			return this.status;
		}
	}

}