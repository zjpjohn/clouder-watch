package com.hoperun.watch.util;

import android.graphics.Bitmap;
import java.io.ByteArrayOutputStream;

/**
 * Created by xing_peng on 2015/7/14.
 */
public final class ImageUtils
{
    public static byte[] bitmap2Bytes(Bitmap paramBitmap)
    {
        return bitmap2Bytes(paramBitmap, false);
    }

    public static byte[] bitmap2Bytes(Bitmap paramBitmap, boolean paramBoolean)
    {
        byte[] arrayOfByte;
        if (paramBitmap == null) {
            arrayOfByte = null;
        } else {
            ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();
            paramBitmap.compress(Bitmap.CompressFormat.PNG, 100, localByteArrayOutputStream);
            if (paramBoolean)
                paramBitmap.recycle();
            arrayOfByte = localByteArrayOutputStream.toByteArray();
            try
            {
                localByteArrayOutputStream.close();
            }
            catch (Exception localException)
            {
            }
        }
        return arrayOfByte;
    }
}