package com.clouder.watch.mobile.sync.handler;

import android.content.Intent;

import com.clouder.watch.common.sync.IHandler;
import com.clouder.watch.common.sync.IMessageListener;
import com.clouder.watch.common.sync.SyncMessage;
import com.clouder.watch.common.sync.message.WifiSyncMessage;
import com.clouder.watch.mobile.SyncService;
import com.clouder.watch.mobile.activity.SyncWifiActivity;

/**
 * Created by yang_shoulai on 9/6/2015.
 */
public class WifiSyncHandler implements IHandler<WifiSyncMessage>, IMessageListener {


    private SyncService syncService;

    public WifiSyncHandler(SyncService service) {
        this.syncService = service;
    }

    @Override
    public void handle(String path, WifiSyncMessage message) {
        if (message.getMethod() == SyncMessage.Method.Get) {
            Intent intent = new Intent(syncService, SyncWifiActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            syncService.startActivity(intent);
        }
    }

    @Override
    public void onMessageReceived(String path, SyncMessage message) {
        handle(path, (WifiSyncMessage) message);
    }
}
