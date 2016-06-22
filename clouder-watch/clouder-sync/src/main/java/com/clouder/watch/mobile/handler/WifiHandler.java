package com.clouder.watch.mobile.handler;

import com.clouder.watch.common.sync.IHandler;
import com.clouder.watch.common.sync.IMessageListener;
import com.clouder.watch.common.sync.SyncMessage;
import com.clouder.watch.common.sync.message.WifiSyncMessage;

/**
 * wifi信息同步处理
 * Created by yang_shoulai on 8/17/2015.
 */
public class WifiHandler implements IHandler<WifiSyncMessage>, IMessageListener {

    @Override
    public void handle(String path, WifiSyncMessage message) {

    }

    @Override
    public void onMessageReceived(String path, SyncMessage message) {
        handle(path, (WifiSyncMessage) message);
    }
}
