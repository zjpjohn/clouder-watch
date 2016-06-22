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
package com.cms.android.common.internal.proxy;

import android.util.Log;

import com.cms.android.common.MobvoiApiManager;
import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.common.api.PendingResult;
import com.cms.android.common.api.Status;
import com.cms.android.wearable.MessageApi;
import com.cms.android.wearable.api.impl.MessageApiImpl;

/**
 * ClassName: MessageApiProxy
 * 
 * @description MessageApiProxy
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public class MessageApiProxy implements Loadable, MessageApi {

	private static final String TAG = "MessageApiProxy";

	private MessageApi instance;

	public MessageApiProxy() {
		MobvoiApiManager.getInstance().registerProxy(this);
		load();
	}

	public void load() {
		if (MobvoiApiManager.getInstance().getGroup() == MobvoiApiManager.ApiGroup.MMS) {
			this.instance = new MessageApiImpl();
		}
		Log.d(TAG, "load message api success.");
	}

	public PendingResult<MessageApi.SendMessageResult> sendMessage(MobvoiApiClient mobvoiApiClient, String node,
			String path, byte[] data) {
		Log.d(TAG, "MessageApiProxy#sendMessage()");
		return this.instance.sendMessage(mobvoiApiClient, node, path, data);
	}

	@Override
	public PendingResult<Status> addListener(MobvoiApiClient mobvoiApiClient, MessageApi.MessageListener listener) {
		return this.instance.addListener(mobvoiApiClient, listener);
	}

	@Override
	public PendingResult<Status> removeListener(MobvoiApiClient mobvoiApiClient, MessageListener listener) {
		return this.instance.removeListener(mobvoiApiClient, listener);
	}
}