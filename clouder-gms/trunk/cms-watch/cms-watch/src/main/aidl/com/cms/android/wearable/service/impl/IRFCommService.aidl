package com.cms.android.wearable.service.impl;

import com.cms.android.wearable.service.impl.IRfcommClientCallback;

interface IRFCommService {

    void registerCallback(in IRfcommClientCallback callback);

    void connect(String address);

    void disconnect();

    boolean write(in byte[] bytes);

    boolean isConnected();

    int getState();

    void setTotalTransportSuccess(in String uuid);

    void setRemoteDeviceState(int state);
}
