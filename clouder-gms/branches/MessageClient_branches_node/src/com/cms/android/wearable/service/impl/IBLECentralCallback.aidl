package com.cms.android.wearable.service.impl;

import com.cms.android.common.api.Status;
import com.cms.android.wearable.internal.SendMessageResponse;
import com.cms.android.wearable.internal.GetConnectedNodesResponse;
import com.cms.android.wearable.internal.GetLocalNodeResponse;

interface IBLECentralCallback {

  void setSendMessageRsp(in SendMessageResponse sendMessageResponse);
  
  void setGetConnectedNodesRsp(in GetConnectedNodesResponse getConnectedNodesResponse);
  
  void setLocalNodeRsp(in GetLocalNodeResponse getLocalNodeResponse);
  
  void setStatusRsp(in Status status);
  
}