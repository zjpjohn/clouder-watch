package com.cms.android.wearable.service.impl;

import com.cms.android.wearable.service.impl.IBLEPeripheralCallback;
import com.cms.android.wearable.service.impl.IWearableListener;
import com.cms.android.wearable.Asset;
import com.cms.android.wearable.PutDataRequest;
import com.cms.android.wearable.internal.DataItemParcelable;

interface IBLEPeripheralService {

  void registerCallback(in String packageName,in IBLEPeripheralCallback callback);
  
  void disconnect();
  
  void sendMessage(in IBLEPeripheralCallback callback, in String node,in String path,in byte[] data);
  
  void addListener(int type,in IBLEPeripheralCallback callback, in IWearableListener listener);
   
  void removeListener(int type,in IBLEPeripheralCallback callback, IWearableListener listener);
  
  void getFdForAsset(in IBLEPeripheralCallback callback,in Asset asset);
  
  void getDataItems(in IBLEPeripheralCallback callback,in Uri uri);
  
  void putDataItem(in IBLEPeripheralCallback callback, in PutDataRequest putDataRequest);

  void getConnectedNodes(in IBLEPeripheralCallback callback);
  
  void getLocalNode(in IBLEPeripheralCallback callback);
}