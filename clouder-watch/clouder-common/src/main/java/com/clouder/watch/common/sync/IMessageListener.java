package com.clouder.watch.common.sync;

/**
 * Created by yang_shoulai on 8/18/2015.
 */
public interface IMessageListener {
    void onMessageReceived(String path, SyncMessage message);
}
