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
package com.cms.android.common.api;

/**
 * ClassName: CommonStatusCodes
 * 
 * @description CommonStatusCodes
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public class CommonStatusCodes {
	public static String getStatusCodeString(int statusCode) {
		String str;
		switch (statusCode) {
		case -1:
			str = "SUCCESS_CACHE";
			break;
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

		case 12:
			str = "INTERRUPT";
			break;

		case 3000:
			str = "AUTH_API_INVALID_CREDENTIALS";
			break;

		case 3001:
			str = "AUTH_API_ACCESS_FORBIDDEN";
			break;

		case 3002:
			str = "AUTH_API_CLIENT_ERROR";
			break;

		case 3003:
			str = "AUTH_API_SERVER_ERROR";
			break;

		case 3004:
			str = "AUTH_TOKEN_ERROR";
			break;

		case 3005:
			str = "AUTH_URL_RESOLUTION";
			break;
			
		case 3006:
			str = "PARAM_ERROR";
			break;

		default:
			str = "unknown status code: " + statusCode;
			break;
		}
		return str;
	}
}