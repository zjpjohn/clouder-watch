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
package com.cms.android.wearable;

/**
 * ClassName: MessageEvent
 * 
 * @description MessageEvent
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public abstract interface MessageEvent {
	public abstract byte[] getData();

	public abstract String getPath();

	public abstract String getSourceNodeId();
}