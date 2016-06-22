package com.cms.android.wearable.service.common;

import java.lang.Thread.UncaughtExceptionHandler;

import android.util.Log;

public class LogTool {
	public static int LOG_LEVEL = 5;

	public static boolean ISSAVEINFILE = false;

	public static void v(String tag, String msg) {
		if (LOG_LEVEL >= 5) {
			Log.v(tag, msg + "");
			saveLog2SDCard("<-verbose->" + tag + msg + "</-verbose->\n");
		}
	}

	public static void v(String tag, String msg, Throwable tr) {
		if (LOG_LEVEL >= 5) {
			Log.v(tag, msg + "", tr);
			saveLog2SDCard("<-verbose->" + tag + msg + tr.getMessage() + "</-verbose->\n");
		}
	}

	public static void d(String tag, String msg) {
		if (LOG_LEVEL >= 4) {
			Log.d(tag, msg + "");
			saveLog2SDCard("<-debug->" + tag + msg + "</-debug->\n");
		}
	}

	public static void d(String tag, String msg, Throwable tr) {
		if (LOG_LEVEL >= 4) {
			Log.d(tag, msg + "", tr);
			saveLog2SDCard("<-debug->" + tag + msg + tr.getMessage() + "</-debug->\n");
		}
	}

	public static void i(String tag, String msg) {
		if (LOG_LEVEL >= 3) {
			Log.i(tag, msg + "");
			saveLog2SDCard("<-info->" + tag + " " + msg + "</-info->\n");
		}
	}

	public static void i(String tag, String msg, Throwable tr) {
		if (LOG_LEVEL >= 3) {
			Log.i(tag, msg + "", tr);
			saveLog2SDCard("<-info->" + tag + " " + msg + tr.getMessage() + "</-info->\n");
		}
	}

	public static void w(String tag, String msg) {
		if (LOG_LEVEL >= 2) {
			Log.w(tag, msg + "");
			saveLog2SDCard("<-warn->" + tag + " " + msg + "</-warn->\n");
		}
	}

	public static void w(String tag, String msg, Throwable tr) {
		if (LOG_LEVEL >= 2) {
			Log.w(tag, msg + "", tr);
			saveLog2SDCard("<-warn->" + tag + " " + msg + tr.getMessage() + "</-warn->\n");
		}
	}

	public static void e(String tag, String msg) {
		if (LOG_LEVEL >= 1) {
			Log.e(tag, msg + "");
			saveLog2SDCard("<-error->" + tag + " " + msg + "</-error->\n");
		}
	}

	public static void e(String tag, String msg, Throwable tr) {
		if (LOG_LEVEL >= 1) {
			Log.e(tag, msg + "", tr);
			saveLog2SDCard("<-error->" + tag + " " + msg + " " + Log.getStackTraceString(tr) + "</-error->\n");
		}
	}

	public static void uncaughtException() {
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(Thread thread, Throwable ex) {
				LogTool.e(this.getClass().getName(), "未捕获异常", ex);
				System.exit(0);
			}
		});
	}

	private static void saveLog2SDCard(String logMsg) {
		if (ISSAVEINFILE) {
			try {
				StorageUtils.saveToSDCard(logMsg);
			} catch (Exception e) {
				e("LogTool.saveLog2SDCard()", e.getMessage());
			}
		}
	}
}
