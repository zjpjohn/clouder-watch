package com.clouder.watch.common.sync.message;

import com.clouder.watch.common.sync.SyncMessage;
import com.google.gson.Gson;

/**
 * 将手机端的通知设置发送至手表端
 * 或者将手表已经设置的通知设置发送至手机端
 * 包含是否启用全局的通知推送、通知到达时是否震动等
 * Created by yang_shoulai on 8/17/2015.
 */
public class NotificationSettingsSyncMessage extends SyncMessage {

    /**
     * 是否打开全局通知使用开关
     * 当全局通知使用开关打开时，所有处于非黑名单中的应用可以接受并显示通知
     */
    private boolean enable;


    /**
     * 当通知到达的时候是否震动
     */
    private boolean shock;

    public NotificationSettingsSyncMessage(String packageName, String path) {
        super(packageName, path);
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public boolean isShock() {
        return shock;
    }

    public void setShock(boolean shock) {
        this.shock = shock;
    }

}
