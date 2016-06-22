package com.hoperun.watch.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.hoperun.watch.R;
import com.hoperun.watch.service.DownloadService;

public class MainActivity extends Activity {

    private static final String TAG = "watch";

    private TextView versionText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        versionText = (TextView) findViewById(R.id.version);
        versionText.setText("当前版本：" + Build.VERSION.RELEASE);

        Log.d(TAG, "start service");
        Intent serviceIntent = new Intent();
        serviceIntent.setAction(DownloadService.INTENT);
        serviceIntent.setPackage(getPackageName());
        startService(serviceIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "stop service");
        Intent serviceIntent = new Intent();
        serviceIntent.setAction(DownloadService.INTENT);
        serviceIntent.setPackage(getPackageName());
        stopService(serviceIntent);
    }
}
