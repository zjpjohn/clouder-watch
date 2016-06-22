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
import java.util.UUID;

import com.cms.android.wearable.service.common.LogTool;

/**
 * ClassName: ResponseParser
 *
 * @description
 * @author xing_peng
 * @Date 2015-8-20
 * 
 */
public class ResponseParser {

	public final static byte RESPONSE_PARSER_TYPE = TransportParser.RESPONSE_PARSER_TYPE;
	
	public final static byte KEY_RESPONSE_START = 38;// &
	public final static byte KEY_END_0 = 0x0d;// \r
	public final static byte KEY_END_1 = 0x0a;// \n

	// Header UUID status Tail
	// A B C D
	// A Header, 2 bytes, defined as @@, ASCII code
	// B UUID, 36 bytes
	// C Status, 1 bytes
	// D Checksum, 2 bytes, calculate from Header to Data excluding Checksum and
	// Tail. Checksum algorithm is
	// CRC Checksum Algorithm (see appendix), calculating range is A,B,C
	// E Tail, 2 bytes, defined as “\r\n”.
	public static byte[] dataPack(ResponseData responseData) {

		byte[] uuid = responseData.getUuid().getBytes();
		byte status = (byte) responseData.getStatus();

		byte[] buf = new byte[48];
		buf[0] = KEY_RESPONSE_START;
		buf[1] = KEY_RESPONSE_START;
		// copy length
		buf[2] = (byte) ((48) & 0xFF); // L1
		buf[3] = (byte) ((48 >> 8) & 0xFF); // L2
		buf[4] = (byte) ((48 >> 16) & 0xFF); // L3
		buf[5] = (byte) ((48 >> 24) & 0xFF); // L4
		// copy type 
		buf[6] = RESPONSE_PARSER_TYPE;
		// copy UUID
		System.arraycopy(uuid, 0, buf, 7, 36);
		// copy status
		buf[43] = status;
		// copy CRC
		byte[] tmp = new byte[44];
		System.arraycopy(buf, 0, tmp, 0, 44);
		byte[] crc = CRCUtil.makeCrcToBytes(tmp);
		buf[44] = crc[1]; // L
		buf[45] = crc[0]; // H
		buf[46] = KEY_END_0; // \r
		buf[47] = KEY_END_1; // \n

		return buf;
	}

	public static ResponseData dataUnpack(byte[] dataPack) {
		ResponseData responseData = null;

		try {
			if (checkDataPack(dataPack)) {
				responseData = new ResponseData();

				byte[] uuid = new byte[36];
				System.arraycopy(dataPack, 7, uuid, 0, 36);
				byte status = dataPack[43];

				responseData.setUuid(new String(uuid));
				responseData.setStatus(status);
			} else {
				LogTool.e("", "Response data is invalid.");
			}
		} catch (Exception e) {
			LogTool.e("", e.getMessage(), e);
		}
		return responseData;
	}

	public static boolean checkDataPack(byte[] dataPack) {
		// check pack length
		if (dataPack.length != 48) {
			// error data pack
			return false;
		}

		// check CRC
		byte[] abc = new byte[dataPack.length - 4];
		System.arraycopy(dataPack, 0, abc, 0, dataPack.length - 4);
		byte crc0 = dataPack[dataPack.length - 4];
		byte crc1 = dataPack[dataPack.length - 3];
		byte[] newCrc = CRCUtil.makeCrcToBytes(abc);
		if (newCrc[0] != crc1 || newCrc[1] != crc0) {
			// error CRC
			return false;
		}

		return true;
	}

	public static void main(String[] args) {
		ResponseData dataPack = new ResponseData(UUID.randomUUID().toString(), 1);
		System.out.println("dataPack uuid : " + dataPack.getUuid() + ", status : " + dataPack.getStatus());
		byte[] data = dataPack(dataPack);
		System.out.println(Arrays.toString(data));
		ResponseData dataUnpack = dataUnpack(data);
		System.out.println("dataUnpack uuid : " + dataUnpack.getUuid() + ", status : " + dataUnpack.getStatus());
	}
}
