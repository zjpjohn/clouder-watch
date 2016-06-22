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

import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.common.api.PendingResult;
import com.cms.android.common.api.Result;
import com.cms.android.common.api.Status;

/**
 * ClassName: MessageApi
 * 
 * @description MessageApi
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public abstract interface MessageApi {
	public abstract PendingResult<SendMessageResult> sendMessage(MobvoiApiClient mobvoiApiClient, String node,
			String path, byte[] data);

	public abstract PendingResult<Status> addListener(MobvoiApiClient mobvoiApiClient, MessageApi.MessageListener listener);
	
	public abstract PendingResult<Status> removeListener(MobvoiApiClient mobvoiApiClient, MessageApi.MessageListener listener);

	public static abstract interface MessageListener {
		public abstract void onMessageReceived(MessageEvent messageEvent);
	}

	public static abstract interface SendMessageResult extends Result {
	}

}