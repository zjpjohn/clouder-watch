package com.clouder.watch.common.sync.message;

import com.clouder.watch.common.sync.SyncMessage;

/**
 * Created by yang_shoulai on 2015/10/11.
 */
public class SocketConnectSyncMessage extends SyncMessage {
    public SocketConnectSyncMessage(String packageName, String path) {
        super(packageName, path);
    }
}
