package com.clouder.watch.mobile.sync;

/**
 * Created by yang_shoulai on 10/10/2015.
 */
public interface INodeListener {
    void onNoNodeConnected();

    void onHasNodesConnected();

    void onGetNodeFailed();
}
