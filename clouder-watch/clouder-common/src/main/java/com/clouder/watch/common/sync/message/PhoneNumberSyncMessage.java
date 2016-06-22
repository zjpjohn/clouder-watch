package com.clouder.watch.common.sync.message;

import com.clouder.watch.common.sync.SyncMessage;
import com.clouder.watch.common.sync.SyncMessagePathConfig;

/**
 * Created by yang_shoulai on 12/7/2015.
 */
public class PhoneNumberSyncMessage extends SyncMessage {

    private String number;

    public PhoneNumberSyncMessage() {
        super(SyncMessagePathConfig.SYNC_PHONE_NUMBER);
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }
}
