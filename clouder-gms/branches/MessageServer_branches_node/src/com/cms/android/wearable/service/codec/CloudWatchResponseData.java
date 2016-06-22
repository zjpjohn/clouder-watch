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


/**
 * 
 * ClassName: LinkResponseData
 * 
 * @description
 * @author hu_wg
 * @Date Jan 15, 2013
 * 
 */
public class CloudWatchResponseData  {
	private byte[] deviceNo;
	private byte[] command;
	private byte[] data;
	private byte[] timeStamp;
	private byte[] packageName;
	private byte[] path;

	public CloudWatchResponseData() {
		super();
	}

	public CloudWatchResponseData(byte[] deviceNo, byte[] command, byte[] data, byte[] timeStamp, byte[] packageName, byte[] path) {
		super();
		this.deviceNo = deviceNo;
		this.command = command;
		this.data = data;
		this.timeStamp = timeStamp;
		this.packageName = packageName;
		this.path = path;
	}
	/**
	 * @return the deviceNo
	 */
	public byte[] getDeviceId() {
		return deviceNo;
	}

	/**
	 * @param deviceNo
	 *            the deviceNo to set
	 */
	public void setDeviceId(byte[] deviceNo) {
		this.deviceNo = deviceNo;
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
	
	/**
	 * @return the timeStamp
	 */
	public byte[] getTimeStamp() {
		return timeStamp;
	}

	/**
	 * @param timeStamp
	 *            the timeStamp to set
	 */
	public void setTimeStamp(byte[] timeStamp) {
		this.timeStamp = timeStamp;
	}

	/**
	 * @return the packageName
	 */
	public byte[] getPackageName() {
		return packageName;
	}

	/**
	 * @param packageName
	 *            the packageName to set
	 */
	public void setPackageName(byte[] packageName) {
		this.packageName = packageName;
	}

	/**
	 * @return the path
	 */
	public byte[] getPath() {
		return path;
	}

	/**
	 * @param path
	 *            the path to set
	 */
	public void setPath(byte[] path) {
		this.path = path;
	}
}
