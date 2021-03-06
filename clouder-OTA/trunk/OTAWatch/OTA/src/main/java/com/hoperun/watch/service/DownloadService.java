package com.hoperun.watch.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.hoperun.watch.activity.ConfirmActivity;
import com.hoperun.watch.activity.RestartActivity;
import com.hoperun.watch.download.IDownload;
import com.hoperun.watch.download.IDownloadEventCallback;
import com.hoperun.watch.utils.HttpUtils;
import com.hoperun.watch.utils.MD5Utils;
import com.hoperun.watch.utils.Utils;
import com.hoperun.watch.vo.AppInfo;
import com.hoperun.watch.vo.enums.EServiceType;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class DownloadService extends Service {

    private static final String TAG = "DownloadService";

    /**
     * 轮询间隔时间
     */
    private static final int INTERVAL = 60 * 60 * 1000 * 24;

    /**
     * OTA轮询间隔时间
     */
    private static final int FIRMWARE_INTERVAL = 60 * 60 * 1000 * 24;

    private static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static final String INTENT = "com.hoperun.watch.downloadservice";

    private static final int DOWNLOAD_FIRMWARE = 2;

    private volatile boolean IS_DOWNLOAD_FIRMWARE = true;

    private HttpUtils httpUtils = new HttpUtils();

    @SuppressLint("SimpleDateFormat")
    private SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT);

    /**
     * 服务开始时间
     */
    private long startTime = 0;

    private String ClOUD_WATCH_DIRECTORY;

    private AppInfo currentFirmwareAppInfo;

    private File saveFirmwareFile;

    private BroadcastReceiver receiver;

    private BroadcastReceiver mPackageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.PACKAGE_ADDED")) {
                String data = intent.getDataString();
                String packageName = data.substring(data.indexOf(":") + 1);
                Log.d(TAG, "installed packageName :" + packageName);
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
        ClOUD_WATCH_DIRECTORY = Environment.getExternalStorageDirectory() + File.separator;
        Utils.updateFirmwareFullDownFlag(DownloadService.this, false);
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {

            @SuppressLint("SimpleDateFormat")
            @Override
            public void run() {
                startTime += INTERVAL;
                Log.d(TAG, "当前轮询时间" + sdf.format(new Date()) + " 已启动时间:" + (startTime - INTERVAL));

                //if (startTime != 0 && startTime % FIRMWARE_INTERVAL == 0 && IS_DOWNLOAD_FIRMWARE) {
                checkFireWareVersion();
                //}

            }
        }, new Date(), INTERVAL);

        IntentFilter filter = new IntentFilter();
        filter.addAction("InstallFirmware");
        filter.addAction("CancelFirmware");
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("InstallFirmware".equals(action)) {
                    Log.d(TAG, "Start download and install");
                    upgrade();
                } else if ("CancelFirmware".equals(action)) {
                    Log.d(TAG, "Cancel download and install");
                    IS_DOWNLOAD_FIRMWARE = true;
                }
            }
        };
        registerReceiver(receiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy...");
        unregisterReceiver(receiver);
    }

    private void checkFireWareVersion() {
        try {
            currentFirmwareAppInfo = httpUtils.queryFirmwareVersionNo();
            if (currentFirmwareAppInfo != null) {
                Log.i(TAG, "firmware is " + currentFirmwareAppInfo.toString());
                saveFirmwareFile = checkFile(ClOUD_WATCH_DIRECTORY + currentFirmwareAppInfo.getFileName(), currentFirmwareAppInfo.getFileName());
                Log.i(TAG, "current :" + Utils.getFirmwareVersionNo() + " download : " + currentFirmwareAppInfo.getVersionCode());
                Log.i(TAG, "Boolean :" + (Utils.getFirmwareVersionNo() < currentFirmwareAppInfo.getVersionCode()));
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
            Log.d(TAG, "file can not write:");
            throw new Exception("can't write into the directory " + parent.getAbsolutePath());
        }
        if (saveFile.exists()) {
            if (!saveFile.canWrite()) {
                Log.d(TAG, "saveFile can not write:");
                throw new Exception("can't write into the file " + saveFile.getAbsolutePath());
            }
        }

        return saveFile;
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
            if (msg.what == DOWNLOAD_FIRMWARE) {
                IS_DOWNLOAD_FIRMWARE = false;
                Intent intent = new Intent(getApplicationContext(), ConfirmActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }

        ;
    };

    private void upgrade() {
        if (Utils.isFirmwareFullDownload(DownloadService.this)) {
            // 完全下载则直接进行安装
            Log.d(TAG, "direct to install firmware");
            install();
        } else {
            new Thread(downloadFirmware).start();
        }
    }

    private void install() {
        try {
            Utils.installPackage(DownloadService.this, saveFirmwareFile.getAbsolutePath());

            // 安装完成后重启 TODO
            Intent cIntent = new Intent(getApplicationContext(), RestartActivity.class);
            cIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(cIntent);

        } catch (IOException e) {
            Log.d(TAG, "Install package fails!");
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            Log.d(TAG, "Validation package fails!");
            e.printStackTrace();
        }
    }

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
                                    install();
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
