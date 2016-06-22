package com.cms.android.wearable;

import java.util.Arrays;
import java.util.Iterator;

import android.net.Uri;

import com.cms.android.wearable.service.common.LogTool;

public class DataMapItem {
	public static final String DATA = "data";
	public static final String ASSET_KEYS = "assetKeys";
	public static final String ASSET_VALUES = "assetValues";
	private final Uri mUri;
	private final DataMap mDataMap;

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
					LogTool.e("spencer", "key = " + key + " pathArray = " + Arrays.toString(pathArray));
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
					LogTool.e("spencer", "putAsset -> " + pathArray[(pathArray.length - 1)] + " " + dataItemAsset.getId());
					localDataMap2.putAsset(pathArray[(pathArray.length - 1)],
							Asset.createFromRef(dataItemAsset.getId()));
				}
			}
			return dataMap;
		} catch (Exception e) {
			LogTool.e("DataMapItem", "parse a DataItem failed.", e);
			throw new IllegalStateException("parse a DataItem failed.", e);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("DataMapItem[");
		sb.append("@");
		sb.append(Integer.toHexString(hashCode()));
		sb.append(",mUri=");
		Object localObject = null;
		if (this.mUri == null) {
			localObject = "null";
		} else {
			localObject = this.mUri;
		}
		sb.append(localObject);
		sb.append("]\n  mDataMap: ");
		Iterator<String> iterator = this.mDataMap.keySet().iterator();
		while (iterator.hasNext()) {
			String str = (String) iterator.next();
			sb.append("\n    " + str + ": " + this.mDataMap.get(str));
		}
		sb.append("\n  ]");
		return sb.toString();
	}

}