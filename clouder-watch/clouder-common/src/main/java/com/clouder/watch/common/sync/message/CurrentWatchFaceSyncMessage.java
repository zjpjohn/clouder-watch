package com.clouder.watch.common.sync.message;

import com.clouder.watch.common.sync.SyncMessage;
import com.google.gson.Gson;

/**
 * 在手表端：
 * 将当前的已经在使用的表盘信息发送至手机app
 * 在手机端：
 * 手机Clouder app端可以设置手表的表盘
 * 当用户选择了一个表盘时将选中的表盘发送至手表，手表端应该立即使用该表盘
 * Created by yang_shoulai on 8/17/2015.
 */
public class CurrentWatchFaceSyncMessage extends SyncMessage {


    /**
     * 当前正在使用表盘的应用包名
     */
    private String packageName;

    /**
     * 当前正在使用的表盘的服务名
     */
    private String serviceName;


    public CurrentWatchFaceSyncMessage(String packageName, String path) {
        super(packageName, path);
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

}
