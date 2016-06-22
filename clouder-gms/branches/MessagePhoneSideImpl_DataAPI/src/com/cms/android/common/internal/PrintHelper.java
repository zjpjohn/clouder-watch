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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ClassName: PrintHelper
 * 
 * @description PrintHelper
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public class PrintHelper {
	public static boolean equal(Object obj1, Object obj2) {
		return (obj1 == obj2) || ((obj1 != obj2) && (obj1.equals(obj2)));
	}

	public static Printer getPrinter(Object obj) {
		return new Printer(obj);
	}

	public static int hashCode(Object[] arrayOfObject) {
		return Arrays.hashCode(arrayOfObject);
	}

	public static final class Printer {
		private final List<String> mFields;
		private final Object mObj;

		private Printer(Object obj) {
			this.mObj = Assert.neNull(obj);
			this.mFields = new ArrayList<String>();
		}

		public Printer addField(String paramString, Object paramObject) {
			this.mFields.add(Assert.notEmpty(paramString) + "=" + String.valueOf(paramObject));
			return this;
		}

		public String toString() {
			StringBuffer localStringBuffer = new StringBuffer(120).append(this.mObj.getClass().getSimpleName()).append(
					'{');
			int size = this.mFields.size();
			for (int j = 0; j <= size - 1; j++) {
				localStringBuffer.append((String) this.mFields.get(j));
				localStringBuffer.append(", ");
				if (j == size - 1) {
					localStringBuffer.append('}');
				}
			}
			return localStringBuffer.toString();
		}
	}
}