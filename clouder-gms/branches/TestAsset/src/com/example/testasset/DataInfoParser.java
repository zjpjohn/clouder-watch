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
package com.example.testasset;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

/**
 * ClassName: DataInfoParser
 *
 * @description
 * @author xing_peng
 * @Date 2015-8-21
 * 
 */
public class DataInfoParser {

	public final static byte KEY_REQUEST_START = 38;// &
	public final static byte KEY_END_0 = 0x0d;// \r
	public final static byte KEY_END_1 = 0x0a;// \n

	public static byte[] dataPack(DataInfo dataObj) {

		byte[] uuid = dataObj.getUUID().toString().getBytes();
		byte[] deviceId = dataObj.getDeviceId().getBytes();
		byte type = (byte) dataObj.getType();
		byte[] uri = dataObj.getUri().getPath().getBytes();
		byte[] timeStamp = ByteUtil.getByteArrayByLong(dataObj.getTimeStamp(), 6);
		byte[] data = dataObj.getData();
		byte[] mData = toByteArray(dataObj.getAssetsMap());
		byte[] versionCode = new byte[4];
		versionCode[0] = (byte) (dataObj.getVersionCode() & 0xFF); // L1
		versionCode[1] = (byte) ((dataObj.getVersionCode() >> 8) & 0xFF); // L2
		versionCode[2] = (byte) ((dataObj.getVersionCode() >> 16) & 0xFF); // L3
		versionCode[3] = (byte) ((dataObj.getVersionCode() >> 24) & 0xFF); // L4

		// head length UUID deviceId type versionCode timeStamp uriL URI dataL data mDataL mData CRC tail
		//   2    4     36     20      1        4         6       1   ?    4     ?    4      ?    2   2
		int totalLength = 86 + data.length + mData.length + uri.length;
		byte[] buf = new byte[totalLength];
		// copy head
		buf[0] = KEY_REQUEST_START;
		buf[1] = KEY_REQUEST_START;
		// copy length
		buf[2] = (byte) (totalLength & 0xFF); // L1
		buf[3] = (byte) ((totalLength >> 8) & 0xFF); // L2
		buf[4] = (byte) ((totalLength >> 16) & 0xFF); // L3
		buf[5] = (byte) ((totalLength >> 24) & 0xFF); // L4
		// copy uuid
		System.arraycopy(uuid, 0, buf, 6, uuid.length);
		// copy deviceId
		System.arraycopy(deviceId, 0, buf, 42, deviceId.length);
		// copy type
		buf[62] = type;
		// copy versionCode
		System.arraycopy(versionCode, 0, buf, 63, versionCode.length);
		// copy timeStamp
		System.arraycopy(timeStamp, 0, buf, 67, timeStamp.length);
		// copy uriLength
		buf[73] = (byte) uri.length;
		// copy uri
		System.arraycopy(uri, 0, buf, 74, uri.length);
		// copy dataLength
		buf[74 + uri.length] = (byte) (data.length & 0xFF); // L1
		buf[75 + uri.length] = (byte) ((data.length >> 8) & 0xFF); // L2
		buf[76 + uri.length] = (byte) ((data.length >> 16) & 0xFF); // L3
		buf[77 + uri.length] = (byte) ((data.length >> 24) & 0xFF); // L4
		// copy data
		System.arraycopy(data, 0, buf, 78 + uri.length, data.length);
		// copy mDataLength
		buf[78 + uri.length + data.length] = (byte) (mData.length & 0xFF); // L1
		buf[79 + uri.length + data.length] = (byte) ((mData.length >> 8) & 0xFF); // L2
		buf[80 + uri.length + data.length] = (byte) ((mData.length >> 16) & 0xFF); // L3
		buf[81 + uri.length + data.length] = (byte) ((mData.length >> 24) & 0xFF); // L4
		// copy mData
		System.arraycopy(mData, 0, buf, 82 + uri.length + data.length, mData.length);
		// copy crc
		byte[] tmp = new byte[totalLength - 4];
		System.arraycopy(buf, 0, tmp, 0, totalLength - 4);
		byte[] crc = CRCUtil.makeCrcToBytes(tmp);
		buf[totalLength - 4] = crc[1]; // L
		buf[totalLength - 3] = crc[0]; // H
		buf[totalLength - 2] = KEY_END_0; // \r
		buf[totalLength - 1] = KEY_END_1; // \n

		return buf;
	}

