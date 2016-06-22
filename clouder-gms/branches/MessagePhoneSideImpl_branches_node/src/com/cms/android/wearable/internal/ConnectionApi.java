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

import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.common.api.PendingResult;
import com.cms.android.common.api.Result;

/**
 * ClassName: ConnectionApi
 * 
 * @description ConnectionApi
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public abstract interface ConnectionApi {
	public abstract PendingResult<GetConfigResult> getConfig(MobvoiApiClient paramMobvoiApiClient);

	public static abstract interface GetConfigResult extends Result {
		public abstract ConnectionConfiguration getConfig();
	}
}