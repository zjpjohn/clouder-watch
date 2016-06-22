package com.hoperun.download.util;

import java.util.Locale;

public class LocaleUtils {

	public static String getCssLocale(Locale locale) {
		
		String lang = locale.getLanguage();
		if (lang.indexOf("zh") > -1) {
			lang = "zh";
		} else if (lang.indexOf("es") > -1) {
			lang = "es";
		} else {
			lang = "en";
		}
		
		return lang;
	}
}
