package com.clouder.watch.common.sync.message;

import com.clouder.watch.common.sync.SyncMessage;
import com.google.gson.Gson;

import java.util.List;

/**
 * 将手机端安装的包含手表应用的apk解压出来，并且发送至手表端
 * Created by yang_shoulai on 8/17/2015.
 */

public class WearableApkSyncMessage extends SyncMessage {
    /**
     * apk应用名称
     */
    private String name;

    private String packageName;

    private String versionCode;

    private String versionName;

    /**
     * apk包的字节数据
     */
    private byte[] apk;


    public WearableApkSyncMessage(String packageName, String path) {
        super(packageName, path);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getApk() {
        return apk;
    }

    public void setApk(byte[] apk) {
        this.apk = apk;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(String versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }


}
