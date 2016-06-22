package com.clouder.watch.mobile.utils;

import android.graphics.Bitmap;
import android.os.Environment;

import java.io.ByteArrayOutputStream;

/**
 * Created by yang_shoulai on 8/3/2015.
 */
public class ImageUtils {

    private static final String TAG = "ImageUtils";

    public static byte[] bitmap2bytes(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
        //bitmap.recycle();
        byte[] result = output.toByteArray();
        try {
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
