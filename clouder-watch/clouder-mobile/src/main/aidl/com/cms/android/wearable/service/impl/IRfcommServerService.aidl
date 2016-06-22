package com.cms.android.wearable.service.impl;

import com.cms.android.wearable.service.impl.IRfcommServerCallback;

interface IRfcommServerService {

  void registerCallback(in IRfcommServerCallback callback);
  
  boolean write(in byte[] bytes);
  

  void start(boolean force);

  void stop();
  
  boolean isConnected();
  
  boolean isConnecting();
  
  void setTotalTransportSuccess(in String uuid);
}