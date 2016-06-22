package com.clouder.watch.common.sync.message;

import com.clouder.watch.common.sync.SyncMessage;
import com.google.gson.Gson;

/**
 * 将手机端/手表端的屏幕锁信息同步至手表/手机端
 * 包含是否启用屏幕锁、屏幕锁密码等
 * Created by yang_shoulai on 8/17/2015.
 */
public class ScreenLockerSyncMessage extends SyncMessage {

    /**
     * 是否启用锁屏
     */
    private boolean enable;

    /**
     * 锁屏的密码
     */
    private String password;

    public ScreenLockerSyncMessage(String packageName, String path) {
        super(packageName, path);
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
