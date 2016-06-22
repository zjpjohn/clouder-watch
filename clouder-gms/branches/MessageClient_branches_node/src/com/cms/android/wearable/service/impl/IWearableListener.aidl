package com.cms.android.wearable.service.impl;

import com.cms.android.wearable.internal.MessageEventHolder;
import com.cms.android.wearable.internal.NodeHolder;

interface IWearableListener {

  void onMessageReceived(in MessageEventHolder messageEventHolder);
  
  void onPeerConnected(in NodeHolder nodeHolder);
  
  void onPeerDisconnected(in NodeHolder nodeHolder);
  
}