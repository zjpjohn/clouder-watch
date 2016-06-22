package com.hoperun.watch;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager;

import com.hoperun.watch.download.IDownload;
import com.hoperun.watch.download.IDownloadEventCallback;
import com.hoperun.watch.utils.MD5Utils;
import com.hoperun.watch.utils.Utils;
import com.hoperun.watch.vo.AppInfo;
import com.hoperun.watch.vo.enums.EServiceType;

public class DownloadService extends Service {

	private static final String TAG = "DownloadService";

	/**
	 * 轮询间隔时间
	 */
	private static final int INTERVAL = 10 * 1000;

	/**
	 * OTA轮询间隔时间
	 */
	private static final int FIRMWARE_INTERVAL = 60 * 1000;

	/**
	 * APK轮询间隔时间
	 */
	private static final int APK_INTERVAL = 30 * 1000;

	private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

	public static final String INTENT = "com.hoperun.watch.downloadservice";
	
	private static final int DOWNLOAD_APK = 1;

	private static final int DOWNLOAD_FIRMWARE = 2;
	
	private volatile boolean IS_DOWNLOAD_APK = true;

	private volatile boolean IS_DOWNLOAD_FIRMWARE = true;

	private HttpUtils httpUtils = new HttpUtils();

	@SuppressLint("SimpleDateFormat")
	private SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT);

	/**
	 * 服务开始时间
	 */
	private long startTime = 0;

	private String ClOUD_WATCH_DIRECTORY;

	private AppInfo currentApkAppInfo;

	private File saveApkFile;

	private AppInfo currentFirmwareAppInfo;

	private File saveFirmwareFile;

	private BroadcastReceiver mPackageReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals("android.intent.action.PACKAGE_ADDED")) {
				String data = intent.getDataString();
				String packageName = data.substring(data.indexOf(":") + 1);
				Log.d(TAG, "installed packageName :" + packageName);
				if (currentApkAppInfo != null && packageName.contains(currentApkAppInfo.getPackageName())) {
					Log.i(TAG, "reset download config and delete file " + saveApkFile.delete());
					Utils.resetDownloadFlag(DownloadService.this);
				}
			} else if (intent.getAction().equals("android.intent.action.PACKAGE_REMOVED")) {
				String packageName = intent.getDataString();
				Log.d(TAG, "uninstalled apk :" + packageName);
			}
			unRegisterPackageReceiver();
		}
	};

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate...");
		// ClOUD_WATCH_DIRECTORY = getFilesDir().getAbsolutePath() +
		// File.separator + "CloudWatch" + File.separator;
		ClOUD_WATCH_DIRECTORY = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
				+ "CloudWatch" + File.separator;

		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {

			@SuppressLint("SimpleDateFormat")
			@Override
			public void run() {
				startTime += INTERVAL;
				Log.d(TAG, "当前轮询时间：" + sdf.format(new Date()) + " 已启动时间：" + (startTime - 10 * 1000));
				if (startTime != 0 && startTime % APK_INTERVAL == 0 && IS_DOWNLOAD_APK) {
					downloadApk();
				}

				if (startTime != 0 && startTime % FIRMWARE_INTERVAL == 0 && IS_DOWNLOAD_FIRMWARE) {
					downloadFireWare();
				}

			}
		}, new Date(), INTERVAL);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy...");
	}

	private void downloadApk() {
		try {
			currentApkAppInfo = httpUtils.queryApkVersionNo();
			if (currentApkAppInfo != null) {
				Log.i(TAG, "apk is " + currentApkAppInfo.toString());
				saveApkFile = checkFile(ClOUD_WATCH_DIRECTORY + currentApkAppInfo.getFileName(), currentApkAppInfo.getPackageName());

				PackageInfo info = Utils.getPackageInfo(DownloadService.this, currentApkAppInfo.getPackageName());
				if (info != null) {
					Log.d(TAG, "installed packageName:" + info.packageName + " versionName:" + info.versionName
							+ " versionCode:" + info.versionCode);
				}

				if (info == null || info.versionCode < currentApkAppInfo.getVersionCode()) {
					Log.i(TAG, "The app doesn't exist in the phone or remote app version is higher than the local app.");
					
					Message msg = new Message();
					msg.what = DOWNLOAD_APK;
					mHandler.sendMessage(msg);					
				} else {
					Log.e(TAG, "The remote app version isn't higher than the local app.");
				}

			} else {
				Log.e(TAG, "response appinfo is null.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void downloadFireWare() {
		try {
			currentFirmwareAppInfo = httpUtils.queryFirmwareVersionNo();
			if (currentFirmwareAppInfo != null) {
				Log.i(TAG, "firmware is " + currentFirmwareAppInfo.toString());
				saveFirmwareFile = checkFile(ClOUD_WATCH_DIRECTORY + currentFirmwareAppInfo.getFileName(), currentFirmwareAppInfo.getFileName());

				if (Utils.getFirmwareVersionNo() < currentFirmwareAppInfo.getVersionCode()) {
					Log.i(TAG, "The firmware version is higher than the local firmware.");

					Message msg = new Message();
					msg.what = DOWNLOAD_FIRMWARE;
					mHandler.sendMessage(msg);					
				} else {
					Log.e(TAG, "The remote firmware version isn't higher than the local firmware.");
				}

			} else {
				Log.e(TAG, "response appinfo is null.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private File checkFile(String filePath, String fileName) throws Exception {
		File saveFile = new File(ClOUD_WATCH_DIRECTORY + fileName);
		Log.d(TAG,
				"cloud watch directory is " + ClOUD_WATCH_DIRECTORY + " save file path is "
						+ saveFile.getAbsolutePath());
		File parent = saveFile.getParentFile();
		if (!parent.exists()) {
			Log.d(TAG, "create directory " + parent.getAbsolutePath());
			boolean isParent = parent.mkdir();
			Log.d(TAG, "isParent:" + isParent);
		}
		if (!parent.canWrite()) {
			throw new Exception("can't write into the directory " + parent.getAbsolutePath());
		}
		if (saveFile.exists()) {
			if (!saveFile.canWrite()) {
				throw new Exception("can't write into the file " + saveFile.getAbsolutePath());
			}
		}

		return saveFile;
	}

	private void registerPackageReceiver() {
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.intent.action.PACKAGE_ADDED");
		filter.addAction("android.intent.action.PACKAGE_REMOVED");
		filter.addDataScheme("package");
		registerReceiver(mPackageReceiver, filter);
	}

	private void unRegisterPackageReceiver() {
		if (mPackageReceiver != null) {
			unregisterReceiver(mPackageReceiver);
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {

		public void handleMessage(Message msg) {
			switch (msg.what) {

			case DOWNLOAD_APK: {

				IS_DOWNLOAD_APK = false;

				Builder builder = new Builder(getApplicationContext());
				builder.setTitle("升级");
				builder.setMessage("是否下载最新版本");

				builder.setNegativeButton("取消", new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						IS_DOWNLOAD_APK = true;
					}

				});

				builder.setPositiveButton("确定", new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						
						if (Utils.isFullDownload(DownloadService.this)) {
							// 完全下载则直接进行安装
							Log.d(TAG, "direct to install apk");
							registerPackageReceiver();
							Utils.install(DownloadService.this, saveApkFile.getAbsolutePath());
							
						} else {
							new Thread(downloadApk).start();
						}
					}

				});

				Dialog dialog = builder.create();
				dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
				dialog.show();
				break;
			}

			case DOWNLOAD_FIRMWARE: {

				IS_DOWNLOAD_FIRMWARE = false;

				Builder builder = new Builder(getApplicationContext());
				builder.setTitle("升级");
				builder.setMessage("是否下载最新固件");

				builder.setNegativeButton("取消", new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						IS_DOWNLOAD_FIRMWARE = true;
					}

				});

				builder.setPositiveButton("确定", new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						
						if (Utils.isFirmwareFullDownload(DownloadService.this)) {
							// 完全下载则直接进行安装
							Log.d(TAG, "direct to install firmware");
							
							try {
								Utils.installPackage(DownloadService.this, saveFirmwareFile.getAbsolutePath());
							} catch (IOException e) {
								Log.d(TAG, "Install package fails!");
								e.printStackTrace();
							} catch (GeneralSecurityException e) {
								Log.d(TAG, "Validation package fails!");
								e.printStackTrace();
							}
							
						} else {
							new Thread(downloadFirmware).start();
						}
					}
				});
				
				Dialog dialog = builder.create();
				dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
				dialog.show();
				break;
			}

			default : break;
			
			}
		};
	};

	Runnable downloadApk = new Runnable() {
		@Override
		public void run() {
			try {
				// 检查和创建File
				if (!saveApkFile.exists()) {
					boolean isCreate = saveApkFile.createNewFile();
					Log.d(TAG, "isCreate:" + isCreate);
				}
				long length = saveApkFile.length();
				Log.d(TAG, "save file current length:" + length);
				httpUtils.download(EServiceType.DOWNLOAD, "{\"type\":\"apk\"}", length,
						currentApkAppInfo.getFileLength(), saveApkFile.getAbsolutePath(), new IDownloadEventCallback() {

							@Override
							public void onDownloading(IDownload download) {
								Log.d(TAG, "Apk onDownloading:" + download.toString());
							}

							@Override
							public void onDownloadError(IDownload download) {
								Log.d(TAG, "onDownloadError:" + download.toString());
								IS_DOWNLOAD_APK = true;
							}

							@Override
							public void onDownloadCompeleted(IDownload download) {
								Log.d(TAG, "onDownloadCompeleted:" + download.toString());
								String checksum = MD5Utils.md5sum(download.getSaveFilePath());
								Log.d(TAG, String.format("The local checksum is %s and queryVersionNo checksum is %s.",
										checksum, currentApkAppInfo.getChecksum()));
								if (currentApkAppInfo.getChecksum().equalsIgnoreCase(checksum)) {
									Utils.updateFullDownFlag(DownloadService.this, true);
									Log.d(TAG, "start to install app.");
									registerPackageReceiver();
									Utils.install(DownloadService.this, saveApkFile.getAbsolutePath());
								} else {
									Log.i(TAG, String.format("Delete wrong file %s.", saveApkFile.delete() ? "succeed"
											: "failed"));
								}
								IS_DOWNLOAD_APK = true;
							}

							@Override
							public void onDownloadCanceled(IDownload download) {
								Log.d(TAG, "onDownloadCanceled:" + download.toString());
								IS_DOWNLOAD_APK = true;
							}
						});
			} catch (Exception e) {
				e.printStackTrace();
				IS_DOWNLOAD_APK = true;
			}
		}
	};

	Runnable downloadFirmware = new Runnable() {

		@Override
		public void run() {
			try {
				// 检查和创建File
				if (!saveFirmwareFile.exists()) {
					boolean isCreate = saveFirmwareFile.createNewFile();
					Log.d(TAG, "saveFirmwareFile isCreate:" + isCreate);
				}
				long length = saveFirmwareFile.length();
				Log.d(TAG, "save firmwareFile current length:" + length);
				httpUtils.download(EServiceType.DOWNLOAD, "{\"type\":\"firmware\"}", length,
						currentFirmwareAppInfo.getFileLength(), saveFirmwareFile.getAbsolutePath(),
						new IDownloadEventCallback() {

							@Override
							public void onDownloading(IDownload download) {
								Log.d(TAG, "Firmware onDownloading:" + download.toString());
							}

							@Override
							public void onDownloadError(IDownload download) {
								Log.d(TAG, "onDownloadError:" + download.toString());
								IS_DOWNLOAD_FIRMWARE = true;
							}

							@Override
							public void onDownloadCompeleted(IDownload download) {
								Log.d(TAG, "onDownloadCompeleted:" + download.toString());
								String checksum = MD5Utils.md5sum(download.getSaveFilePath());
								Log.d(TAG, String.format("The local checksum is %s and queryVersionNo checksum is %s.",
										checksum, currentFirmwareAppInfo.getChecksum()));
								if (currentFirmwareAppInfo.getChecksum().equalsIgnoreCase(checksum)) {
									Utils.updateFirmwareFullDownFlag(DownloadService.this, true);
									Log.d(TAG, "start to install firmware.");
									
									try {
										Utils.installPackage(DownloadService.this, saveFirmwareFile.getAbsolutePath());
									} catch (IOException e) {
										Log.d(TAG, "Install package fails!");
										e.printStackTrace();
									} catch (GeneralSecurityException e) {
										Log.d(TAG, "Validation package fails!");
										e.printStackTrace();
									}

								} else {
									Log.i(TAG, String.format("Delete wrong firmware %s.",
											saveFirmwareFile.delete() ? "succeed" : "failed"));
								}
								IS_DOWNLOAD_FIRMWARE = true;
							}

							@Override
							public void onDownloadCanceled(IDownload download) {
								Log.d(TAG, "onDownloadCanceled:" + download.toString());
								IS_DOWNLOAD_FIRMWARE = true;
							}
						});
			} catch (Exception e) {
				e.printStackTrace();
				IS_DOWNLOAD_FIRMWARE = true;
			}
		}
	};
}
