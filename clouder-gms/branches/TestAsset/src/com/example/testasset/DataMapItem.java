package com.example.testasset;

import java.util.Iterator;

import android.net.Uri;
import android.util.Log;

public class DataMapItem {
	public static final String DATA = "data";
	public static final String ASSET_KEYS = "assetKeys";
	public static final String ASSET_VALUES = "assetValues";
	private Uri mUri;
	private DataMap mDataMap;

	public static DataMapItem fromDataItem(DataItem dataItem) {
		if (dataItem == null)
			throw new IllegalStateException("unexpected null dataItem.");
		return new DataMapItem(dataItem);
	}

	private DataMapItem(DataItem dataItem) {
		this.mUri = dataItem.getUri();
		this.mDataMap = create((DataItem) dataItem.freeze());
	}

	public Uri getUri() {
		return this.mUri;
	}

	public DataMap getDataMap() {
		return this.mDataMap;
	}

	private DataMap create(DataItem dataItem) {
		if ((dataItem.getData() == null) && (dataItem.getAssets().size() > 0))
			throw new IllegalArgumentException("Cannot create DataMapItem from an empty DataItem.");
		if (dataItem.getData() == null)
			return new DataMap();
		try {
			byte[] data = dataItem.getData();
			DataMap dataMap = DataMap.fromByteArray(data);
			if (dataItem.getAssets() != null) {
				Iterator<String> iterator = dataItem.getAssets().keySet().iterator();
				while (iterator.hasNext()) {
					String key = (String) iterator.next();
					DataItemAsset dataItemAsset = (DataItemAsset) dataItem.getAssets().get(key);
					String[] pathArray = key.split("_@_");
					DataMap localDataMap2 = dataMap;
					for (int i = 0; i < pathArray.length - 1; i++) {
						String str2 = pathArray[(i + 1)];
						String[] indexArray = str2.split("_#_");
						if (indexArray.length == 1) {
							localDataMap2 = localDataMap2.getDataMap(pathArray[i]);
						} else {
							int j = Integer.parseInt(indexArray[0]);
							localDataMap2 = (DataMap) localDataMap2.getDataMapArrayList(pathArray[i]).get(j);
							pathArray[(i + 1)] = indexArray[1];
						}
					}
					localDataMap2.putAsset(pathArray[(pathArray.length - 1)],
							Asset.createFromRef(dataItemAsset.getId()));
				}
			}
			return dataMap;
		} catch (Exception e) {
			Log.e("DataMapItem", "parse a DataItem failed.", e);
			throw new IllegalStateException("parse a DataItem failed.", e);
		}
	}
}