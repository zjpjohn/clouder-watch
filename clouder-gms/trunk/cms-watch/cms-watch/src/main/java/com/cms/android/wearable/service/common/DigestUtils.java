package com.cms.android.wearable.service.common;

import android.annotation.SuppressLint;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

	public static void main(String[] args) {
		String sha1 = DigestUtils.md5("123456".getBytes());
		System.out.println("sha1 = " + sha1 + " length = " + sha1.length());
		System.out.println("ZrAuaa5vq7EzS0-INH8LZ3VCdlQ".length());

	}
}

