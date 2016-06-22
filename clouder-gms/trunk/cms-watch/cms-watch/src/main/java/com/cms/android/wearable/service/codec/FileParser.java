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

	public static void main(String[] args) {

		byte[] data = null;
		String fileName = null;
		int fileLength = 0;
		try {
			File file = new File("D://watch_firmware.zip");
			FileInputStream fis = new FileInputStream(file);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] b = new byte[1024];
			int n;
			while ((n = fis.read(b)) != -1) {
				bos.write(b, 0, n);
			}
			fis.close();
			bos.close();
			data = bos.toByteArray();

			fileName = file.getName();
			fileLength = (int) file.length();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// 封包
		FileData fileData = new FileData(fileName.getBytes(), ByteUtil.getByteArrayByLong(fileLength, 4), data);

		String packageName = "com.hoperun.watch";
		String nodeId = "node1";
		MessageData request = new MessageData("12345678901234567890", new byte[] { 1, 0 }, dataPack(fileData),
				new Date().getTime(), packageName, "/test", nodeId);

//		byte[] requestByte = MessageParser.dataPack(request);
//
//		// 解包
//		MessageData response = MessageParser.dataUnpack(requestByte);
//		System.out.println(response.getDeviceNo());
//		System.out.println(response.getPackageName());
//
//		long time = response.getTimeStamp();
//		// long time = (0xffL & (long) bytes[0]) | (0xff00L & ((long) bytes[1]
//		// << 8))
//		// | (0xff0000L & ((long) bytes[2] << 16)) | (0xff000000L & ((long)
//		// bytes[3] << 24))
//		// | (0xff00000000L & ((long) bytes[4] << 32)) | (0xff0000000000L &
//		// ((long) bytes[5] << 40));
//		System.out.println(new Date(time));
//
//		FileData dataPack = dataUnpack(response.getData());
//		System.out.println(new String(dataPack.getFileName()));
//		byte[] bytes2 = dataPack.getFileLength();
//		System.out.println((0xffL & (long) bytes2[0]) | (0xff00L & ((long) bytes2[1] << 8))
//				| (0xff0000L & ((long) bytes2[2] << 16)) | (0xff000000L & ((long) bytes2[3] << 24)));
//
//		System.out.println(Arrays.equals(data, dataPack.getData()));
	}
}
