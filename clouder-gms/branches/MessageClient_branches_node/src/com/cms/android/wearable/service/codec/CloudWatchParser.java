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

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;

import android.util.Log;

/**
 * 
 * ClassName: LinkParser
 * 
 * @description
 * @author hu_wg
 * @Date Jan 15, 2013
 * 
 */
public class CloudWatchParser {
	private final static String KEY_SPLIT = " ";
	public final static byte KEY_REQUEST_START = 64;// @
	public final static byte KEY_RESPONSE_START = 36;// $
	public final static byte KEY_END_0 = 0x0d;// \r
	public final static byte KEY_END_1 = 0x0a;// \n

	public static byte[] dataPack(CloudWatchRequestData dataObj, boolean isRequest) {

		byte[] deviceNo = dataObj.getDeviceId();
		byte[] command = dataObj.getCommand();
		byte[] data = dataObj.getData();
		byte[] timeStamp = dataObj.getTimeStamp();
		byte[] packageName = dataObj.getPackageName();
		byte[] path = dataObj.getPath();
		byte[] buf = new byte[40 + data.length + packageName.length + path.length];
		buf[0] = isRequest ? KEY_REQUEST_START : KEY_RESPONSE_START;
		buf[1] = isRequest ? KEY_REQUEST_START : KEY_RESPONSE_START;
		buf[2] = (byte) ((40 + data.length + packageName.length + path.length) & 0xFF); // L1
		buf[3] = (byte) (((40 + data.length + packageName.length + path.length) >> 8) & 0xFF); // L2
		buf[4] = (byte) (((40 + data.length + packageName.length + path.length) >> 16) & 0xFF); // L3
		buf[5] = (byte) (((40 + data.length + packageName.length + path.length) >> 24) & 0xFF); // L4
		// copy deviceNo
		System.arraycopy(deviceNo, 0, buf, 6, deviceNo.length);
		buf[26] = command[0]; // command type
		buf[27] = command[1]; // command sub type
		// copy timeStamp
		System.arraycopy(timeStamp, 0, buf, 28, timeStamp.length);
		buf[34] = (byte) packageName.length; // packageName length
		// copy packageName
		System.arraycopy(packageName, 0, buf, 35, packageName.length);
		buf[35 + packageName.length] = (byte) path.length; // path length
		// copy path
		System.arraycopy(path, 0, buf, 36 + packageName.length, path.length);
		// copy data
		System.arraycopy(data, 0, buf, 36 + packageName.length + path.length, data.length);
		byte[] tmp = new byte[36 + data.length + packageName.length + path.length];
		System.arraycopy(buf, 0, tmp, 0, 36 + data.length + packageName.length + path.length);
		byte[] crc = CRCUtil.makeCrcToBytes(tmp);
		buf[36 + data.length + packageName.length + path.length] = crc[1]; // L
		buf[37 + data.length + packageName.length + path.length] = crc[0]; // H
		buf[38 + data.length + packageName.length + path.length] = KEY_END_0; // \r
		buf[39 + data.length + packageName.length + path.length] = KEY_END_1; // \n

		return buf;
	}

	// Header Length Device ID Type Data Checksum Tail
	// A B C D E F G
	// A Header, 2 bytes, defined as @@, ASCII code
	// B Length, 2 bytes, indicates the data length from Header to Tail
	// ([Header, Tail)), statistical length range is
	// A,B,C,D,E,F,G
	// C Device ID (LinkII product ID), 20 bytes, ASCII bytes. If length < 20
	// bytes, filled up with ‘\0’.
	// D Type, 2 bytes, high byte represents to main identifier and low byte
	// represent to sub identifier.
	// E Data, random length.
	// F Checksum, 2 bytes, calculate from Header to Data excluding Checksum and
	// Tail. Checksum algorithm is
	// CRC Checksum Algorithm (see appendix), calculating range is A,B,C,D,E
	// G Tail, 2 bytes, defined as “\r\n”.
	public static byte[] dataPack(CloudWatchRequestData dataObj) {
		return dataPack((CloudWatchRequestData) dataObj, true);
	}

