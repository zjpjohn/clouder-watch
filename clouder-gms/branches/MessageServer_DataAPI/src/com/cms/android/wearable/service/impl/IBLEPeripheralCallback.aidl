package com.cms.android.wearable.service.impl;

import com.cms.android.common.api.Status;
import com.cms.android.wearable.internal.SendMessageResponse;
import com.cms.android.wearable.internal.GetFdForAssetResponse;
import com.cms.android.wearable.internal.DataHolder;
import com.cms.android.wearable.internal.PutDataResponse;
import com.cms.android.wearable.internal.GetConnectedNodesResponse;
import com.cms.android.wearable.internal.GetLocalNodeResponse;

interface IBLEPeripheralCallback {
  
  void setSendMessageRsp(in SendMessageResponse sendMessageResponse);
  
  void setStatusRsp(in Status status);
  
  void setGetFdForAssetRsp(in GetFdForAssetResponse getFdForAssetResponse);
  
  void setDataHolderRsp(in DataHolder dataHolder);
  
  void setPutDataRsp(in PutDataResponse response);
  
  void setAssetRsp();
  
  void setGetConnectedNodesRsp(in GetConnectedNodesResponse getConnectedNodesResponse);
  
  void setLocalNodeRsp(in GetLocalNodeResponse getLocalNodeResponse);
  
  String getPackageName();
}