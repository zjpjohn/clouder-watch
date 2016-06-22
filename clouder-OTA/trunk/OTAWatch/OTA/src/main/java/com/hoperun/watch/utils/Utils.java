package com.hoperun.watch.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.RecoverySystem;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

public class Utils {

	public static PackageInfo getPackageInfo(Context context, String packageName) {
		PackageManager pckMan = context.getPackageManager();
		List<PackageInfo> packageInfo = pckMan.getInstalledPackages(0);
		for (PackageInfo info : packageInfo) {
			if (info.packageName.equalsIgnoreCase(packageName)) {
				return info;
			}
		}
		return null;
	}

	public static int getFirmwareVersionNo() {
		int sdk = Build.VERSION.SDK_INT;
		return sdk;
	}

	public static String getFirmwareVersionName() {
		String Firmwareversion = Build.VERSION.RELEASE;
		return Firmwareversion;
	}

	public static boolean isFullDownload(Context context) {
		return SharePreUtils.getDataBoolean(context, "ClouderWatch", "isFullDownload", false);
	}

	public static void updateFullDownFlag(Context context, boolean flag) {
		SharePreUtils.saveData(context, "ClouderWatch", "isFullDownload", Boolean.valueOf(flag));
	}

	public static boolean isFullInstall(Context context) {
		return SharePreUtils.getDataBoolean(context, "ClouderWatch", "isFullInstall", false);
	}

	public static void updateFullInstallFlag(Context context, boolean flag) {
		SharePreUtils.saveData(context, "ClouderWatch", "isFullInstall", Boolean.valueOf(flag));
	}

	public static boolean isFirmwareFullDownload(Context context) {
		return SharePreUtils.getDataBoolean(context, "ClouderWatch", "isFirmwareFullDownload", false);
	}

	public static void updateFirmwareFullDownFlag(Context context, boolean flag) {
		SharePreUtils.saveData(context, "ClouderWatch", "isFirmwareFullDownload", Boolean.valueOf(flag));
	}

	public static boolean isFirmwareFullInstall(Context context) {
		return SharePreUtils.getDataBoolean(context, "ClouderWatch", "isFirmwareFullInstall", false);
	}

	public static void updateFirmwareFullInstallFlag(Context context, boolean flag) {
		SharePreUtils.saveData(context, "ClouderWatch", "isFirmwareFullInstall", Boolean.valueOf(flag));
	}

	public static void resetDownloadFlag(Context context) {
		updateFullDownFlag(context, false);
		updateFullInstallFlag(context, false);
	}

	public static void install(Context context, String apkPath) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setDataAndType(Uri.fromFile(new File(apkPath)), "application/vnd.android.package-archive");
		context.startActivity(intent);
	}

	public static void installPackage(Context context, String apkPath) throws IOException, GeneralSecurityException {
		// 如果安装失败，需要将升级包放在cache目录下
		File packageFile = new File(apkPath);
		// 验证固件
		RecoverySystem.verifyPackage(packageFile, null, null);
		// 安装固件
		RecoverySystem.installPackage(context, packageFile);
	}

}
