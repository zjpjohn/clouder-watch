package com.clouder.watch.locker;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


public class UnLockActivity extends Activity implements View.OnClickListener {

    private TextView edtPswOne, edtPswTwo, edtPswThree, edtPswFour, textNotify;
    private TextView textOne, textTwo, textThree, textFour, textFive, textSix, textSeven, textEight, textNine, textZeron, textOk, textCancel;
    private int num;
    private boolean flag = true;
    private Handler handler;
    private static String passWord = null;
    private LinearLayout passWordView, shakeView;
    private static final int FLAG_HOMEKEY_DISPATCHED = 0x80000000;
    private boolean shake = true;
    private static final String TAG = "UnLockActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_lock_square);
        Intent intent = new Intent(this, LockService.class);
        startService(intent);
        init();
        Log.d(TAG, "onCreate");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onAttachedToWindow() {
        this.getWindow().setFlags(FLAG_HOMEKEY_DISPATCHED, FLAG_HOMEKEY_DISPATCHED);
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock lock = keyguardManager.newKeyguardLock(KEYGUARD_SERVICE);
        lock.disableKeyguard();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(this, LockService.class);
        startService(intent);
    }

    private void init() {
        edtPswOne = (TextView) findViewById(R.id.editOne);
        edtPswTwo = (TextView) findViewById(R.id.editTwo);
        edtPswThree = (TextView) findViewById(R.id.editThree);
        edtPswFour = (TextView) findViewById(R.id.editFour);

        textOne = (TextView) findViewById(R.id.textOne);
        textTwo = (TextView) findViewById(R.id.textTwo);
        textThree = (TextView) findViewById(R.id.textThree);
        textFour = (TextView) findViewById(R.id.textFour);
        textFive = (TextView) findViewById(R.id.textFive);
        textSix = (TextView) findViewById(R.id.textSix);
        textSeven = (TextView) findViewById(R.id.textSeven);
        textEight = (TextView) findViewById(R.id.textEight);
        textNine = (TextView) findViewById(R.id.textNine);
        textZeron = (TextView) findViewById(R.id.textZeron);
        textOk = (TextView) findViewById(R.id.textOk);
        textCancel = (TextView) findViewById(R.id.textCancel);
        textNotify = (TextView) findViewById(R.id.innerPassWord);
        passWordView = (LinearLayout) findViewById(R.id.password);
        shakeView = (LinearLayout) findViewById(R.id.shake);
        shakeView.setBackgroundResource(R.drawable.bg);
        shakeView.setAlpha((float) 0.95);
        textOne.setOnClickListener(this);
        textOne.setOnClickListener(this);
        textOne.setOnClickListener(this);
        textTwo.setOnClickListener(this);
        textThree.setOnClickListener(this);
        textFour.setOnClickListener(this);
        textFive.setOnClickListener(this);
        textSix.setOnClickListener(this);
        textSeven.setOnClickListener(this);
        textEight.setOnClickListener(this);
        textNine.setOnClickListener(this);
        textZeron.setOnClickListener(this);
        textOk.setVisibility(View.INVISIBLE);
        textCancel.setOnClickListener(this);
        textNotify.setText(R.string.activity_setpassword_password);
        MyHandler();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.textOne:
                num = 1;
                Message msg = handler.obtainMessage();
                msg.what = 2;
                handler.sendMessage(msg);
                break;
            case R.id.textTwo:
                num = 2;
                Message msgTwo = handler.obtainMessage();
                msgTwo.what = 2;
                handler.sendMessage(msgTwo);
                break;
            case R.id.textThree:
                num = 3;
                Message msgThree = handler.obtainMessage();
                msgThree.what = 2;
                handler.sendMessage(msgThree);
                break;
            case R.id.textFour:
                num = 4;
                Message msgFour = handler.obtainMessage();
                msgFour.what = 2;
                handler.sendMessage(msgFour);
                break;
            case R.id.textFive:
                num = 5;
                Message msgFive = handler.obtainMessage();
                msgFive.what = 2;
                handler.sendMessage(msgFive);
                break;
            case R.id.textSix:
                num = 6;
                Message msgSix = handler.obtainMessage();
                msgSix.what = 2;
                handler.sendMessage(msgSix);
                break;
            case R.id.textSeven:
                num = 7;
                Message msgSeven = handler.obtainMessage();
                msgSeven.what = 2;
                handler.sendMessage(msgSeven);
                break;
            case R.id.textEight:
                num = 8;
                Message msgEight = handler.obtainMessage();
                msgEight.what = 2;
                handler.sendMessage(msgEight);
                break;
            case R.id.textNine:
                num = 9;
                Message msgNine = handler.obtainMessage();
                msgNine.what = 2;
                handler.sendMessage(msgNine);
                break;
            case R.id.textZeron:
                num = 0;
                Message msgZ = handler.obtainMessage();
                msgZ.what = 2;
                handler.sendMessage(msgZ);
                break;
            case R.id.textCancel:
                Message msgC = handler.obtainMessage();
                msgC.what = 3;
                handler.sendMessage(msgC);
                break;
            default:
                break;
        }
    }

    private int getNum() {
        return num;
    }

    private void MyHandler() {
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 2:
                        textNotify.setVisibility(View.INVISIBLE);
                        passWordView.setVisibility(View.VISIBLE);
                        if (edtPswOne.getText().toString() != "") {
                            if (edtPswTwo.getText().toString() != "") {
                                if (edtPswThree.getText().toString() != "") {
                                    if (flag == true) {
                                        Message msgZ = handler.obtainMessage();
                                        msgZ.what = 2;
                                        handler.sendMessageDelayed(msgZ, 150);
                                        flag = false;
                                        Log.d("password", "======" + edtPswThree.getText().toString());
                                    }
                                    if (edtPswFour.getText().toString() != "") {
                                        SharedPreferences sharedPreferences = getSharedPreferences("configuration", 0);
                                        String psw = sharedPreferences.getString("PassWord", String.valueOf(0));
                                        if (psw.equals("0")) {
                                            psw = "0000";
                                        }
                                        Log.d("psw", "" + psw);
                                        if (psw.equals(passWord)) {
                                            Toast.makeText(UnLockActivity.this, R.string.activity_unlock_password_true, Toast.LENGTH_SHORT).show();

                                            UnLockActivity.this.finish();
                                            UnLockActivity.this.finish();
                                        } else if (shake == true) {
                                            Toast.makeText(UnLockActivity.this, R.string.activity_unlock_password_false, Toast.LENGTH_SHORT).show();
                                            Vibrator vib = (Vibrator) UnLockActivity.this.getSystemService(Service.VIBRATOR_SERVICE);
                                            //vibrator.vibrate(1000);// Only shock second, once
                                            long[] pattern = {0, 150};
                                            vib.vibrate(pattern, -1);
                                            Animation shakes = AnimationUtils.loadAnimation(UnLockActivity.this, R.anim.shake);
                                            passWordView.startAnimation(shakes); //Play animation effects to the component
                                            shakes.setAnimationListener(new Animation.AnimationListener() {
                                                @Override
                                                public void onAnimationStart(Animation animation) {
                                                    DisableClick();
                                                }

                                                @Override
                                                public void onAnimationEnd(Animation animation) {
                                                    EnableClick();
                                                    edtPswOne.setText("");
                                                    edtPswTwo.setText("");
                                                    edtPswThree.setText("");
                                                    edtPswFour.setText("");
                                                    flag = true;
                                                }

                                                @Override
                                                public void onAnimationRepeat(Animation animation) {

                                                }
                                            });
                                        }
                                    } else {
                                        edtPswFour.setText("" + getNum());
                                    }

                                } else {
                                    edtPswThree.setText("" + getNum());
                                }
                            } else {
                                edtPswTwo.setText("" + getNum());
                            }
                        } else {
                            edtPswOne.setText("" + getNum());
                        }
                        passWord = "" + edtPswOne.getText() + edtPswTwo.getText() + edtPswThree.getText() + edtPswFour.getText();

                        break;
                    case 3:
                        if (!"".equals(edtPswFour.getText())) {
                            edtPswFour.setText("");
                        } else if (!"".equals(edtPswThree.getText())) {
                            edtPswThree.setText("");
                        } else if (!"".equals(edtPswTwo.getText())) {
                            edtPswTwo.setText("");
                        } else if (!"".equals(edtPswOne.getText())) {
                            edtPswOne.setText("");
                        }
                        flag = true;
                        break;
                    default:
                        break;
                }
            }
        };
    }

    private void EnableClick() {
        textOne.setClickable(true);
        textTwo.setClickable(true);
        textThree.setClickable(true);
        textFour.setClickable(true);
        textFive.setClickable(true);
        textSix.setClickable(true);
        textSeven.setClickable(true);
        textEight.setClickable(true);
        textNine.setClickable(true);
        textZeron.setClickable(true);
    }

    private void DisableClick() {
        textOne.setClickable(false);
        textTwo.setClickable(false);
        textThree.setClickable(false);
        textFour.setClickable(false);
        textFive.setClickable(false);
        textSix.setClickable(false);
        textSeven.setClickable(false);
        textEight.setClickable(false);
        textNine.setClickable(false);
        textZeron.setClickable(false);
    }
}