	// Header Length Device ID Type Data Checksum Tail
	// A B C D E F G
	// A Header, 2 bytes, defined as $$, ASCII code.
	// B Length, 2 bytes, indicates the data length from Header to Tail
	// ([Header, Tail)), statistical length range is
	// A,B,C,D,E,F,G
	// C Device ID (LinkII product ID), 20 bytes, ASCII bytes. If length is < 20
	// bytes, filled up with ‘\0’.
	// D Type, 2 bytes, high byte represents to main identifier and low byte
	// represents to sub identifier.
	// E Data, random length.
	// F Checksum, 2 bytes, calculate from Header to Data excluding Checksum and
	// Tail. Checksum algorithm is
	// CRC Checksum Algorithm (see appendix), calculation range is A,B,C,D,E.
	// G Tail, 2 bytes, defined as “\r\n”.
	// 24 24
	// 20 00
	// 77 6b 68 68 6e 6a 36 31 30 30 31 32 34 35 30 30 33 36 34
	// 00
	// 22 08
	// 14 10 0c 12
	// 0d 0a
	public static CloudWatchResponseData dataUnpack(byte[] dataPack) {
		CloudWatchResponseData dataObj = null;

		try {
			// check data start and end when read byte[] from io.
			if (checkDataPack(dataPack)) {
				dataObj = new CloudWatchResponseData();
				byte[] deviceNo = new byte[20];
				System.arraycopy(dataPack, 6, deviceNo, 0, 20);
				byte[] command = new byte[2];
				command[0] = dataPack[26];
				command[1] = dataPack[27];
				byte[] timeStamp = new byte[6];
				System.arraycopy(dataPack, 28, timeStamp, 0, 6);
				int packageLength = dataPack[34];
				byte[] packgeName = new byte[packageLength];
				System.arraycopy(dataPack, 35, packgeName, 0, packageLength);
				int pathLength = dataPack[35 + packageLength];
				byte[] path = new byte[pathLength];
				System.arraycopy(dataPack, 36 + packageLength, path, 0, pathLength);
				byte[] data = new byte[dataPack.length - packageLength - pathLength - 40];
				if (dataPack.length - packageLength - 40 > 0) {
					System.arraycopy(dataPack, 36 + packageLength + pathLength, data, 0, dataPack.length
							- packageLength - pathLength - 40);
				}
				dataObj.setCommand(command);
				dataObj.setData(data);
				dataObj.setDeviceId(deviceNo);
				dataObj.setTimeStamp(timeStamp);
				dataObj.setPackageName(packgeName);
				dataObj.setPath(path);
			} else {
				Log.e("", "Response data is invalid.");
			}
		} catch (Exception e) {
			Log.e("", e.getMessage(), e);
		}
		return dataObj;
	}

	public static boolean checkDataPack(byte[] dataPack) {
		// check is valid pack
		if (dataPack.length < 40) {
			// error data pack
			return false;
		}

		// check pack length
		byte l0 = dataPack[2];
		byte l1 = dataPack[3];
		byte l2 = dataPack[4];
		byte l3 = dataPack[5];
		int length = ((l3 << 24) & 0xFF000000) | ((l2 << 16) & 0xFF0000) | ((l1 << 8) & 0xFF00) | l0 & 0xFF;
		if (dataPack.length != length) {
			// error data pack length
			return false;
		}

		// check crc
		byte[] abcde = new byte[dataPack.length - 4];
		System.arraycopy(dataPack, 0, abcde, 0, dataPack.length - 4);
		byte crc0 = dataPack[dataPack.length - 4];
		byte crc1 = dataPack[dataPack.length - 3];
		byte[] newCrc = CRCUtil.makeCrcToBytes(abcde);
		if (newCrc[0] != crc1 || newCrc[1] != crc0) {
			// error crc
			return false;
		}

		return true;
	}

	public static String generateKey(byte[] command, byte[] data) {
		StringBuffer sb = new StringBuffer();
		for (byte b : command) {
			String strData = Integer.toHexString(b);
			strData = strData.length() == 1 ? "0" + strData : strData;
			sb.append(strData).append(KEY_SPLIT);
		}
		for (byte b : data) {
			String strData = Integer.toHexString(b);
			strData = strData.length() == 1 ? "0" + strData : strData;
			sb.append(strData).append(KEY_SPLIT);
		}
		return sb.toString();
	}

