package com.cms.android.wearable.service.impl;

import com.cms.android.wearable.service.impl.IRfcommClientCallback;

interface IRfcommClientService {

  void registerCallback(in IRfcommClientCallback callback);
  
  void start(String address);
  
  void restart();
  
  void stop();
  
  boolean write(in byte[] bytes);
  
  boolean isConnected();
  
  void setStatus(int status);
  
  void setTotalTransportSuccess(in String uuid);
}