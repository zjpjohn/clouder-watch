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
        Thread thread = Thread.currentThread();
        LogTool.d(TAG, "DataCallable thread id = " + thread.getId() + " name = " + thread.getName());
        String uuid = childAsset.getUuid();
        int index = childAsset.getIndex();
        long size = childAsset.getSize();
        long assetSize = childAsset.getAssetSize();
        LogTool.e(TAG, "DataCallable<<<>>>" + " uuid = " + uuid + " index = " + index + " size = " + size + " assetSize = " + assetSize);
        MappedByteBuffer buffer = FileUtil.createBuffer(uuid, size);
        if (buffer != null) {
            LogTool.d(TAG, "process assets: write data to FD : " + this.pfd);
            ParcelFileDescriptor.AutoCloseOutputStream autoCloseOutputStream = new ParcelFileDescriptor.AutoCloseOutputStream(this.pfd);
            try {

                int count = 0;
                byte[] cache = new byte[4096];
                buffer.position(index);
                while (count < assetSize) {
                    if (assetSize - count < 4096) {
                        cache = new byte[(int) (assetSize - count)];
                    }
                    buffer.get(cache, 0, cache.length);
                    count += cache.length;
                    //LogTool.d(TAG, "autoCloseOutputStream write assetSize = " + assetSize + ", count = " + count);
                    autoCloseOutputStream.write(cache);
                    //LogTool.d(TAG, "autoCloseOutputStream flush");
                    autoCloseOutputStream.flush();
                    //LogTool.d(TAG, "autoCloseOutputStream flush end");
                    //LogTool.d(TAG, "process assets: wrote bytes length " + cache.length);
                }
                return Boolean.TRUE;
            } catch (Exception e) {
                LogTool.d(TAG, "process assets: write data failed " + this.pfd, e);
            } finally {
                try {
                    LogTool.d(TAG, "process assets: close " + this.pfd);
                    autoCloseOutputStream.close();
                } catch (IOException e) {
                }
                buffer.clear();
            }
        }
        return Boolean.FALSE;
    }
}