	public static void main(String[] args) {
		byte[] data = new byte[] { 36, 36, 30, 2, 119, 107, 104, 104, 110, 106, 54, 49, 48, 48, 49, 50, 52, 53, 48, 48,
				51, 54, 52, 0, 32, 4, 61, 0, 0, 0, 0, 1, 0, 0, 82, -79, 8, 81, 41, 9, 0, 0, 1, 2, 1, 6, 33, 13, 33, 12,
				33, 5, 33, 16, 33, 15, 33, 49, 9, 6, 30, 1, 13, 5, 36, 19, 102, 95, -36, 6, -32, 84, 123, 25, 0, 0, 73,
				3, -65, 3, 10, 65, 125, 0, -92, 66, 22, -41, 3, 9, -1, 125, 0, -94, 66, 22, -41, 0, 10, 55, 125, 0,
				-94, 66, 22, -41, 0, 7, -108, 125, 0, -74, 66, 22, -41, 0, 10, 99, 125, 1, -39, 66, 22, -41, 6, 0, 52,
				5, 1, 51, 5, 0, 53, 8, 1, 52, 12, 2, 52, 30, 1, 13, 5, 36, 29, -118, 101, -36, 6, -84, 85, 123, 25,
				-120, 3, -16, 13, -65, 8, 26, 62, 125, 1, 72, 65, 22, -41, 12, 16, 122, 125, 3, -17, 65, 22, -41, 19,
				25, -10, 125, 5, -5, 63, 22, -41, 25, 21, 2, 125, 7, 122, 63, 22, -41, 29, 26, -125, 125, 7, 125, 62,
				22, -41, 9, 5, 46, 10, 7, 50, 12, 1, 51, 10, 1, 53, 12, -1, 55, 30, 1, 13, 5, 36, 39, 72, 116, -36, 6,
				22, 85, 123, 25, -12, 4, 57, 0, -65, 35, 22, -79, 126, 1, 19, 62, 22, -41, 37, 24, 6, 126, 6, -60, 62,
				22, -41, 43, 26, -91, 126, 6, 81, 61, 22, -41, 47, 28, 66, 126, 1, -48, 61, 22, -41, 47, 22, 71, 127,
				3, -9, 62, 22, -41, 5, 0, 54, 8, -2, 47, 8, -3, 52, 5, -5, 54, 6, -4, 54, 30, 1, 13, 5, 36, 49, 54,
				-125, -36, 6, 60, 92, 123, 25, 7, 5, 58, 1, -65, 44, 22, -23, 127, 4, -127, 61, 22, -41, 46, 23, 52,
				127, 4, 55, 61, 22, -41, 47, 23, 105, -128, 4, 88, 61, 22, -41, 46, 23, -94, -128, 3, 83, 61, 22, -41,
				46, 22, -57, -128, 1, 19, 62, 22, -41, 9, -8, 53, 8, -7, 50, 9, -7, 53, 2, 6, 48, 3, 0, 53, 30, 1, 13,
				5, 36, 59, -94, -116, -36, 6, -2, 93, 123, 25, -65, 1, -111, 11, -65, 44, 20, -8, -128, 0, -30, 63, 22,
				-41, 38, 14, 75, -128, 0, -55, 63, 22, -41, 30, 11, 21, -128, 0, -66, 63, 22, -41, 23, 11, -76, -128,
				0, -72, 64, 22, -41, 18, 11, -110, -128, 0, -69, 64, 22, -41, 4, 6, 54, 0, 9, 54, 6, 7, 52, -1, 8, 51,
				2, 7, 54, 30, 1, 13, 5, 37, 9, -68, -118, -36, 6, -96, 88, 123, 25, -120, 1, -108, 9, -65, 15, 12, -85,
				-128, 2, -54, 63, 22, -41, 17, 14, 100, -128, 0, -61, 63, 22, -41, 16, 13, 78, -128, 0, -63, 64, 22,
				-41, 16, 12, -128, -128, 1, 109, 63, 22, -41, 15, 12, 28, -128, 0, -77, 63, 22, -41, 13, -6, 52, 4, 5,
				52, 4, 9, 57, 10, -13, 49, 4, -9, 54, 0, 0, 0, 0 };
		// byte[] data = null;
		// try
		// {
		// File file = new File("D://watch_firmware.zip");
		// FileInputStream fis = new FileInputStream(file);
		// ByteArrayOutputStream bos = new ByteArrayOutputStream();
		// byte[] b = new byte[1024];
		// int n;
		// while ((n = fis.read(b)) != -1)
		// {
		// bos.write(b, 0, n);
		// }
		// fis.close();
		// bos.close();
		// data = bos.toByteArray();
		// }
		// catch (FileNotFoundException e)
		// {
		// e.printStackTrace();
		// }
		// catch (IOException e)
		// {
		// e.printStackTrace();
		// }

		System.out.println(Arrays.toString(data));
		System.out.println();

		String packageName = "com.hoperun.watch";
		String path = "/test";
		CloudWatchRequestData request = new CloudWatchRequestData("12345678901234567890".getBytes(Charset
				.forName("utf-8")), new byte[] { 1, 0 }, data, ByteUtil.getByteArrayByLong(new Date().getTime(), 6),
				packageName.getBytes(), path.getBytes());

		byte[] requestByte = dataPack(request);
		CloudWatchResponseData response = dataUnpack(requestByte);
		System.out.println(new String(response.getDeviceId()));
		System.out.println(new String(response.getPackageName()));
		System.out.println(new String(response.getPath()));

		byte[] bytes = response.getTimeStamp();
		long time = (0xffL & (long) bytes[0]) | (0xff00L & ((long) bytes[1] << 8))
				| (0xff0000L & ((long) bytes[2] << 16)) | (0xff000000L & ((long) bytes[3] << 24))
				| (0xff00000000L & ((long) bytes[4] << 32)) | (0xff0000000000L & ((long) bytes[5] << 40));
		System.out.println(new Date(time));
		System.out.println();
		System.out.println(Arrays.toString(response.getData()));
	}
}
