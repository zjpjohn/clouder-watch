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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

import com.cms.android.wearable.service.common.LogTool;

/**
 * ClassName: FileParser
 * 
 * @description
 * @author xing_peng
 * @Date 2015-7-28
 * 
 */
public class FileParser {

	public static byte[] dataPack(FileData dataObj) {

		byte[] fileLength = dataObj.getFileLength();
		byte[] fileName = dataObj.getFileName();
		byte[] data = dataObj.getData();
		byte[] buf = new byte[5 + fileName.length + data.length];

		// copy fileLength
		System.arraycopy(fileLength, 0, buf, 0, fileLength.length);
		// fileName length
		buf[4] = (byte) fileName.length;
		// copy fileName
		System.arraycopy(fileName, 0, buf, 5, fileName.length);
		// copy data
		System.arraycopy(data, 0, buf, 5 + fileName.length, data.length);

		return buf;
	}

	public static FileData dataUnpack(byte[] dataPack) {
		FileData dataObj = null;

		try {
			dataObj = new FileData();

			// copy fileLength
			byte[] fileLength = new byte[4];
			System.arraycopy(dataPack, 0, fileLength, 0, 4);
			// fileName length
			int fileNameLength = dataPack[4];
			// copy fileName
			byte[] fileName = new byte[fileNameLength];
			System.arraycopy(dataPack, 5, fileName, 0, fileNameLength);
			// copy data
			byte[] data = new byte[dataPack.length - fileNameLength - 5];
			System.arraycopy(dataPack, 5 + fileNameLength, data, 0, dataPack.length - fileNameLength - 5);

			dataObj.setFileLength(fileLength);
			dataObj.setFileName(fileName);
			dataObj.setData(data);

		} catch (Exception e) {
			LogTool.e("", e.getMessage(), e);
		}

		return dataObj;
	}
}
