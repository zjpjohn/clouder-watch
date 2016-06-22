package com.cms.android.wearable.service.impl;

import com.cms.android.wearable.service.impl.IBLEPeripheralCallback;
import com.cms.android.wearable.service.impl.IWearableListener;

interface IBLEPeripheralService {

  void registerCallback(in String packageName,in IBLEPeripheralCallback callback);
  
  void disconnect();
  
  void sendMessage(in IBLEPeripheralCallback callback, in String node,in String path,in byte[] data);
  
  void addListener(in IBLEPeripheralCallback callback, in IWearableListener listener);
   
  void removeListener(IBLEPeripheralCallback callback, IWearableListener listener);
  
  void addNodeListener(in IBLEPeripheralCallback callback, in IWearableListener listener);
  
  void removeNodeListener(IBLEPeripheralCallback callback, IWearableListener listener);
  
  void getConnectedNodes(in IBLEPeripheralCallback callback);
  
  void getLocalNode(in IBLEPeripheralCallback callback);
}