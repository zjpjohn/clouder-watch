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

import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.HashSet;
import java.util.Set;

import android.util.Log;

import com.cms.android.common.internal.proxy.Loadable;

/**
 * ClassName: MobvoiApiManager
 * 
 * @description MobvoiApiManager
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public class MobvoiApiManager {
	private static final String[] GOOGLE_SERVICE_PACKAGES;
	private static final String[] GOOGLE_SERVICE_SIGNATURES;
	private static MobvoiApiManager INSTANCE;
	private static final String[] MOBVOI_SERVICE_PACKAGES = { "com.mobvoi.android", "com.mobvoi.companion" };
	private static final String[] MOBVOI_SERVICE_SIGNATURES = { "", "" };
	private static CertificateFactory certFactory;
	private ApiGroup group = ApiGroup.MMS;
	private boolean init = false;
	private Set<Loadable> proxies = new HashSet<Loadable>();

	static {
		GOOGLE_SERVICE_PACKAGES = new String[] { "com.google.android.gms" };
		GOOGLE_SERVICE_SIGNATURES = new String[] { "" };
		try {
			certFactory = CertificateFactory.getInstance("X.509");
		} catch (CertificateException e) {
			e.printStackTrace();
		}
	}

	public static MobvoiApiManager getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new MobvoiApiManager();
		}
		return INSTANCE;
	}

	public ApiGroup getGroup() {
		return this.group;
	}

	public void registerProxy(Loadable loadable) {
		Log.d("MobvoiApiManager", "register proxy " + loadable.getClass().getSimpleName());
		this.proxies.add(loadable);
	}

	public static enum ApiGroup {
		MMS("MMS", 0), GMS("GMS", 1), NONE("NONE", 2);

		private String name;

		private int value;

		private ApiGroup(String name, int value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public int getValue() {
			return value;
		}

	}
}