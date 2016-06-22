package com.clouder.watch.setting.model;

import android.content.Intent;
import android.graphics.drawable.Drawable;

import com.clouder.watch.common.sync.SyncUtils;

/**
 * Created by yang_shoulai on 8/5/2015.
 */
public class SyncWatchFaceItem {
    /**
     * 表盘应用包名
     */
    private String packageName;

    /**
     * 表盘的服务名
     */
    private String serviceName;

    /**
     * 表盘的缩略图
     */
    private Drawable drawable;

    /**
     * 表盘名称
     */
    private String name;

    private Intent intent;

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Drawable getDrawable() {
        return drawable;
    }

    public void setDrawable(Drawable drawable) {
        this.drawable = drawable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Intent getIntent() {
        return intent;
    }

    public void setIntent(Intent intent) {
        this.intent = intent;
    }

    public byte[] toBytes() {
        byte[] pBytes = packageName.getBytes();
        byte[] sBytes = serviceName.getBytes();
        byte[] nBytes = name.getBytes();
        int pLength = pBytes.length;
        int sLength = sBytes.length;
        int nLength = nBytes.length;

        byte[] plBytes = SyncUtils.intToBytes(pLength);
        byte[] slBytes = SyncUtils.intToBytes(sLength);
        byte[] nlBytes = SyncUtils.intToBytes(nLength);

        byte[] drawableBytes = SyncUtils.bitmap2Bytes(SyncUtils.drawableToBitmap(drawable));
        byte[] temp = new byte[plBytes.length + slBytes.length + nlBytes.length + pBytes.length + sBytes.length + nBytes.length + drawableBytes.length];
        System.arraycopy(plBytes, 0, temp, 0, plBytes.length);
        System.arraycopy(slBytes, 0, temp, plBytes.length, slBytes.length);
        System.arraycopy(nlBytes, 0, temp, plBytes.length + slBytes.length, nlBytes.length);
        System.arraycopy(pBytes, 0, temp, plBytes.length + slBytes.length + nlBytes.length, pBytes.length);
        System.arraycopy(sBytes, 0, temp, plBytes.length + slBytes.length + nlBytes.length + pBytes.length, sBytes.length);
        System.arraycopy(nBytes, 0, temp, plBytes.length + slBytes.length + nlBytes.length + pBytes.length + sBytes.length, nBytes.length);
        System.arraycopy(drawableBytes, 0, temp, plBytes.length + slBytes.length + nlBytes.length + pBytes.length + sBytes.length + nBytes.length + drawableBytes.length, drawableBytes.length);
        return temp;
    }


}
