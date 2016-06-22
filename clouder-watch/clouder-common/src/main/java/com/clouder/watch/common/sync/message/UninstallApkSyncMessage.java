package com.clouder.watch.common.sync.message;

import com.clouder.watch.common.sync.SyncMessage;
import com.clouder.watch.common.sync.SyncMessagePathConfig;

/**
 * Created by yang_shoulai on 10/26/2015.
 */
public class UninstallApkSyncMessage extends SyncMessage {

    private String packageName;


    public UninstallApkSyncMessage(String packageName) {
        super(null, SyncMessagePathConfig.UNINSTALL_APK);
        this.packageName = packageName;

    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }


}
