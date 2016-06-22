package com.cms.android.wearable.service.impl;

import com.cms.android.wearable.service.impl.IBLECentralCallback;
import com.cms.android.wearable.service.impl.IWearableListener;
import com.cms.android.wearable.Asset;
import com.cms.android.wearable.PutDataRequest;
import com.cms.android.wearable.internal.DataItemParcelable;

interface IBLECentralService {

  void registerCallback(in String packageName,in IBLECentralCallback callback);
  
  void connect(String address);
  
  void disconnect();
  
  void sendMessage(in IBLECentralCallback callback, in String node,in String path,in byte[] data);
  
  void addListener(int type,in IBLECentralCallback callback, in IWearableListener listener);
   
  void removeListener(int type,in IBLECentralCallback callback, IWearableListener listener);
  
  void getFdForAsset(IBLECentralCallback callback,in Asset asset);
  
  void getDataItems(IBLECentralCallback callback,in Uri uri);
  
  void putDataItem(in IBLECentralCallback callback, in PutDataRequest putDataRequest);

  void getConnectedNodes(in IBLECentralCallback callback);
  
  void getLocalNode(in IBLECentralCallback callback);
  
  boolean syncTime(long time,int reason);
  
}