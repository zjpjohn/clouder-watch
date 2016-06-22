/*****************************************************************************
 *
 *                      HOPERUN PROPRIETARY INFORMATION
 *
 *          The information contained herein is proprietary to HopeRun
 *           and shall not be reproduced or disclosed in whole or in part
 *                    or used for any design or manufacture
 *              without direct written authorization from HopeRun.
 *
 *            Copyright (c) 2014 by HopeRun.  All rights reserved.
 *
 *****************************************************************************/
package com.cms.android.wearable.service.codec;

import java.util.Arrays;

/**
 * ClassName: FileData
 *
 * @description
 * @author xing_peng
 * @Date 2015-7-28
 * 
 */
public class FileData {

	byte[] fileName;

	byte[] fileLength;

	byte[] data;

	public FileData() {
		super();
	}

	public FileData(byte[] fileName, byte[] fileLength, byte[] data) {
		super();
		this.fileName = fileName;
		this.fileLength = fileLength;
		this.data = data;
	}
	
	
	/**
	 * @return the fileName
	 */
	public byte[] getFileName() {
		return fileName;
	}

	/**
	 * @param fileName
	 *            the fileName to set
	 */
	public void setFileName(byte[] fileName) {
		this.fileName = fileName;
	}

	/**
	 * @return the fileLength
	 */
	public byte[] getFileLength() {
		return fileLength;
	}

	/**
	 * @param fileLength
	 *            the fileLength to set
	 */
	public void setFileLength(byte[] fileLength) {
		this.fileLength = fileLength;
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
		return "FileData [fileName=" + Arrays.toString(fileName) + ", fileLength=" + Arrays.toString(fileLength)
				+ ", data=" + Arrays.toString(data) + "]";
	}

}
