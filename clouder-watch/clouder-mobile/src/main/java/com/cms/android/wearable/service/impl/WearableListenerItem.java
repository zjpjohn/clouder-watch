package com.cms.android.wearable.service.impl;

import com.cms.android.wearable.service.common.LogTool;

/**
 * Created by yang_shoulai on 11/17/2015.
 */
public class WearableListenerItem {

    private static final String TAG = "WearableListenerItem";
    private String packageName;

    private IWearableListener listener;

    public WearableListenerItem() {
        super();
    }

    public WearableListenerItem(String packageName, IWearableListener listener) {
        super();
        LogTool.d(TAG, "WearableListenerItem constructor ->packageName = " + packageName + " listener = "
                + listener);
        this.packageName = packageName;
        this.listener = listener;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public IWearableListener getListener() {
        return listener;
    }

    public void setListener(IWearableListener listener) {
        this.listener = listener;
    }
}