	public static DataInfo dataUnpack(byte[] dataPack) {
		DataInfo dataObj = null;

		try {
			// check data start and end when read byte[] from io.
			if (checkDataPack(dataPack)) {
				dataObj = new DataInfo();
				// UUID
				byte[] uuid = new byte[36];
				System.arraycopy(dataPack, 6, uuid, 0, 36);
				// deviceId
				byte[] deviceId = new byte[20];
				System.arraycopy(dataPack, 42, deviceId, 0, 20);
				// type
				byte type = dataPack[62];
				// versionCode
				byte vL0 = dataPack[63];
				byte vL1 = dataPack[64];
				byte vL2 = dataPack[65];
				byte vL3 = dataPack[66];
				int versionCode = byteToInt(vL0, vL1, vL2, vL3);
				// timeStamp
				byte[] timeStamp = new byte[6];
				System.arraycopy(dataPack, 67, timeStamp, 0, 6);
				// URI
				int uriLength = dataPack[73];
				byte[] uri = new byte[uriLength];
				System.arraycopy(dataPack, 74, uri, 0, uriLength);
				// data
				byte dL0 = dataPack[74 + uri.length];
				byte dL1 = dataPack[75 + uri.length];
				byte dL2 = dataPack[76 + uri.length];
				byte dL3 = dataPack[77 + uri.length];
				int dataLength = byteToInt(dL0, dL1, dL2, dL3);
				byte[] data = new byte[dataLength];
				System.arraycopy(dataPack, 78 + uri.length, data, 0, dataLength);
				// mData
				byte mL0 = dataPack[78 + uri.length + data.length];
				byte mL1 = dataPack[79 + uri.length + data.length];
				byte mL2 = dataPack[80 + uri.length + data.length];
				byte mL3 = dataPack[81 + uri.length + data.length];
				int mDataLength = byteToInt(mL0, mL1, mL2, mL3);
				byte[] mData = new byte[mDataLength];
				System.arraycopy(dataPack, 82 + uri.length + data.length, mData, 0, mDataLength);

				dataObj.setData(data);
				dataObj.setDeviceId(new String(deviceId));
				dataObj.setTimeStamp(timeStamp);
				dataObj.setUri(parse(new String(uri)));
				dataObj.setVersionCode(versionCode);
				dataObj.setProtocolType(type);
				dataObj.setAssetsMap(fromByteArray(mData));
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
		if (dataPack.length < 86) {
			// error data pack
			return false;
		}

		// check pack length
		byte l0 = dataPack[2];
		byte l1 = dataPack[3];
		byte l2 = dataPack[4];
		byte l3 = dataPack[5];
		int length = byteToInt(l0, l1, l2, l3);
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
 
	public static byte[] assetPack(Asset asset) {
		byte[] data = asset.getData() == null ? new byte[] {} : asset.getData();
		byte[] digest = asset.getDigest() == null ? new byte[] {} : asset.getDigest().getBytes();
		byte[] uri = asset.getUri() == null ? new byte[] {} : asset.getUri().getPath().getBytes();
		byte[] versionCode = new byte[4];
		versionCode[0] = (byte) (asset.getVersionCode() & 0xFF); // L1
		versionCode[1] = (byte) ((asset.getVersionCode() >> 8) & 0xFF); // L2
		versionCode[2] = (byte) ((asset.getVersionCode() >> 16) & 0xFF); // L3
		versionCode[3] = (byte) ((asset.getVersionCode() >> 24) & 0xFF); // L4

		// dataLength data digestLength digest uriLength uri versionCode 
		//      4       ?        1         ?       1      ?       4    
		byte[] buf = new byte[10 + data.length + digest.length + uri.length];

		// copy dataLength
		byte[] dataLength = new byte[4];
		dataLength[0] = (byte) (data.length & 0xFF); // L1
		dataLength[1] = (byte) ((data.length >> 8) & 0xFF); // L2
		dataLength[2] = (byte) ((data.length >> 16) & 0xFF); // L3
		dataLength[3] = (byte) ((data.length >> 24) & 0xFF); // L4
		System.arraycopy(dataLength, 0, buf, 0, 4);
		// copy data
		System.arraycopy(data, 0, buf, 4, data.length);
		// copy digestLength
		buf[4 + data.length] = (byte) digest.length;
		// copy digest
		System.arraycopy(digest, 0, buf, 5 + data.length, digest.length);
		// copy uriLength
		buf[5 + data.length + digest.length] = (byte) uri.length;
		// copy uri
		System.arraycopy(uri, 0, buf, 6 + data.length + digest.length, uri.length);
		// copy versionCode
		System.arraycopy(versionCode, 0, buf, 6 + data.length + digest.length + uri.length, 4);

		return buf;
	}

	public static Asset assetUnpack(byte[] dataPack) {

		Asset asset = null;

		try {
			// copy data
			byte dL0 = dataPack[0];
			byte dL1 = dataPack[1];
			byte dL2 = dataPack[2];
			byte dL3 = dataPack[3];
			int dataLength = byteToInt(dL0, dL1, dL2, dL3);
			byte[] data = new byte[dataLength];
			System.arraycopy(dataPack, 4, data, 0, dataLength);
			// copy digest
			int digestLength = dataPack[4 + dataLength];
			byte[] digest = new byte[digestLength];
			System.arraycopy(dataPack, 5 + dataLength, digest, 0, digestLength);
			// copy uri
			int uriLength = dataPack[5 + dataLength + digestLength];
			byte[] uri = new byte[uriLength];
			System.arraycopy(dataPack, 6 + dataLength + digestLength, uri, 0, uriLength);
			// copy versionCode
			byte vL0 = dataPack[dataPack.length - 4];
			byte vL1 = dataPack[dataPack.length - 3];
			byte vL2 = dataPack[dataPack.length - 2];
			byte vL3 = dataPack[dataPack.length - 1];
			int versionCode = byteToInt(vL0, vL1, vL2, vL3);

			asset = new Asset(versionCode, data, new String(digest), null, uri.length > 0 ? parse(new String(uri))
					: null);
		} catch (Exception e) {
			Log.e("", "Asset data is invalid.");
		}

		return asset;
	}

	public static byte[] toByteArray(Map<String, Asset> map) {

		HashMap<String, byte[]> assetByteMap = new HashMap<String, byte[]>();
		for (String key : map.keySet()) {
			assetByteMap.put(key, assetPack(map.get(key)));
		}

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);

		byte[] mData = new byte[] {};
		try {
			writeFields(dos, assetByteMap);
			mData = bos.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				bos.flush();
				bos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		return mData;
	}

	public static Map<String, Asset> fromByteArray(byte[] data) {

		Map<String, Asset> assetMap = new HashMap<String, Asset>();

		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bis);

		try {
			HashMap<String, byte[]> assetByteMap = readFields(dis);
			for (String key : assetByteMap.keySet()) {
				assetMap.put(key, assetUnpack(assetByteMap.get(key)));
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				bis.close();
				dis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		return assetMap;
	}

	public static void writeFields(DataOutput dataOutput, HashMap<String, byte[]> map) throws IOException {
		dataOutput.writeInt(map.size());
		Iterator<String> iterator = map.keySet().iterator();
		while (iterator.hasNext()) {
			String key = (String) iterator.next();
			dataOutput.writeUTF(key);
			byte[] dest = map.get(key);
			dataOutput.writeInt(((byte[]) dest).length);
			dataOutput.write((byte[]) dest);
		}
	}

	public static HashMap<String, byte[]> readFields(DataInput dataInput) throws IOException {
		HashMap<String, byte[]> map = new HashMap<String, byte[]>();
		int i = dataInput.readInt();
		for (int j = 0; j < i; j++) {
			String str = dataInput.readUTF();

			int count = dataInput.readInt();
			byte[] bytes = new byte[count];
			dataInput.readFully((byte[]) bytes);
			map.put(str, bytes);
		}
		return map;
	}

	private static Uri parse(String path) {
		if (TextUtils.isEmpty(path))
			throw new IllegalArgumentException("path is null or empty.");
		if ((!path.startsWith("/")) || (path.startsWith("//")))
			throw new IllegalArgumentException("path must start with a single / .");
		return new Uri.Builder().scheme("wear").path(path).build();
	}

	public static void main(String[] args) {

		Asset asset = new Asset(100, new byte[] { 1, 0, 0 }, "test1", null, parse("/test1"));
		byte[] dataAsset1 = assetPack(asset);
		Asset newAsset = assetUnpack(dataAsset1);
		System.out.println(newAsset.getVersionCode() + " " + Arrays.toString(newAsset.getData()) + " "
				+ newAsset.getDigest() + " " + newAsset.getUri().getPath());

		HashMap<String, byte[]> map = new HashMap<String, byte[]>();
		String b1 = "byte1";
		map.put("test1", b1.getBytes());
		String b2 = "byte2";
		map.put("test2", b2.getBytes());
		String b3 = "byte3";
		map.put("test3", b3.getBytes());

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		try {
			writeFields(dos, map);
			bos.flush();
			bos.close();
			byte[] data = bos.toByteArray();

			System.out.println(Arrays.toString(data));

			ByteArrayInputStream bis = new ByteArrayInputStream(data);
			DataInputStream dataInputStream = new DataInputStream(bis);
			HashMap<String, byte[]> newMap = readFields(dataInputStream);

			for (String key : newMap.keySet()) {
				System.out.println(key);
				System.out.println(new String((byte[]) newMap.get(key)));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static int byteToInt(byte l0, byte l1, byte l2, byte l3) {
		return ((l3 << 24) & 0xFF000000) | ((l2 << 16) & 0xFF0000) | ((l1 << 8) & 0xFF00) | l0 & 0xFF;
	}
}
