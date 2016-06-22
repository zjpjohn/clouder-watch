package com.cms.android.wearable.service.codec;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.net.Uri;
import android.os.Bundle;

import com.cms.android.wearable.Asset;
import com.cms.android.wearable.service.common.LogTool;

public class DataInfo extends Parser {

	public DataInfo() {
		super();
	}

	public DataInfo(int versionCode, Uri uri, Bundle assets, byte[] data, String deviceId, long timeStamp,
			String packageName) {
		super();
		this.versionCode = versionCode;
		this.uri = uri;
		this.assets = assets;
		this.data = data;
		this.deviceId = deviceId;
		this.timeStamp = timeStamp;
		this.packageName = packageName;
		LogTool.d("spencer", toString());
	}

	private int versionCode;

	private Uri uri;

	private Bundle assets;

	private byte[] data;

	private String deviceId;

	private long timeStamp;

	private String packageName;

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public int getVersionCode() {
		return versionCode;
	}

	public void setVersionCode(int versionCode) {
		this.versionCode = versionCode;
	}

	public Uri getUri() {
		return uri;
	}

	public void setUri(Uri uri) {
		this.uri = uri;
	}

	public Bundle getAssets() {
		return assets;
	}

	public void setAssets(Bundle assets) {
		this.assets = assets;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public void setTimeStamp(byte[] timeStamp) {
		this.timeStamp = (0xffL & (long) timeStamp[0]) | (0xff00L & ((long) timeStamp[1] << 8))
				| (0xff0000L & ((long) timeStamp[2] << 16)) | (0xff000000L & ((long) timeStamp[3] << 24))
				| (0xff00000000L & ((long) timeStamp[4] << 32)) | (0xff0000000000L & ((long) timeStamp[5] << 40));
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	@Override
	public int getType() {
		return TYPE_DATA;
	}

	public Map<String, Asset> getAssetsMap() {
		HashMap<String, Asset> map = new HashMap<String, Asset>();
		Iterator<String> iterator = this.assets.keySet().iterator();
		while (iterator.hasNext()) {
			String str = (String) iterator.next();
			map.put(str, (Asset) this.assets.getParcelable(str));
		}
		return Collections.unmodifiableMap(map);
	}

	public void setAssetsMap(Map<String, Asset> map) {
		this.assets = new Bundle();
		Iterator<String> iterator = map.keySet().iterator();
		while (iterator.hasNext()) {
			String str = (String) iterator.next();
			this.assets.putParcelable(str, map.get(str));
		}
	}

	@Override
	public String toString() {
		return "DataInfo [versionCode=" + versionCode + ", uri=" + uri + ", assets=" + assets + ", data="
				+ Arrays.toString(data) + ", deviceId=" + deviceId + ", timeStamp=" + timeStamp + ", packageName="
				+ packageName + "]";
	}
}
