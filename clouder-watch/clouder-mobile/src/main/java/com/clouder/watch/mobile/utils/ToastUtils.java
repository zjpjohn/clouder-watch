package com.clouder.watch.mobile.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by yang_shoulai on 7/28/2015.
 */
public class ToastUtils {

    public static void show(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
