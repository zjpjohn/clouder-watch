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

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;

import com.cms.android.wearable.Asset;
import com.cms.android.wearable.service.common.FileUtil;
import com.cms.android.wearable.service.common.LogTool;
import com.cms.android.wearable.service.impl.BLEPeripheralService.MappedInfo;

/**
 * ClassName: DataInfoParser
 * 
 * @description
 * @author xing_peng
 * @Date 2015-8-21
 * 
 */
public class DataInfoParser {

	private static final String TAG = "DataInfoParser";

	public final static byte KEY_REQUEST_START = 38;// &
	public final static byte KEY_END_0 = 0x0d;// \r
	public final static byte KEY_END_1 = 0x0a;// \n

	public static MappedInfo dataPack(DataInfo dataObj) throws IOException {
		String filepath = FileUtil.createFilePath(dataObj.getUUID().toString());
		LogTool.d(TAG, "filepath = " + filepath);
		byte[] uuid = dataObj.getUUID() == null ? new byte[] {} : dataObj.getUUID().toString().getBytes();
		byte[] deviceId = dataObj.getDeviceId() == null ? new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0 } : dataObj.getDeviceId().getBytes();
		byte type = (byte) dataObj.getType();
		byte[] uri = dataObj.getUri().getPath().getBytes();
		byte[] timeStamp = ByteUtil.getByteArrayByLong(dataObj.getTimeStamp(), 6);
		byte[] packageName = dataObj.getPackageName() == null ? new byte[] {} : dataObj.getPackageName().getBytes();
		byte[] data = dataObj.getData();
		LogTool.d(TAG, dataObj.toString());
		// byte[] mData = toByteArray(dataObj.getAssetsMap());
		// LogTool.e(TAG, "mData size = " + mData.length);
		// head length UUID deviceId type versionCode timeStamp uriL URI
		// packageNameL packageName dataL data mDataL mData CRC tail
		// 2 4 36 20 1 4 6 1 ? 1 ? 4 ? 4 ? 2 2
		long assetsLength = calculateDataLength(dataObj.getAssetsMap());
		LogTool.d(TAG, "assetsLength = " + assetsLength);
		long totalLength = 91 + data.length + assetsLength + uri.length + packageName.length;
		LogTool.d(TAG, "totalLength = " + totalLength);
		RandomAccessFile randomAccessFile = new RandomAccessFile(filepath, "rw");
		FileChannel fc = randomAccessFile.getChannel();
		// 注意，文件通道的可读可写要建立在文件流本身可读写的基础之上
		MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_WRITE, 0, totalLength);
		// copy head 0-1
		buffer.put(KEY_REQUEST_START);
		buffer.put(KEY_REQUEST_START);
		// copy length 2-5
		buffer.put((byte) (totalLength & 0xFF));// L1
		buffer.put((byte) ((totalLength >> 8) & 0xFF));// L2
		buffer.put((byte) ((totalLength >> 16) & 0xFF));// L3
		buffer.put((byte) ((totalLength >> 24) & 0xFF));// L4
		// copy UUID 6-41
		LogTool.d(TAG, "UUID->" + Arrays.toString(uuid));
		buffer.put(uuid);
		// copy deviceId
		buffer.put(deviceId);
		LogTool.d(TAG, "deviceId->" + Arrays.toString(deviceId));
		// copy type
		buffer.position(62);
		buffer.put(type);
		LogTool.d(TAG, "type->" + type);
		// copy versionCode 63
		byte[] versionCode = new byte[4];
		versionCode[0] = (byte) (dataObj.getVersionCode() & 0xFF); // L1
		versionCode[1] = (byte) ((dataObj.getVersionCode() >> 8) & 0xFF); // L2
		versionCode[2] = (byte) ((dataObj.getVersionCode() >> 16) & 0xFF); // L3
		versionCode[3] = (byte) ((dataObj.getVersionCode() >> 24) & 0xFF); // L4
		LogTool.d(TAG, "versionCode->" + Arrays.toString(versionCode));
		buffer.put(versionCode);
		// copy timeStamp 67
		LogTool.d(TAG, "timeStamp->" + Arrays.toString(timeStamp));
		buffer.put(timeStamp);
		// copy uriLength 73
		buffer.put((byte) uri.length);
		LogTool.d(TAG, "uri.length->" + uri.length);
		// copy URI 74
		buffer.put(uri);
		LogTool.d(TAG, "uri->" + Arrays.toString(uri));
		// copy packageNameLength
		buffer.position(74 + uri.length);
		buffer.put((byte) packageName.length);
		LogTool.d(TAG, "packageName.length->" + packageName.length);
		// copy packageName
		buffer.position(75 + uri.length);
		buffer.put(packageName);
		LogTool.d(TAG, "packageName->" + Arrays.toString(packageName));
		// copy data length
		buffer.putInt(data == null ? 0 : data.length);
		LogTool.d(TAG, "data->" + Arrays.toString(data));
		// copy data
		buffer.position(79 + uri.length + packageName.length);
		buffer.put(data);
		// copy assets Length
		LogTool.d(TAG, "assetsLength->" + assetsLength);
		buffer.putLong(assetsLength);
		buffer.position(87 + uri.length + packageName.length + data.length);
		toByteArray(buffer, dataObj.getAssetsMap());
		// System.arraycopy(mData, 0, buf, 83 + uri.length + packageName.length
		// + data.length, mData.length);
		// copy CRC
		// byte[] tmp = new byte[totalLength - 4];
		// System.arraycopy(buf, 0, tmp, 0, totalLength - 4);
		// 根据文件生成CRC TODO
		byte[] crc = CRCUtil.makeCrcToBytes(filepath);
		buffer.put(crc[1]);// L
		buffer.put(crc[0]);// H
		buffer.put(KEY_END_0);// \r
		buffer.put(KEY_END_1); // \n

		buffer.position(0);
		MappedInfo info = new MappedInfo(filepath, buffer);

		return info;
	}

	public static DataInfo dataUnpack(RandomAccessFile randomAccessFile) throws IOException {
		DataInfo dataObj = null;
		// RandomAccessFile randomAccessFile = null;
		try {
			// randomAccessFile = new RandomAccessFile(filepath, "rw");
			FileChannel fc = randomAccessFile.getChannel();
			// 注意，文件通道的可读可写要建立在文件流本身可读写的基础之上
			long size = randomAccessFile.getChannel().size();
			LogTool.d(TAG, "totalLength = " + size);
			MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_WRITE, 0, size);

			// check data start and end when read byte[] from IO.
			// if (checkDataPack(dataPack)) {
			dataObj = new DataInfo();
			// UUID
			byte[] uuid = new byte[36];
			buffer.position(6);
			buffer.get(uuid, 0, 36);
			LogTool.d(TAG, "uuid = " + new String(uuid));
			// System.arraycopy(dataPack, 6, uuid, 0, 36);
			// deviceId
			byte[] deviceId = new byte[20];
			buffer.position(42);
			buffer.get(deviceId, 0, 20);
			LogTool.d(TAG, "deviceId = " + new String(deviceId));
			// System.arraycopy(dataPack, 42, deviceId, 0, 20);
			// type
			// byte type = dataPack[62];
			buffer.position(62);
			byte type = buffer.get();
			LogTool.d(TAG, "type = " + type);
			// versionCode
			buffer.position(63);
			byte vL0 = buffer.get();
			byte vL1 = buffer.get();
			byte vL2 = buffer.get();
			byte vL3 = buffer.get();
			int versionCode = byteToInt(vL0, vL1, vL2, vL3);
			LogTool.d(TAG, "versionCode = " + versionCode);
			// timeStamp
			byte[] timeStamp = new byte[6];
			buffer.position(67);
			buffer.get(timeStamp, 0, 6);
			LogTool.d(TAG, "timeStamp = " + Arrays.toString(timeStamp));
			// System.arraycopy(dataPack, 67, timeStamp, 0, 6);
			// URI 73
			int uriLength = buffer.get();
			LogTool.d(TAG, "uriLength = " + uriLength);
			// int uriLength = dataPack[73];
			byte[] uri = new byte[uriLength];
			buffer.get(uri, 0, uriLength);
			LogTool.d(TAG, "uri = " + new String(uri));
			// System.arraycopy(dataPack, 74, uri, 0, uriLength);
			// packageName
			buffer.position(74 + uriLength);
			int packageNameLength = buffer.get();
			// int packageNameLength = dataPack[74 + uriLength];
			byte[] packageName = new byte[packageNameLength];
			buffer.get(packageName, 0, packageNameLength);
			LogTool.d(TAG, "packageName = " + new String(packageName));
			int dataLength = buffer.getInt();
			byte[] data = new byte[dataLength];
			buffer.get(data, 0, dataLength);

			dataObj.setData(data);
			dataObj.setDeviceId(new String(deviceId));
			dataObj.setTimeStamp(timeStamp);
			dataObj.setPackageName(new String(packageName));
			dataObj.setUri(parse(new String(uri)));
			dataObj.setVersionCode(versionCode);
			dataObj.setAssetsMap(fromByteArray(size, new String(uuid), buffer));
			dataObj.setUUID(new String(uuid));
		} catch (Exception e) {
			LogTool.e("", e.getMessage(), e);
		} finally {
			if (randomAccessFile != null) {
				randomAccessFile.close();
			}
		}
		return dataObj;
	}

	// public static boolean checkDataPack(byte[] dataPack) {
	// // check is valid pack
	// if (dataPack.length < 87) {
	// // error data pack
	// return false;
	// }
	//
	// // check pack length
	// byte l0 = dataPack[2];
	// byte l1 = dataPack[3];
	// byte l2 = dataPack[4];
	// byte l3 = dataPack[5];
	// int length = byteToInt(l0, l1, l2, l3);
	// if (dataPack.length != length) {
	// // error data pack length
	// return false;
	// }
	//
	// // check CRC
	// byte[] abcde = new byte[dataPack.length - 4];
	// System.arraycopy(dataPack, 0, abcde, 0, dataPack.length - 4);
	// byte crc0 = dataPack[dataPack.length - 4];
	// byte crc1 = dataPack[dataPack.length - 3];
	// byte[] newCrc = CRCUtil.makeCrcToBytes(abcde);
	// if (newCrc[0] != crc1 || newCrc[1] != crc0) {
	// // error CRC
	// return false;
	// }
	//
	// return true;
	// }

	private static long calculateAssetPack(Asset asset) {
		long length = 0;
		byte[] digest = asset.getDigest() == null ? new byte[] {} : asset.getDigest().getBytes();
		byte[] uri = asset.getUri() == null ? new byte[] {} : asset.getUri().getPath().getBytes();
		ParcelFileDescriptor pfd = asset.getFd();
		if (pfd != null) {
			// total length(8) + data length(8) + data + digest length(1) +
			// digest + uri length(1) + uri + version
			length = 8 + 8 + pfd.getStatSize() + 1 + digest.length + 1 + uri.length + 4;
		} else {
			length = 8 + 8 + 0 + 1 + digest.length + 1 + uri.length + 4;
		}
		return length;
	}

	public static long assetPack(MappedByteBuffer buffer, Asset asset) {
		LogTool.d(TAG, "assetPack...");
		byte[] digest = asset.getDigest() == null ? new byte[] {} : asset.getDigest().getBytes();
		byte[] uri = asset.getUri() == null ? new byte[] {} : asset.getUri().getPath().getBytes();

		long length = 0;
		ParcelFileDescriptor pfd = asset.getFd();
		if (pfd != null) {
			long dataLength = pfd.getStatSize();
			LogTool.d(TAG, "assetPack dataLength = " + dataLength);
			// total length(8) + data length(8) + data + digest length(1) +
			// digest + uri length(1) + uri + version
			length = 8 + 8 + pfd.getStatSize() + 1 + digest.length + 1 + uri.length + 4;
			buffer.putLong(length);
			// copy dataLength
			buffer.putLong(dataLength);
			LogTool.d(TAG,
					"stat Size = " + pfd.getStatSize() + " fd = " + pfd.getFd() + " pfd = " + pfd.getFileDescriptor());
			InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
			byte[] buf = new byte[1024];
			int readLength = -1;
			try {
				while ((readLength = is.read(buf)) > 0) {
					buffer.put(buf, 0, readLength);
				}
			} catch (IOException e) {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		} else {
			LogTool.d(TAG, "assetPack else ");
			length = 8 + 8 + 0 + 1 + digest.length + 1 + uri.length + 4;
			buffer.putLong(length);
			// copy dataLength
			buffer.putLong(0);
		}
		// copy digestLength
		buffer.put((byte) digest.length);
		// copy digest
		buffer.put(digest);
		// copy uriLength
		buffer.put((byte) uri.length);
		// copy URI
		buffer.put(uri);
		// copy versionCode
		buffer.putInt(asset.getVersionCode());
		return length;
	}

	public static Asset assetUnpack(Long size, String uuid, MappedByteBuffer buffer) {
		long assetlength = buffer.getLong();
		long assetDataLength = buffer.getLong();
		LogTool.d(TAG, " assetlength = " + assetlength + " assetDataLength = " + assetDataLength);
		int index = buffer.position();
		buffer.position(index + (int) assetDataLength);
		int assetDigestLength = buffer.get();
		byte[] assetDigest = new byte[assetDigestLength];
		buffer.get(assetDigest, 0, assetDigestLength);

		int assetUriLength = buffer.get();
		byte[] assetUri = new byte[assetUriLength];
		buffer.get(assetUri, 0, assetUriLength);

		int assetVersion = buffer.getInt();
		int versionIndex = buffer.position();
		LogTool.d(TAG, "index = " + index + " versionIndex = " + versionIndex);
		LogTool.d(TAG, " digest = " + new String(assetDigest) + " uri = " + new String(assetUri) + " version = "
				+ assetVersion);
		ChildAsset asset = new ChildAsset(assetVersion, null, new String(assetDigest), null,
				assetUri.length > 0 ? parse(new String(assetUri)) : null);
		asset.setUuid(uuid);
		asset.setIndex(index);
		asset.setSize(size);
		asset.setAssetSize(assetDataLength);
		return asset;
	}

	private static long calculateDataLength(Map<String, Asset> map) {
		int length = 4;
		for (String key : map.keySet()) {
			byte[] keyBytes = key.getBytes();
			length += 4;
			length += keyBytes.length;
			length += calculateAssetPack(map.get(key));
		}
		return length;
	}

	public static void toByteArray(MappedByteBuffer buffer, Map<String, Asset> map) {
		buffer.putInt(map.size());
		for (String key : map.keySet()) {
			byte[] keyBytes = key.getBytes();
			buffer.putInt(keyBytes.length);
			buffer.put(keyBytes);
			assetPack(buffer, map.get(key));
		}
	}

	public static Map<String, Asset> fromByteArray(Long size, String uuid, MappedByteBuffer buffer) {
		long assetsLength = buffer.getLong();
		LogTool.d(TAG, "assetsLength = " + assetsLength);
		int count = buffer.getInt();
		LogTool.d(TAG, "count = " + count);
		Map<String, Asset> assetMap = new HashMap<String, Asset>();
		for (int i = 0; i < count; i++) {
			int keyLength = buffer.getInt();
			byte[] key = new byte[keyLength];
			buffer.get(key, 0, keyLength);
			LogTool.d(TAG, " keyLength = " + keyLength + " key = " + new String(key));
			assetMap.put(new String(key), assetUnpack(size, uuid, buffer));
		}
		return assetMap;
	}

	private static Uri parse(String path) {
		if (TextUtils.isEmpty(path))
			throw new IllegalArgumentException("path is null or empty.");
		if ((!path.startsWith("/")) || (path.startsWith("//")))
			throw new IllegalArgumentException("path must start with a single / .");
		return new Uri.Builder().scheme("wear").path(path).build();
	}

	private static int byteToInt(byte l0, byte l1, byte l2, byte l3) {
		return ((l3 << 24) & 0xFF000000) | ((l2 << 16) & 0xFF0000) | ((l1 << 8) & 0xFF00) | l0 & 0xFF;
	}

}
