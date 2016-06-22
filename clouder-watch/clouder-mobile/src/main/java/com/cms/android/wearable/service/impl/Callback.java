package com.cms.android.wearable.service.impl;

/**
 * Created by yang_shoulai on 11/14/2015.
 */
public class Callback {
    public static final int TYPE_MESSAGE = 1;

    public static final int TYPE_DATA = 2;

    private int type;

    private IBLECentralCallback callback;

    public Callback(int type, IBLECentralCallback callback) {
        super();
        this.type = type;
        this.callback = callback;
    }

    public int getType() {
        return type;
    }

    public IBLECentralCallback getCallback() {
        return callback;
    }
}
