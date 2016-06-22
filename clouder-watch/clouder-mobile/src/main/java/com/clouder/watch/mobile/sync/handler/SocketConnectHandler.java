package com.clouder.watch.mobile.sync.handler;

import com.clouder.watch.common.sync.IHandler;
import com.clouder.watch.common.sync.IMessageListener;
import com.clouder.watch.common.sync.SyncMessage;
import com.clouder.watch.common.sync.message.SocketConnectSyncMessage;
import com.clouder.watch.mobile.SyncService;

/**
 * Created by yang_shoulai on 2015/10/11.
 */
public class SocketConnectHandler implements IHandler<SocketConnectSyncMessage>, IMessageListener {

    private SyncService syncService;

    public SocketConnectHandler(SyncService context) {
        this.syncService = context;
    }

    @Override
    public void handle(String path, SocketConnectSyncMessage message) {
        syncService.sendMessage(message);
    }

    @Override
    public void onMessageReceived(String path, SyncMessage message) {
        handle(path, (SocketConnectSyncMessage) message);
    }
}