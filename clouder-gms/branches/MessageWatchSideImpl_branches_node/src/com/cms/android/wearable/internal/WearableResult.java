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
package com.cms.android.wearable.internal;

import com.cms.android.common.api.Result;
import com.cms.android.common.internal.MobvoiApi;
import com.cms.android.wearable.Wearable;

/**
 * ClassName: WearableResult
 * 
 * @description WearableResult
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public abstract class WearableResult<R extends Result> extends MobvoiApi.ApiResult<R, WearableAdapter> {
	protected WearableResult() {
		super(Wearable.CLIENT_KEY);
	}
}