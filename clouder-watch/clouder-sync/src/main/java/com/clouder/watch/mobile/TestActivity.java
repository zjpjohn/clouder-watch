package com.clouder.watch.mobile;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;

import com.clouder.watch.common.ui.SwipeRightActivity;
import com.clouder.watch.mobile.widgets.CallStateDialog;

/**
 * Created by yang_shoulai on 12/3/2015.
 */
public class TestActivity extends SwipeRightActivity implements View.OnClickListener {

    private Handler handler = new Handler();

    private CallStateDialog callStateDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        Button btnActive = (Button) findViewById(R.id.btn_active);
        Button btnIncoming = (Button) findViewById(R.id.btn_incoming);
        Button btnDial = (Button) findViewById(R.id.btn_dialing);
        callStateDialog = new CallStateDialog(this, null);
        btnActive.setOnClickListener(this);
        btnIncoming.setOnClickListener(this);
        btnDial.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {

            case R.id.btn_active:
                callStateDialog.showActiveState("AA", BitmapFactory.decodeResource(getResources(), R.drawable.img_call_head));

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        callStateDialog.showActiveState("BB", BitmapFactory.decodeResource(getResources(), R.drawable.btn_terminate_hover));
                    }
                }, 2000);

                break;

            case R.id.btn_dialing:

                callStateDialog.showDialState("BB", BitmapFactory.decodeResource(getResources(), R.drawable.img_call_head));
                break;

            case R.id.btn_incoming:
                callStateDialog.showIncomingState("CC", BitmapFactory.decodeResource(getResources(), R.drawable.img_call_head));
                break;
            default:
                break;
        }
    }
}
