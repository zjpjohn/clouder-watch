package com.cms.android.wearable.service.codec;

import java.util.UUID;

import com.cms.android.wearable.service.impl.BLECentralService.MappedInfo;

public class TransportData implements Comparable<TransportData> {

	public static final byte PROTOCOL_MESSAGE_TYPE = 1;

	public static final byte PROTOCOL_DATA_TYPE = 2;

	public static final byte PROTOCOL_RESPONSE_TYPE = 3;

	private String packageName;

	private String uri;

	private String id;

	private String uuid;

	private byte protocolType;

	private MappedInfo mappedInfo;

	private long contentLength;

	private int index;

	private int readLength;

	private long count;

	private int packIndex;

	public TransportData() {
		super();
		this.id = UUID.randomUUID().toString();
	}

	public TransportData(String packageName, String uri, String uuid, byte protocolType, MappedInfo mappedInfo,
			long contentLength, int index, int readLength, long count, int packIndex) {
		super();
		this.packageName = packageName;
		this.uri = uri;
		this.id = UUID.randomUUID().toString();
		this.uuid = uuid;
		this.protocolType = protocolType;
		this.mappedInfo = mappedInfo;
		this.contentLength = contentLength;
		this.index = index;
		this.readLength = readLength;
		this.count = count;
		this.packIndex = packIndex;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public byte getProtocolType() {
		return protocolType;
	}

	public void setProtocolType(byte protocolType) {
		this.protocolType = protocolType;
	}

	public long getContentLength() {
		return contentLength;
	}

	public void setContentLength(long contentLength) {
		this.contentLength = contentLength;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int getReadLength() {
		return readLength;
	}

	public void setReadLength(int readLength) {
		this.readLength = readLength;
	}

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

	public int getPackIndex() {
		return packIndex;
	}

	public void setPackIndex(int packIndex) {
		this.packIndex = packIndex;
	}

	public MappedInfo getMappedInfo() {
		return mappedInfo;
	}

	public void setMappedInfo(MappedInfo mappedInfo) {
		this.mappedInfo = mappedInfo;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof TransportData && this.id.equals(((TransportData) o).id);
	}

	@Override
	public int compareTo(TransportData another) {
		return this.packIndex > another.packIndex ? 1 : this.packIndex < another.packIndex ? -1 : 0;
	}

	@Override
	public String toString() {
		return "TransportData [packageName=" + packageName + ", uri=" + uri + ", id=" + id + ", uuid=" + uuid
				+ ", protocolType=" + translateProtocolType(protocolType) + ", mappedInfo=" + mappedInfo
				+ ", contentLength=" + contentLength + ", index=" + index + ", readLength=" + readLength + ", count="
				+ count + ", packIndex=" + packIndex + "]";
	}

	private String translateProtocolType(int type) {
		String protocolTypeName = "";
		switch (type) {
		case PROTOCOL_MESSAGE_TYPE:
			protocolTypeName = "message";
			break;
		case PROTOCOL_DATA_TYPE:
			protocolTypeName = "data";
			break;
		case PROTOCOL_RESPONSE_TYPE:
			protocolTypeName = "response";
			break;

		default:
			break;
		}
		return protocolTypeName;
	}

}
