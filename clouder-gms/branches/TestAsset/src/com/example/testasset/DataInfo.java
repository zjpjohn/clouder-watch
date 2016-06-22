package com.example.testasset;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.net.Uri;
import android.os.Bundle;

public class DataInfo extends Parser {

	public DataInfo() {
		super();
	}

	private int versionCode;

	private Uri uri;

	private Bundle assets;

	private byte[] data;

	private String deviceId;
	
	private long timeStamp;
	
	private int protocolType;

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
	
	public int getProtocolType() {
		return protocolType;
	}

	public void setProtocolType(int protocolType) {
		this.protocolType = protocolType;
	}

	@Override
	public byte[] parse() {
		return DataInfoParser.dataPack(this);
	}

	@Override
	public void callback() {

	}

	@Override
	public int getType() {
		return TYPE_DATA;
	}

	public Map<String, Asset> getAssetsMap() {
		HashMap<String, Asset> map = new HashMap<String, Asset>();
		Iterator<String> localIterator = this.assets.keySet().iterator();
		while (localIterator.hasNext()) {
			String str = (String) localIterator.next();
			map.put(str, (Asset) this.assets.getParcelable(str));
		}
		return Collections.unmodifiableMap(map);
	}
	
	public void setAssetsMap(Map<String, Asset> map) {
		this.assets = new Bundle();
		Iterator<String> localIterator = map.keySet().iterator();
		while (localIterator.hasNext()) {
			String str = (String) localIterator.next();
			this.assets.putParcelable(str, map.get(str));
		}	
	}
}
