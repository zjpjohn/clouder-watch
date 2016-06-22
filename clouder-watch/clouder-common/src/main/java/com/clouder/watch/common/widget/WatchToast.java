package com.clouder.watch.common.widget;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.clouder.watch.common.R;

/**
 * Created by yang_shoulai on 7/20/2015.
 */
public class WatchToast {

    public static Toast make(Context context, String message, int duration) {
        Toast result = new Toast(context);
        View layout = LayoutInflater.from(context).inflate(R.layout.watch_toast_layout, null);
        TextView text = (TextView) layout.findViewById(R.id.text);
        text.setText(message);
        result.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        result.setDuration(duration);
        result.setView(layout);
        return result;
    }
}
