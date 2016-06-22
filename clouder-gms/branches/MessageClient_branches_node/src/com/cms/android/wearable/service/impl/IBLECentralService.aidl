package com.cms.android.wearable.service.impl;

import com.cms.android.wearable.service.impl.IBLECentralCallback;
import com.cms.android.wearable.service.impl.IWearableListener;

interface IBLECentralService {

  void registerCallback(in String packageName,in IBLECentralCallback callback);
  
  void connect(String address);
  
  void disconnect();
  
  void sendMessage(in IBLECentralCallback callback, in String node,in String path,in byte[] data);
 
  void addListener(in IBLECentralCallback callback, in IWearableListener listener,in String node);
  
  void removeListener(IBLECentralCallback callback, IWearableListener listener, String node);
  
  void addNodeListener(in IBLECentralCallback callback, in IWearableListener listener);
  
  void removeNodeListener(IBLECentralCallback callback, IWearableListener listener);
  
  void getConnectedNodes(in IBLECentralCallback callback);
  
  void getLocalNode(in IBLECentralCallback callback);
  
  boolean syncTime(long time,int reason);
  
}