package com.cms.android.wearable.service.impl;

/**
 * Created by yang_shoulai on 11/14/2015.
 */
public class Callback {

    public static final int TYPE_MESSAGE = 1;

    public static final int TYPE_DATA = 2;

    private int type;

    private IBLEPeripheralCallback callback;

    public Callback(int type, IBLEPeripheralCallback callback) {
        super();
        this.type = type;
        this.callback = callback;
    }

    public int getType() {
        return type;
    }

    public IBLEPeripheralCallback getCallback() {
        return callback;
    }
}
