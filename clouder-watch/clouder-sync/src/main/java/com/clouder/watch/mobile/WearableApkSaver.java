package com.clouder.watch.mobile;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;

import com.clouder.watch.common.utils.Constant;
import com.clouder.watch.common.utils.StringUtils;
import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.wearable.Asset;
import com.cms.android.wearable.DataMapItem;
import com.cms.android.wearable.Wearable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by yang_shoulai on 9/8/2015.
 */
public class WearableApkSaver extends Thread {

    private static final String TAG = "WearableApkSaver";

    public static final String SDCARD_PATH = Environment.getExternalStorageDirectory().getPath() +
            File.separator + "ClouderWatch" + File.separator + "SyncApp";

    private MobvoiApiClient client;

    private DataMapItem dataMapItem;

    private Context context;

    public WearableApkSaver(Context context, MobvoiApiClient client, DataMapItem dataMapItem) {
        this.client = client;
        this.dataMapItem = dataMapItem;
        this.context = context;
    }

    @Override
    public void run() {
        String appName = dataMapItem.getDataMap().getString("name");
        String packageName = dataMapItem.getDataMap().getString("packageName");
        int versionCode = Integer.parseInt(dataMapItem.getDataMap().getString("versionCode"));
        String versionName = dataMapItem.getDataMap().getString("versionName");
        Asset asset = dataMapItem.getDataMap().getAsset("apk");
        Log.e(TAG, String.format("Wearable App name [%s], package [%s], version code [%s],version name [%s].", appName, packageName, versionCode, versionName));
        if (asset == null || StringUtils.isEmpty(appName) || StringUtils.isEmpty(packageName)) {
            Log.e(TAG, "wearable应用安装包在data api asset中不存在！");
            return;
        }
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Log.e(TAG, "SD 卡不可用！");
            return;
        }
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(client, asset).await().getInputStream();
        if (assetInputStream == null) {
            Log.e(TAG, "Data api 获取asset流失败！");
            return;
        }
        PackageInfo packageInfo = loadPackageInfo(context, packageName);
        if (packageInfo == null || packageInfo.versionCode < versionCode) {
            String folderName = packageName + "-" + versionCode;
            String folderPath = SDCARD_PATH + File.separator + folderName;
            String apkName = appName + ".apk";
            File folder = new File(folderPath);
            if (!folder.exists()) {
                if (!folder.mkdirs()) {
                    Log.w(TAG, "can not make dir");
                }
            }
            File file = new File(folderPath, apkName);
            if (file.exists()) {
                Log.d(TAG, "检测到文件" + file.getName() + "已经存在，将要先删除以前的文件！");
                boolean success = file.delete();
                if (success) {
                    Log.d(TAG, "删除文件" + file.getName() + "成功！");

                } else {
                    Log.e(TAG, "保存文件失败，" + file.getName() + "已经存在并且删除失败！");
                    return;
                }
            }
            FileOutputStream fos = null;
            try {
                if (!file.createNewFile()) {
                    Log.d(TAG, "can not create new file " + file);
                }
                fos = new FileOutputStream(file);
                byte[] buffer = new byte[1024];
                int len;
                int total = 0;
                while ((len = assetInputStream.read(buffer)) != -1) {
                    total += len;
                    fos.write(buffer, 0, len);
                }
                Log.e(TAG, "Wearable应用[" + packageName + ":" + appName + "]保存成功!，大小为[" + total + "]个字节！");
                //通知安装服务进行安装
                Intent installService = new Intent();
                installService.setComponent(new ComponentName(Constant.CLOUDER_INSTALLER_PKG, Constant.CLOUDER_INSTALLER_SERVICE));
                installService.putExtra("apk_file_path", file.getAbsolutePath());
                installService.putExtra("package", packageName);
                installService.putExtra("install", true);
                try {
                    context.startService(installService);
                } catch (Exception e) {
                    Log.e(TAG, "Error when start installer service!", e);
                }

            } catch (IOException e) {
                Log.e(TAG, "IOException occurs when saving wearable apk file!", e);
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            Log.d(TAG, String.format("应用【%s】已经安装", packageName));
        }
        try {
            assetInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private PackageInfo loadPackageInfo(Context context, String packageName) {
        try {
            return context.getPackageManager().getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }


}
