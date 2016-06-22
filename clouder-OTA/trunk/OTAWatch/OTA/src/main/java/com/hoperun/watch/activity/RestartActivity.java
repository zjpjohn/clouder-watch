package com.hoperun.watch.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;

import com.hoperun.watch.R;

import java.util.Timer;
import java.util.TimerTask;

public class RestartActivity extends Activity {

    private int[] images = { R.mipmap.update00, R.mipmap.update01, R.mipmap.update02,
            R.mipmap.update03, R.mipmap.update04, R.mipmap.update05, R.mipmap.update06,
            R.mipmap.update07, R.mipmap.update08, R.mipmap.update09, R.mipmap.update10,
            R.mipmap.update11, R.mipmap.update12, R.mipmap.update13, R.mipmap.update14,
            R.mipmap.update15, R.mipmap.update16, R.mipmap.update17, R.mipmap.update18,
            R.mipmap.update19, R.mipmap.update20, R.mipmap.update21, R.mipmap.update22,
            R.mipmap.update23, R.mipmap.update24, R.mipmap.update25, R.mipmap.update26,
            R.mipmap.update27, R.mipmap.update28, R.mipmap.update29, R.mipmap.update30,
            R.mipmap.update31, R.mipmap.update32, R.mipmap.update33, R.mipmap.update34,
            R.mipmap.update35, R.mipmap.update36, R.mipmap.update37, R.mipmap.update38,
            R.mipmap.update39, R.mipmap.update40, R.mipmap.update41, R.mipmap.update42,
            R.mipmap.update43, R.mipmap.update44, R.mipmap.update45};

    private int SIGN = 0, num = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restart);

        final ImageView image = (ImageView) findViewById(R.id.upgrading);
        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == SIGN) {
                    image.setImageResource(images[num++]);
                    if (num >= images.length) {
                        num = 0;
                    }
                }
            }
        };

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = SIGN;
                handler.sendMessage(msg);
            }
        }, 0, 50);
    }
}
