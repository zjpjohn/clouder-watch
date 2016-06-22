/*****************************************************************************
 *
 *                      HOPERUN PROPRIETARY INFORMATION
 *
 *          The information contained herein is proprietary to HopeRun
 *           and shall not be reproduced or disclosed in whole or in part
 *                    or used for any design or manufacture
 *              without direct written authorization from HopeRun.
 *
 *            Copyright (c) 2013 by HopeRun.  All rights reserved.
 *
 *****************************************************************************/
package com.cms.android.wearable.service.codec;

import java.util.Arrays;

/**
 * 
 * ClassName: LinkRequestData
 * 
 * @description
 * @author hu_wg
 * @Date Jan 15, 2013
 * 
 */
public class MessageData extends Parser {

	private String deviceNo;
	private byte[] command;
	private byte[] data;
	private long timeStamp;
	private String packageName;
	private String path;
	private String nodeId;

	public MessageData() {
		super();
	}

	public MessageData(String deviceNo, byte[] data, long timeStamp, String packageName, String path, String nodeId) {
		super();
		this.deviceNo = deviceNo;
		this.data = data;
		this.timeStamp = timeStamp;
		this.packageName = packageName;
		this.path = path;
		this.nodeId = nodeId;
	}

	public MessageData(String deviceNo, byte[] command, byte[] data, long timeStamp, String packageName, String path,
			String nodeId) {
		super();
		this.deviceNo = deviceNo;
		this.command = command;
		this.data = data;
		this.timeStamp = timeStamp;
		this.packageName = packageName;
		this.path = path;
		this.nodeId = nodeId;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getDeviceNo() {
		return deviceNo;
	}

	public void setDeviceNo(String deviceNo) {
		this.deviceNo = deviceNo;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * @return the command
	 */
	public byte[] getCommand() {
		return command;
	}

	/**
	 * @param command
	 *            the command to set
	 */
	public void setCommand(byte[] command) {
		this.command = command;
	}

	/**
	 * @return the data
	 */
	public byte[] getData() {
		return data;
	}

	/**
	 * @param data
	 *            the data to set
	 */
	public void setData(byte[] data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "MessageData [deviceNo=" + deviceNo + ", uuid=" + uuid + " command=" + Arrays.toString(command)
				+ ", data=" + Arrays.toString(data) + ", timeStamp=" + timeStamp + ", packageName=" + packageName
				+ ", path=" + path + ", nodeId=" + nodeId + "]";
	}

	@Override
	public int getType() {
		return TYPE_MESSAGE;
	}

}
