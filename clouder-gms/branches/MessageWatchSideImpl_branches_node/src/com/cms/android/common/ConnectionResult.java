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
package com.cms.android.common;

import android.app.PendingIntent;

/**
 * ClassName: ConnectionResult
 * 
 * @description ConnectionResult
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public class ConnectionResult {
	public static final ConnectionResult SUCCESS_CONNECTION_RESULT = new ConnectionResult(0, null);
	private final int mErrorCode;
	private final PendingIntent mPendingIntent;

	public ConnectionResult(int errorCode, PendingIntent pendingIntent) {
		this.mErrorCode = errorCode;
		this.mPendingIntent = pendingIntent;
	}
	
	public int getErrorCode() {
		return mErrorCode;
	}

	public PendingIntent getPendingIntent() {
		return mPendingIntent;
	}



	public String getErrorText() {
		String str;
		switch (this.mErrorCode) {
		case 0:
			str = "SUCCESS";
			break;

		case 1:
			str = "SERVICE_MISSING";
			break;

		case 2:
			str = "SERVICE_VERSION_UPDATE_REQUIRED";
			break;

		case 3:
			str = "SERVICE_DISABLED";
			break;

		case 4:
			str = "SIGN_IN_REQUIRED";
			break;

		case 5:
			str = "INVALID_ACCOUNT";
			break;
		case 6:
			str = "RESOLUTION_REQUIRED";
			break;
		case 7:
			str = "NETWORK_ERROR";
			break;

		case 8:
			str = "INTERNAL_ERROR";
			break;

		case 9:
			str = "SERVICE_INVALID";
			break;
		case 10:
			str = "DEVELOPER_ERROR";
			break;
		case 11:
			str = "LICENSE_CHECK_FAILED";
			break;
		case 13:
			str = "CANCELED";
			break;
		case 14:
			str = "TIMEOUT";
			break;

		default:
			str = "unknown status code " + this.mErrorCode;
			break;
		}

		return str;

	}

	public String toString() {
		return "statusCode: " + getErrorText() + ", resolution: " + this.mPendingIntent;
	}
}