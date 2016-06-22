package com.clouder.watch.locker;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


public class SetPassWordActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "SetPassWordActivity";
    private TextView edtPswOne, edtPswTwo, edtPswThree, edtPswFour, textNotify;
    private TextView textOne, textTwo, textThree, textFour, textFive, textSix, textSeven, textEight, textNine, textZeron, textOk, textCancel;
    private int num;
    private static String passWordFirst, passWordSecond;
    private LinearLayout passWordView, shakeView;
    private int flag = 0;
    private Handler handler;
    public static final int FIRSTPASSWORD = 0;
    public static final int SECONDPASSWORD = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_lock_square);
        Intent intent = new Intent(this, LockService.class);
        startService(intent);
        init();
        check();
        Log.d(TAG, "========onCreate========");
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                return true;
        }
        return super.onKeyDown(keyCode, event);
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
        textOk.setOnClickListener(this);
        textCancel.setOnClickListener(this);
        createHandler();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.textOne:
                num = 1;
                Message msg = handler.obtainMessage();
                msg.what = 2;
                handler.sendMessage(msg);
                textNotify.setVisibility(View.INVISIBLE);
                passWordView.setVisibility(View.VISIBLE);
                break;
            case R.id.textTwo:
                num = 2;
                Message msgTwo = handler.obtainMessage();
                msgTwo.what = 2;
                handler.sendMessage(msgTwo);
                textNotify.setVisibility(View.INVISIBLE);
                passWordView.setVisibility(View.VISIBLE);
                break;
            case R.id.textThree:
                num = 3;
                Message msgThree = handler.obtainMessage();
                msgThree.what = 2;
                handler.sendMessage(msgThree);
                textNotify.setVisibility(View.INVISIBLE);
                passWordView.setVisibility(View.VISIBLE);
                break;
            case R.id.textFour:
                num = 4;
                Message msgFour = handler.obtainMessage();
                msgFour.what = 2;
                handler.sendMessage(msgFour);
                textNotify.setVisibility(View.INVISIBLE);
                passWordView.setVisibility(View.VISIBLE);
                break;
            case R.id.textFive:
                num = 5;
                Message msgFive = handler.obtainMessage();
                msgFive.what = 2;
                handler.sendMessage(msgFive);
                textNotify.setVisibility(View.INVISIBLE);
                passWordView.setVisibility(View.VISIBLE);
                break;
            case R.id.textSix:
                num = 6;
                Message msgSix = handler.obtainMessage();
                msgSix.what = 2;
                handler.sendMessage(msgSix);
                textNotify.setVisibility(View.INVISIBLE);
                passWordView.setVisibility(View.VISIBLE);
                break;
            case R.id.textSeven:
                num = 7;
                Message msgSeven = handler.obtainMessage();
                msgSeven.what = 2;
                handler.sendMessage(msgSeven);
                textNotify.setVisibility(View.INVISIBLE);
                passWordView.setVisibility(View.VISIBLE);
                break;
            case R.id.textEight:
                num = 8;
                Message msgEight = handler.obtainMessage();
                msgEight.what = 2;
                handler.sendMessage(msgEight);
                textNotify.setVisibility(View.INVISIBLE);
                passWordView.setVisibility(View.VISIBLE);
                break;
            case R.id.textNine:
                num = 9;
                Message msgNine = handler.obtainMessage();
                msgNine.what = 2;
                handler.sendMessage(msgNine);
                textNotify.setVisibility(View.INVISIBLE);
                passWordView.setVisibility(View.VISIBLE);
                break;
            case R.id.textZeron:
                num = 0;
                Message msgZ = handler.obtainMessage();
                msgZ.what = 2;
                handler.sendMessage(msgZ);
                textNotify.setVisibility(View.INVISIBLE);
                passWordView.setVisibility(View.VISIBLE);
                break;
            case R.id.textOk:
                textNotify.setVisibility(View.VISIBLE);
                textNotify.setText(R.string.activity_setpassword_input_password);
                passWordView.setVisibility(View.INVISIBLE);
                if (flag == SECONDPASSWORD) {
                    if (edtPswOne.getText().equals("")) {
                        Toast.makeText(SetPassWordActivity.this, R.string.activity_setpassword_input_password_agin, Toast.LENGTH_SHORT).show();
                        textNotify.setText(R.string.activity_setpassword_input_password_agin);
                    } else if (passWordSecond.length() < 4 || edtPswFour.getText().equals("")) {
                        Toast.makeText(SetPassWordActivity.this, R.string.activity_setpassword_input_password_four, Toast.LENGTH_SHORT).show();
                        textNotify.setVisibility(View.INVISIBLE);
                        passWordView.setVisibility(View.VISIBLE);
                    } else if (passWordFirst.equalsIgnoreCase(passWordSecond) == true) {
                        Toast.makeText(SetPassWordActivity.this, R.string.activity_setpassword_success, Toast.LENGTH_SHORT).show();
                        SharedPreferences sharedPreferences = getSharedPreferences("configuration", 0);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("PassWord", passWordFirst);
                        editor.commit();
                        SetPassWordActivity.this.finish();
                    } else if (passWordFirst.equalsIgnoreCase(passWordSecond) == false) {
                        Toast.makeText(SetPassWordActivity.this, R.string.activity_setpassword_password_pair_fail, Toast.LENGTH_SHORT).show();
                        flag = FIRSTPASSWORD;
                        setText();
                    }

                } else if (flag == FIRSTPASSWORD) {
                    if (edtPswOne.getText().equals("")) {
                        Toast.makeText(SetPassWordActivity.this, R.string.activity_setpassword_input_password, Toast.LENGTH_SHORT).show();
                        flag = FIRSTPASSWORD;
                    } else if (passWordFirst.length() < 4 || edtPswFour.getText().equals("")) {
                        Toast.makeText(SetPassWordActivity.this, R.string.activity_setpassword_input_password_four, Toast.LENGTH_SHORT).show();
                        textNotify.setVisibility(View.INVISIBLE);
                        passWordView.setVisibility(View.VISIBLE);
                    } else {
                        textNotify.setText(R.string.activity_setpassword_input_password_agin);
                        flag = SECONDPASSWORD;
                        setText();
                    }
                }
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

    private void setText() {
        edtPswOne.setText("");
        edtPswTwo.setText("");
        edtPswThree.setText("");
        edtPswFour.setText("");
    }

    private int getNum() {
        return num;
    }

    private void check() {
        if (flag == 1) {
            edtPswOne.setText("");
            edtPswTwo.setText("");
            edtPswThree.setText("");
            edtPswFour.setText("");

        }
    }

    public void createHandler() {
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 2:
                        if (!"".equals(edtPswOne.getText().toString())) {
                            if (!"".equals(edtPswTwo.getText().toString())) {
                                if (!"".equals(edtPswThree.getText().toString())) {
                                    if (!"".equals(edtPswFour.getText().toString())) {

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
                        if (flag == 0) {
                            passWordFirst = "" + edtPswOne.getText() + edtPswTwo.getText() + edtPswThree.getText() + edtPswFour.getText();
                            Log.e("passWordFirst:", "" + passWordFirst);
                        } else if (flag == 1) {
                            passWordSecond = "" + edtPswOne.getText() + edtPswTwo.getText() + edtPswThree.getText() + edtPswFour.getText();
                            Log.e("passWordSecond:", "" + passWordSecond);
                        }

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
                        } else {
                            SetPassWordActivity.this.finish();
                        }
                        break;
                    default:
                        break;
                }
            }
        };
    }

    @Override
    protected void onResume() {
        check();
        super.onResume();
    }
}
