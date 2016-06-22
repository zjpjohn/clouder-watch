package com.cms.android.wearable.service.common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.annotation.SuppressLint;

public class DigestUtils {

	public static String md5(byte[] bytes) {
		MessageDigest md = null;
		String outStr = null;
		try {
			md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(bytes);
			outStr = byteToString(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		return outStr;
	}

	@SuppressLint("DefaultLocale")
	private static String byteToString(byte[] digest) {
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < digest.length; i++) {
			String tempStr = Integer.toHexString(digest[i] & 0xff);
			if (tempStr.length() == 1) {
				buf.append("0").append(tempStr);
			} else {
				buf.append(tempStr);
			}
		}
		return buf.toString().toLowerCase();
	}
}

