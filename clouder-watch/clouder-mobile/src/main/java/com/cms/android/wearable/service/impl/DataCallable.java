package com.cms.android.wearable.service.impl;

import android.os.ParcelFileDescriptor;

import com.cms.android.wearable.service.codec.ChildAsset;
import com.cms.android.wearable.service.common.FileUtil;
import com.cms.android.wearable.service.common.LogTool;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.concurrent.Callable;

public class DataCallable implements Callable<Boolean> {

    private static final String TAG = "WearableAdapter";

    private ParcelFileDescriptor pfd;

    private ChildAsset childAsset;

    DataCallable(ParcelFileDescriptor pfd, ChildAsset childAsset) {
        this.pfd = pfd;
        this.childAsset = childAsset;
    }

    @Override
    public Boolean call() throws Exception {
        String uuid = childAsset.getUuid();
        int index = childAsset.getIndex();
        long size = childAsset.getSize();
        long assetSize = childAsset.getAssetSize();
        LogTool.d(TAG, "DataCallable<<<>>>" + " uuid = " + uuid + " index = " + index + " size = " + size + " assetSize = " + assetSize);
        MappedByteBuffer buffer = FileUtil.createBuffer(uuid, size);
        if (buffer == null) return Boolean.FALSE;
        LogTool.d(TAG, "process assets: write data to FD : " + this.pfd);
        ParcelFileDescriptor.AutoCloseOutputStream autoCloseOutputStream = new ParcelFileDescriptor.AutoCloseOutputStream(
                this.pfd);
        try {
            int count = 0;
            byte[] cache = new byte[1024];
            buffer.position(index);
            while (count < assetSize) {
                if (assetSize - count < 1024) {
                    cache = new byte[(int) (assetSize - count)];
                }
                buffer.get(cache, 0, cache.length);
                count += cache.length;
                autoCloseOutputStream.write(cache);
                autoCloseOutputStream.flush();
                // LogTool.d(TAG, "process assets: wrote bytes length " +
                // cache.length);
            }
            return Boolean.TRUE;
        } catch (IOException e) {
            LogTool.d(TAG, "process assets: write data failed " + this.pfd);
        } finally {
            try {
                LogTool.d(TAG, "process assets: close " + this.pfd);
                autoCloseOutputStream.close();
                if (pfd != null) {
                    pfd.close();
                }
            } catch (IOException e) {
            }
        }
        return Boolean.FALSE;
    }
}
