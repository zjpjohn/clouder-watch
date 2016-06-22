/*****************************************************************************
 *
 *                      HOPERUN PROPRIETARY INFORMATION
 *
 *          The information contained herein is proprietary to HopeRun
 *           and shall not be reproduced or disclosed in whole or in part
 *                    or used for any design or manufacture
 *              without direct written authorization from HopeRun.
 *
 *            Copyright (coffee) 2015 by HopeRun.  All rights reserved.
 *
 *****************************************************************************/
package com.example.testasset;

import java.util.Date;

/**
 * ClassName: ByteUtil
 * 
 * @description
 * @author hu_wg
 * @Date Jan 22, 2013
 * 
 */
public class ByteUtil {

	public static void main(String[] args) {
		System.out.println(new Date());
		System.out.println(new Date().getTime());
		System.out.println(getBinaryString((byte) -91));
		// 1 00000010 00000100 00001000
		System.out.println(getUIntByByteArray(new byte[] { 1, -25, -19, 3 }, false));
		System.out.println(getUIntByByteArray(new byte[] { 7, 20, 5, -128 }, false));
		System.out.println(getUIntByByteArray(new byte[] { 1, 1, 1, 1 }));
		System.out.println(getUIntByByteArray(new byte[] { 8, 4 }));
		System.out.println(getUIntByByteArray(new byte[] { 8 }));
		System.out.println(getUIntByByte((byte) 8));
		System.out.println(getUIntByByte((byte) -8));
		getCopyByteArrayArray(new byte[] { 8, 4, 2, 1 }, 0, 4, 2);
		System.out.println(getByteArrayByLong(0x2002024L, 4));
		System.out.println(checkBit(new byte[] { 13 }, 0));
		System.out.println(getBinaryString((byte) 13));
		System.out.println(getBinaryString((byte) getUIntByByteArray(new byte[] { 0, 15, 0, 13 }, false)));
		System.out.println(checkBit(new byte[] { 0, 15, 0, 13 }, 2));
		System.out.println(checkBit(new byte[] { 0, 15, 0, 13 }, 3));
		System.out.println(checkBit(new byte[] { 0, 15, 0, 13 }, 5));

	}

	/**
	 * new byte[] { 19, 3, 40, 20, 72, 56 }, 19 => 1
	 * 
	 * @param data
	 * @return
	 */
	public static int countFlag(byte[] data, byte flag) {
		int count = 0;
		for (int i = 0; i < data.length; i++) {
			if (data[i] == flag) {
				count++;
			}
		}
		return count;
	}

	/**
	 * new byte[]{00001101} => 0, 2, 3 = true, 1, 4, 5, 6, 7 = false
	 * 
	 * @param src
	 * @param index
	 * @return
	 */
	public static boolean checkBit(byte[] src, int index) {
		return checkBit(src, index, true);
	}

	public static boolean checkBit(byte[] src, int index, boolean isLH) {
		return getBitData(src, index, isLH) == 1;
	}

	/**
	 * new byte[]{13, 0} => 00001101, 00000000 => 8 = 1, 10 = 1, 11 = 1
	 * 
	 * @param src
	 * @param index
	 * @return
	 */
	public static byte getBitData(byte[] src, int index) {
		return getBitData(src, index, true);
	}

	public static byte getBitData(byte[] src, int index, boolean isLH) {
		return getBitData(src, index, index + 1, isLH)[0];
	}

	/**
	 * new byte[]{13, 0} => 00001101, 00000000 => 8-11 = new byte[]{1, 0, 1, 1}
	 * 
	 * @param src
	 * @param index
	 * @return
	 */
	public static byte[] getBitData(byte[] src, int start, int end) {
		return getBitData(src, start, end, true);
	}

