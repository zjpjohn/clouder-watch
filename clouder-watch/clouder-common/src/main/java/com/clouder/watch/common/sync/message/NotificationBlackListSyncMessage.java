package com.clouder.watch.common.sync.message;

import com.clouder.watch.common.sync.SyncMessage;
import com.google.gson.Gson;

import java.util.List;

/**
 * 处于通知黑名单中的app信息
 * Created by yang_shoulai on 8/17/2015.
 */
public class NotificationBlackListSyncMessage extends SyncMessage {

    /**
     * 所有处于通知使用黑名单中的应用的包名
     */
    private List<String> list;

    public NotificationBlackListSyncMessage(String packageName, String path) {
        super(packageName, path);
    }

    public List<String> getList() {
        return list;
    }

    public void setList(List<String> list) {
        this.list = list;
    }

}
