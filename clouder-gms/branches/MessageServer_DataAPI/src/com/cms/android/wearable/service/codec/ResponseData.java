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


 /**
 * ClassName: ResponseData
 *
 * @description
 * @author xing_peng
 * @Date 2015-8-20
 * 
 */
public class ResponseData {

	public final static byte RESPONSE_STATUS_FAIL = 0;
	public final static byte RESPONSE_STATUS_SUCCESS = 1;
	public final static byte RESPONSE_STATUS_TOTAL_SUCCESS = 2;
	
	private String uuid;
	private int status;

	public ResponseData() {
		super();
	}
	
	public ResponseData(String uuid, int status) {
		super();
		this.uuid = uuid;
		this.status = status;
	}
	
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return "ResponseData [uuid=" + uuid + ", status=" + status + "]";
	}

}
