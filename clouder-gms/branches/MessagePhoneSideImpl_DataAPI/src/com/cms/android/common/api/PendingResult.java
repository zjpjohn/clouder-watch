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

import java.util.concurrent.TimeUnit;

/**
 * ClassName: PendingResult
 * 
 * @description PendingResult
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public abstract interface PendingResult<R extends Result> {
	public abstract R await();

	public abstract R await(long time, TimeUnit timeUnit);

	public abstract void setResultCallback(ResultCallback<R> resultCallback);
}