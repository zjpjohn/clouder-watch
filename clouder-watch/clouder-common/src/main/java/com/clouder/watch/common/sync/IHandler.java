package com.clouder.watch.common.sync;

/**
 * Created by yang_shoulai on 8/17/2015.
 */
public interface IHandler<T extends SyncMessage> {

    void handle(String path, T message);

}