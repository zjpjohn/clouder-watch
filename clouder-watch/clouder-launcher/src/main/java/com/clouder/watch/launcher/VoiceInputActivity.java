package com.clouder.watch.launcher;

import android.content.Intent;
import android.graphics.drawable.RotateDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.clouder.watch.common.ui.SwipeLeftActivity;
import com.clouder.watch.common.widget.WatchDialog;

/**
 * 语音倾听界面，等待用户输入语音
 * Created by yang_shoulai on 8/22/2015.
 */
public class VoiceInputActivity extends SwipeLeftActivity {

    private static final String TAG = "VoiceInputActivity";

    private ImageView imageView;

    private TextView tvStatus;

    private static final int[] LISTENING_IMAGES = new int[]{R.drawable.round_1, R.drawable.round_2, R.drawable.round_3, R.drawable.round_4};

    private static final int RESOLVE_IMAGE = R.drawable.voice_resolving_bg;

    private static final int STATE_LISTENING = 1;

    private static final int STATE_RESOLVING = 2;

    private static final int STATE_IDLE = 0;

    private int state = STATE_IDLE;

    private Handler mHandler = new Handler();

    private ImageView icon;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_input);
        initWidgets();
    }

    private void initWidgets() {
        this.imageView = (ImageView) findViewById(R.id.icon_voice);
        this.tvStatus = (TextView) findViewById(R.id.state);
        this.icon = (ImageView) findViewById(R.id.image_icon);
        this.icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (state == STATE_IDLE) {
                    startListening();
                } else if (state == STATE_LISTENING) {
                    stopListening();
                    startResolving();
                } else if (state == STATE_RESOLVING) {
                    stopResolving();
                }
            }
        });
        startListening();

    }


    private void startListening() {
        this.tvStatus.setText(R.string.voice_input);
        state = STATE_LISTENING;
        new Thread(new Runnable() {
            @Override
            public void run() {
                int tmp = 0;
                while (state == STATE_LISTENING) {
                    final int i = tmp;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            imageView.setImageResource(LISTENING_IMAGES[i % 4]);
                        }
                    });
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    tmp++;

                }


            }
        }).start();
    }


    private void stopListening() {
        state = STATE_IDLE;
    }

    private void startResolving() {
        this.tvStatus.setText(getString(R.string.voice_resolving));
        state = STATE_RESOLVING;
        final RotateDrawable drawable = (RotateDrawable) getDrawable(RESOLVE_IMAGE);
        imageView.setImageDrawable(drawable);
        if (drawable == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                int tmp = 0;
                while (state == STATE_RESOLVING) {
                    tmp += 500;
                    final int i = tmp;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            drawable.setLevel(i);
                        }
                    });
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (state == STATE_RESOLVING) {
                    state = STATE_IDLE;
                    WatchDialog dialog = new WatchDialog(VoiceInputActivity.this, "", getString(R.string.my_plan), true);
                    dialog.setCallbackListener(new WatchDialog.ICallbackListener() {
                        @Override
                        public void onNegativeClick(WatchDialog dialog) {
                            dialog.dismiss();
                        }

                        @Override
                        public void onPositiveClick(WatchDialog dialog) {
                            dialog.dismiss();
                            startActivity(new Intent(VoiceInputActivity.this, VoiceResultActivity.class));
                            finish();
                        }
                    });
                    dialog.show();
                }
            }
        }, 3000);


    }

    public void stopResolving() {
        state = STATE_IDLE;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        state = STATE_IDLE;
    }

    @Override
    public void finish() {
        super.finish();
        Log.d(TAG, "finish");
        state = STATE_IDLE;
    }
}
