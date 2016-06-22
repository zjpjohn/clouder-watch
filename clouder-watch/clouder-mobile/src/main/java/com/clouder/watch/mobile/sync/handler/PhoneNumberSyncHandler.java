package com.clouder.watch.mobile.sync.handler;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.clouder.watch.common.sync.IHandler;
import com.clouder.watch.common.sync.IMessageListener;
import com.clouder.watch.common.sync.SyncMessage;
import com.clouder.watch.common.sync.message.PhoneNumberSyncMessage;
import com.clouder.watch.mobile.SyncService;

/**
 * Created by yang_shoulai on 12/7/2015.
 */
public class PhoneNumberSyncHandler implements IHandler<PhoneNumberSyncMessage>, IMessageListener {

    private SyncService syncService;

    public PhoneNumberSyncHandler(SyncService syncService) {
        this.syncService = syncService;
    }

    @Override
    public void handle(String path, PhoneNumberSyncMessage message) {
        message.setMethod(SyncMessage.Method.Set);
        TelephonyManager tm = (TelephonyManager) syncService.getSystemService(Context.TELEPHONY_SERVICE);
        String number = tm.getLine1Number();
        message.setNumber(number == null ? "" : number);
        Log.d("PhoneNumberSyncHandler", "Get phone number = " + number);
        syncService.sendMessage(message);
    }

    @Override
    public void onMessageReceived(String path, SyncMessage message) {
        handle(path, (PhoneNumberSyncMessage) message);
    }
}
