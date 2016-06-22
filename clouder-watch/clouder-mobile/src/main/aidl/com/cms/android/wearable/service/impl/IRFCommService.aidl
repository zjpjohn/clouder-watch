package com.cms.android.wearable.service.impl;

import com.cms.android.wearable.service.impl.IRfcommServerCallback;

interface IRFCommService {

      void registerCallback(in IRfcommServerCallback callback);

      boolean write(in byte[] bytes);

      void start();

      void stop();

      boolean isConnected();

      void setTotalTransportSuccess(in String uuid);
}
