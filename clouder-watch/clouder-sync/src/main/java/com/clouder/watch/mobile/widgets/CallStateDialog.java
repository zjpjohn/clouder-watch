package com.clouder.watch.mobile.widgets;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.TextView;

import com.clouder.watch.mobile.CallMessageListenerService;
import com.clouder.watch.mobile.R;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by yang_shoulai on 12/3/2015.
 */
public class CallStateDialog extends AlertDialog {


    private int[] images = {R.drawable.bg_call_light_1, R.drawable.bg_call_light_2, R.drawable.bg_call_light_3};

    private int index = 0;

    private RoundImageView imgHead;

    private TextView tvName;

    private ImageButton btnAccept;

    private ImageButton btnReject;

    private ImageButton btnTerminate;

    private View bgHead;

    private Chronometer chronometer;

    private Timer timer = null;

    private Handler uiHandler = new Handler();


    private CallMessageListenerService service;

    public CallStateDialog(Context context, CallMessageListenerService service) {
        super(context);
        this.service = service;
    }

    public static final int STATE_NONE = 0;

    public static final int STATE_ACTIVE = 1;

    public static final int STATE_DIAL = 2;

    public static final int STATE_INCOMING = 3;

    public int state = STATE_NONE;


    //标志位，在接听打电话是置为false，防止来电的页面无法消除
    public boolean state_change_after_accept_call = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.call_state_dialog);
        imgHead = (RoundImageView) findViewById(R.id.img_head);
        tvName = (TextView) findViewById(R.id.tv_name);
        btnReject = (ImageButton) findViewById(R.id.btn_reject);
        btnAccept = (ImageButton) findViewById(R.id.btn_accept);
        btnTerminate = (ImageButton) findViewById(R.id.btn_terminate);
        chronometer = (Chronometer) findViewById(R.id.chronometer);
        bgHead = findViewById(R.id.headBg);

        btnReject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (service != null) {
                    service.rejectPhoneCall();
                }
                dismiss();
            }
        });

        btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (service != null) {
                    state_change_after_accept_call = false;
                    service.acceptPhoneCall();
                    uiHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //5秒状态没改变则强制dismiss，防止页面卡住
                            if (!state_change_after_accept_call) {
                                dismiss();
                            }
                        }
                    }, 5000);
                } else {
                    dismiss();
                }


            }
        });

        btnTerminate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (service != null) {
                    service.terminatePhoneCall();
                }
                dismiss();
            }
        });
        getWindow().setType((WindowManager.LayoutParams.TYPE_SYSTEM_ALERT));
    }


    public void setName(String text) {
        if (this.tvName != null) {
            this.tvName.setText(text);
        }
    }

    public void setHead(Bitmap head) {
        if (this.imgHead != null) {
            this.imgHead.setImageBitmap(head);
        }
    }

    private void startChronometer() {
        if (this.chronometer != null) {
            this.chronometer.setBase(SystemClock.elapsedRealtime());
            this.chronometer.start();
        }
    }

    private void stopChronometer() {
        if (this.chronometer != null) {
            this.chronometer.stop();
        }
    }

    private void showChronometer() {
        if (this.chronometer != null) {
            this.chronometer.setVisibility(View.VISIBLE);
        }
    }

    private void dismissChronometer() {
        if (this.chronometer != null) {
            this.chronometer.setVisibility(View.INVISIBLE);
        }
    }

    private void showTerminateButton() {
        if (btnTerminate != null) {
            btnTerminate.setVisibility(View.VISIBLE);
        }
    }

    private void dismissTerminateButton() {
        if (btnTerminate != null) {
            btnTerminate.setVisibility(View.GONE);
        }
    }


    private void showRejectButton() {
        if (btnReject != null) {
            btnReject.setVisibility(View.VISIBLE);
        }
    }

    private void dismissRejectButton() {
        if (btnReject != null) {
            btnReject.setVisibility(View.GONE);
        }
    }

    private void showAcceptButton() {
        if (btnAccept != null) {
            btnAccept.setVisibility(View.VISIBLE);
        }
    }

    private void dismissAcceptButton() {
        if (btnAccept != null) {
            btnAccept.setVisibility(View.GONE);
        }
    }


    private void startAnimation() {
        if (timer == null) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            bgHead.setBackgroundResource(images[index % 3]);
                            index++;
                        }
                    });
                }
            }, 0, 200);
        }
    }

    private void stopAnimation() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void showActiveState(String name, Bitmap head) {
        super.show();
        setState(STATE_ACTIVE);
        showChronometer();
        startChronometer();
        startAnimation();
        setName(name);
        setHead(head);
        showTerminateButton();
        dismissAcceptButton();
        dismissRejectButton();
    }

    public void showDialState(String name, Bitmap head) {
        super.show();
        setState(STATE_DIAL);
        dismissChronometer();
        startAnimation();
        setName(name);
        setHead(head);
        showTerminateButton();
        dismissAcceptButton();
        dismissRejectButton();
    }


    public void showIncomingState(String name, Bitmap head) {
        super.show();
        setState(STATE_INCOMING);
        dismissChronometer();
        startAnimation();
        setName(name);
        setHead(head);
        dismissTerminateButton();
        showAcceptButton();
        showRejectButton();
    }

    @Override
    public void dismiss() {
        super.dismiss();
        setState(STATE_NONE);
        stopAnimation();
        stopChronometer();
    }

    private void setState(int state) {
        this.state = state;
        this.state_change_after_accept_call = true;

    }

    public int getState() {
        return state;
    }
}
