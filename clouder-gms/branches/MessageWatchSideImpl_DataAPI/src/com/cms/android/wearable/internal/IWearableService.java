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

import android.os.IInterface;
import android.os.RemoteException;

/**
 * ClassName: IWearableService
 * 
 * @description IWearableService
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public interface IWearableService extends IInterface {

	public abstract void sendMessage(String node, String path, byte[] data) throws RemoteException;

}
