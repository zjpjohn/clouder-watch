package com.hoperun.watch.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.hoperun.watch.R;

public class ConfirmActivity extends Activity {

    private static final String TAG = "watch";
    private ImageButton btnTrue, btnFalse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm);

        btnTrue = (ImageButton)findViewById(R.id.btnTrue);
        btnFalse = (ImageButton)findViewById(R.id.btnFalse);

        btnTrue.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "start install");

                Intent intent = new Intent(getApplicationContext(), UpgradActivity.class);
                startActivity(intent);

                Intent broadcast = new Intent();
                broadcast.setAction("InstallFirmware");
                sendBroadcast(broadcast);

            }
        });

        btnFalse.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Intent broadcast = new Intent();
                broadcast.setAction("CancelFirmware");
                sendBroadcast(broadcast);
                finish();
            }
        });
    }

}