	public static byte[] getBitData(byte[] src, int start, int end, boolean isLH) {
		byte[] result = null;
		StringBuffer sb = new StringBuffer();
		for (byte b : src) {
			sb.append(getBinaryString(b));
		}
		String bitString = (isLH ? sb : sb.reverse()).substring(start, end);
		result = new byte[bitString.length()];
		for (int i = 0; i < bitString.length(); i++) {
			result[i] = Byte.valueOf(String.valueOf(bitString.charAt(i)));
		}
		return result;
	}

	/**
	 * 7 => 00000111
	 * 
	 * @param src
	 * @return
	 */
	public static String getBinaryString(long src) {

		String binaryString = Long.toBinaryString(src);
		String temp = "";
		for (int i = 0; i < Long.SIZE - binaryString.length(); i++) {
			temp += "0";
		}
		binaryString = temp + binaryString;
		return binaryString;
	}

	/**
	 * 7 => 00000111
	 * 
	 * @param src
	 * @return
	 */
	public static String getBinaryString(byte src) {
		String binaryString = Integer.toBinaryString(src);
		if (binaryString.length() > Byte.SIZE) {
			binaryString = binaryString.substring(binaryString.length() - Byte.SIZE);
		} else {
			String temp = "";
			for (int i = 0; i < Byte.SIZE - binaryString.length(); i++) {
				temp += "0";
			}
			binaryString = temp + binaryString;
		}
		return binaryString;
	}

	/**
	 * get bytes by the long value. l before h after
	 * 
	 * @param value
	 * @param n
	 *            return bytes length
	 * @return
	 */
	public static byte[] getByteArrayByLong(long value, int n) {
		return getByteArrayByLong(value, n, true);
	}

	/**
	 * get bytes by the long value
	 * 
	 * @param value
	 * @param n
	 * @param isLH
	 * @return
	 */
	public static byte[] getByteArrayByLong(long value, int n, boolean isLH) {
		byte[] dest = new byte[n];
		for (int i = 0; i < n; i++) {

			dest[i] = (byte) ((value >> ((isLH ? i : n - i - 1) * 8)) & 0xFF);
		}
		return dest;
	}

	public static byte[] getCopyByteArray(byte[] src, int start, int length) {
		byte[] dest = new byte[length];
		System.arraycopy(src, start, dest, 0, length);
		return dest;
	}

	/**
	 * new byte[] { 8, 4, 2, 1 }, 0, 4, 2 => new byte[][]{{8,4}, {2,1}}
	 * 
	 * @param src
	 * @param start
	 * @param length
	 * @param subArrayLength
	 * @return
	 */

	public static byte[][] getCopyByteArrayArray(byte[] src, int start, int length, int subArrayLength) {
		byte[][] dest = new byte[length / subArrayLength][subArrayLength];
		byte[] temp = new byte[length];
		System.arraycopy(src, start, temp, 0, length);
		for (int i = 0; i < dest.length; i++) {
			System.arraycopy(temp, i * subArrayLength, dest[i], 0, subArrayLength);
		}

		return dest;
	}

	public static String getStringByByteArray(byte[] b) {
		return new String(b);
	}

	public static long getUIntByByteArray(byte[] data, boolean isLH) {

		long result = 0;
		for (int i = 0; i < data.length; i++) {
			byte b = data[i];

			int t = getUIntByByte(b);
			result += t << ((isLH ? i : data.length - i - 1) * 8);
		}

		return result;
	}

	/**
	 * new byte[] { 8, 4, 2, 1 } => 00000001 00000010 00000100 00001000
	 * 
	 * @param data
	 * @return
	 */
	public static long getUIntByByteArray(byte[] data) {
		return getUIntByByteArray(data, true);
	}

	/**
	 * new byte[] { 8, 4, 2, 1 } => 00000001 00000010 00000100 00001000
	 * 
	 * @param data
	 * @return
	 */
	public static int getUIntByByte(byte data) {
		return data & 0xFF;
	}

	public static byte[] updateFFto00(byte[] src) {
		byte[] dest = new byte[src.length];
		for (int i = 0; i < src.length; i++) {
			dest[i] = src[i] == -1 ? 0 : src[i];
		}
		return dest;
	}
}
