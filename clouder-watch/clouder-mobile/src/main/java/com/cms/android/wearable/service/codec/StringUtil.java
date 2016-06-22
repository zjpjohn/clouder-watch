/*****************************************************************************
 * HOPERUN PROPRIETARY INFORMATION
 * <p/>
 * The information contained herein is proprietary to HopeRun
 * and shall not be reproduced or disclosed in whole or in part
 * or used for any design or manufacture
 * without direct written authorization from HopeRun.
 * <p/>
 * Copyright (c) 2013 by HopeRun.  All rights reserved.
 *****************************************************************************/
package com.cms.android.wearable.service.codec;

import java.security.SecureRandom;
import java.util.List;
import java.util.Random;

import android.util.Base64;

/**
 * ClassName: StringUtil
 *
 * @description
 * @author hu_wg
 * @Date Jan 23, 2013
 *
 */
public class StringUtil {
    public static final String HEX_STRING_BLANK_SPLIT = " ";
    public static final String HEX_STRING_NOT_SPLIT = "";

    private static final Random RANDOM = new SecureRandom();

    static final char[] RANDOM_LETTERS = new char[]{'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k',
            'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F',
            'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '1',
            '2', '3', '4', '5', '6', '7', '8', '9', '0', '+', '-', '@'};

    public static String generateRandomPassword(int length) {

        StringBuffer sb = new StringBuffer(length);
        for (int i = 0; i < length; i++) {
            int index = (int) (RANDOM.nextDouble() * RANDOM_LETTERS.length);
            sb.append(RANDOM_LETTERS[index]);
        }
        return sb.toString();
    }


    public static String decodeBase64(String src) {
        return new String(Base64.decode(src, Base64.DEFAULT));
    }

    public static String join(List<String> list, String glue) {
        return join(list.toArray(new String[0]), glue);
    }

    public static String join(String[] s, String glue) {
        int k = s.length;
        if (k == 0)
            return null;
        StringBuilder out = new StringBuilder();
        out.append(s[0]);
        for (int x = 1; x < k; ++x)
            out.append(glue).append(s[x]);
        return out.toString();
    }

    public static byte[] getByteArrayByHexString(String hex) {
        return getByteArrayByHexString(hex, StringUtil.HEX_STRING_BLANK_SPLIT);
    }

    public static byte[] getByteArrayByHexString(String hex, String splitString) {
        String[] hexArrays = null;
        if (splitString.equals(HEX_STRING_NOT_SPLIT)) {
            hexArrays = new String[hex.length() / 2];
            for (int i = 0; i < hexArrays.length; i++) {
                hexArrays[i] = hex.substring(i * 2, i * 2 + 2);
            }
        } else {
            hexArrays = hex.split(splitString);
        }
        byte[] b = new byte[hexArrays.length];
        for (int i = 0; i < hexArrays.length; i++) {
            b[i] = (byte) Integer.parseInt(hexArrays[i], 16);
        }
        return b;
    }

    public static String getHexString(byte[] b) {
        return getHexString(b, HEX_STRING_BLANK_SPLIT);
    }

    public static String getHexString(byte[] b, String splitString) {
        int[] intArray = new int[b.length];
        for (int i = 0; i < b.length; i++) {
            if (b[i] < 0) {
                intArray[i] = b[i] + 256;
            } else {
                intArray[i] = b[i];
            }
        }
        return getHexString(intArray, splitString);
    }

    public static String getHexString(int[] b) {
        return getHexString(b, HEX_STRING_BLANK_SPLIT);
    }

    public static String getHexString(int[] b, String splitString) {
        StringBuffer sb = new StringBuffer();
        for (int c : b) {
            String strData = Integer.toHexString(c);
            if (strData.length() == 1) {
                sb.append("0").append(strData);
            } else {
                sb.append(strData);
            }
            sb.append(splitString);
        }
        return sb.toString().trim();
    }

    public static String getHexString(int i) {
        return getHexString(new int[]{i});
    }

    public static String getHexString(byte i) {
        return getHexString(new byte[]{i});
    }

    public static byte[] getBytes(String hexString) {
        String[] hexArray = hexString.split(HEX_STRING_BLANK_SPLIT);
        byte[] bytes = new byte[hexArray.length];
        for (int i = 0; i < hexArray.length; i++) {
            String hex = hexArray[i];
            bytes[i] = Integer.valueOf(hex, 16).byteValue();
        }
        return bytes;
    }

    public static byte[] longToByte(long number) {
        long temp = number;
        byte[] b = new byte[8];
        for (int i = 0; i < b.length; i++) {
            b[i] = Long.valueOf(temp & 0xff).byteValue();// 将最低位保存在最低位
            temp = temp >> 8; // 向右移8位
        }
        return b;
    }

    // byte数组转成long
    public static long byteToLong(byte[] b) {
        long s = 0;
        long s0 = b[0] & 0xff;// 最低位
        long s1 = b[1] & 0xff;
        long s2 = b[2] & 0xff;
        long s3 = b[3] & 0xff;
        long s4 = b[4] & 0xff;// 最低位
        long s5 = b[5] & 0xff;
        long s6 = b[6] & 0xff;
        long s7 = b[7] & 0xff;

        // s0不变
        s1 <<= 8;
        s2 <<= 16;
        s3 <<= 24;
        s4 <<= 8 * 4;
        s5 <<= 8 * 5;
        s6 <<= 8 * 6;
        s7 <<= 8 * 7;
        s = s0 | s1 | s2 | s3 | s4 | s5 | s6 | s7;
        return s;
    }
}
