package com.hoperun.download.util;

import java.util.Locale;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class ApplicationContextManager implements ApplicationContextAware {

	private static ApplicationContext mContext;

	private static Locale mLocale;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		mContext = applicationContext;
	}

	public static ApplicationContext getApplicationContext() {
		return mContext;
	}

	public static void setLocale(Locale locale) {
		mLocale = locale;
	}

	public static Locale getLocale() {
		return mLocale;
	}
}
