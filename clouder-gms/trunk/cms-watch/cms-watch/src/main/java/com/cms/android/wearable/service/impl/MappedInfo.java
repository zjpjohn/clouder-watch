package com.cms.android.wearable.service.impl;

import java.nio.MappedByteBuffer;

/**
 * Created by yang_shoulai on 11/14/2015.
 */
public class MappedInfo {
    private String filepath;

    private MappedByteBuffer buffer;

    public MappedInfo(String filepath, MappedByteBuffer buffer) {
        super();
        this.filepath = filepath;
        this.buffer = buffer;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public MappedByteBuffer getBuffer() {
        return buffer;
    }

    public void setBuffer(MappedByteBuffer buffer) {
        this.buffer = buffer;
    }
}
