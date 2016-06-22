package com.clouder.watch.mobile.sync;

import com.cms.android.common.api.MobvoiApiClient;
import com.cms.android.wearable.DataItem;

/**
 * Created by yang_shoulai on 9/8/2015.
 */
public interface IDataListener {

    void onDataChanged(String path, DataItem dataItem,MobvoiApiClient client);
}
