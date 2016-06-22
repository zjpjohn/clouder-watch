package com.example.testasset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.net.Uri;
import android.util.Log;

public class PutDataMapRequest {

	private PutDataRequest request;

	private DataMap dataMap;

	private PutDataMapRequest(PutDataRequest request, DataMap dataMap) {
		this.request = request;
		this.dataMap = new DataMap();
		if (dataMap != null) {
			this.dataMap.putAll(dataMap);
		}
	}

	public static PutDataMapRequest createFromDataMapItem(DataMapItem dataMapItem) {
		return new PutDataMapRequest(PutDataRequest.createFromUri(dataMapItem.getUri()), dataMapItem.getDataMap());
	}

	public static PutDataMapRequest createWithAutoAppendedId(String appendedId) {
		return new PutDataMapRequest(PutDataRequest.createWithAutoAppendedId(appendedId), null);
	}

	public static PutDataMapRequest createFromUri(Uri paramUri) {
		return new PutDataMapRequest(PutDataRequest.createFromUri(paramUri), null);
	}

	public static PutDataMapRequest create(String uri) {
		return new PutDataMapRequest(PutDataRequest.create(uri), null);
	}

	public Uri getUri() {
		return this.request.getUri();
	}

	public DataMap getDataMap() {
		return this.dataMap;
	}

	@SuppressWarnings("rawtypes")
	private static void resolveMap(DataMap dataMap, Map<String, Asset> map, String prefix) {
		ArrayList<String> prefixList = new ArrayList<String>();
		Iterator<String> iterator = dataMap.keySet().iterator();
		String str;
		while (iterator.hasNext()) {
			str = iterator.next();
			Object obj = dataMap.get(str);
			if ((obj instanceof Asset)) {
				map.put(prefix + str, (Asset) obj);
				prefixList.add(str);
				Log.d("spencer", "resolve Asset");
			} else if ((obj instanceof DataMap)) {
				resolveMap((DataMap) obj, map, prefix + str + "_@_");
				Log.d("spencer", "resolve DataMap");
			} else if ((obj instanceof List)) {
				List list = (List) obj;
				for (int i = 0; i < list.size(); i++) {
					if (!(list.get(i) instanceof DataMap))
						continue;
					resolveMap((DataMap) list.get(i), map, prefix + str + "_@_" + i + "_#_");
				}
				Log.d("spencer", "resolve List");
			}
		}
		iterator = prefixList.iterator();
		while (iterator.hasNext()) {
			str = (String) iterator.next();
			dataMap.remove(str);
		}
	}

	public PutDataRequest asPutDataRequest() {
		HashMap<String, Asset> hashMap = new HashMap<String, Asset>();
		resolveMap(this.dataMap, hashMap, "");
		byte[] data = this.dataMap.toByteArray();
		Log.d("spencer", "data size = " + (data == null ? 0 : data.length) + " hashMap size = " + hashMap.size());
		this.request.setData(data);
		Iterator<String> iterator = hashMap.keySet().iterator();
		while (iterator.hasNext()) {
			String str = (String) iterator.next();
			this.request.putAsset(str, hashMap.get(str));
		}
		return this.request;
	}
}