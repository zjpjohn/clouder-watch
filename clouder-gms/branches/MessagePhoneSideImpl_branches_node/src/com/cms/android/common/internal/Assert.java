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
package com.cms.android.common.internal;

import android.text.TextUtils;

/**
 * ClassName: Assert
 * 
 * @description Assert
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public final class Assert {
	public static void isTrue(boolean flag, Object message) {
		if (!flag)
			throw new IllegalStateException(String.valueOf(message));
	}

	public static Object neNull(Object obj) {
		if (obj == null)
			throw new NullPointerException("obj = " + obj);
		return obj;
	}

	public static Object notEmpty(Object obj1, Object message) {
		if (obj1 == null)
			throw new NullPointerException(String.valueOf(message));
		return obj1;
	}

	public static String notEmpty(String str) {
		if (TextUtils.isEmpty(str))
			throw new IllegalArgumentException("String is = " + str);
		return str;
	}

	public static String notEmpty(String str, Object message) {
		if (TextUtils.isEmpty(str))
			throw new IllegalArgumentException(String.valueOf(message));
		return str;
	}

	public static void notEmpty(boolean flag, Object message) {
		if (!flag)
			throw new IllegalArgumentException(String.valueOf(message));
	}

	public static void valid(boolean flag) {
		if (!flag)
			throw new IllegalStateException();
	}
}