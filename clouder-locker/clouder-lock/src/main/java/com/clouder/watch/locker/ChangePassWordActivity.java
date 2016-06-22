package com.clouder.watch.locker;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


public class ChangePassWordActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "ChangePassWordActivity";
    private TextView edtPswOne, edtPswTwo, edtPswThree, edtPswFour, textNotify;
    private TextView textOne, textTwo, textThree, textFour, textFive, textSix, textSeven, textEight, textNine, textZeron, textOk, textCancel;
    private int num;
    private Handler handler;
    private String psw = "2500";
    private LinearLayout passWordView;
    private LinearLayout shakeView;
    private int flag = 0;
    private static String passWordOri, passWordFirst, passWordSecond;
    public static final int ORIGINALPASSWORD = 0;
    private static final int FIRSTPASSWORD = 1;
    private static final int SECONDPASSWORD = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_lock_square);
        SharedPreferences sharedPreferences = getSharedPreferences("configuration", 0);
        String psw = sharedPreferences.getString("PassWord", null);
        if (psw == null || psw.trim().length() == 0) {
            Intent i = new Intent(ChangePassWordActivity.this, SetPassWordActivity.class);
            startActivity(i);
            finish();
        }
        init();
        Log.d(TAG, "======onCreate======");
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
        textNotify.setText(R.string.activity_changepsw_input_password_ori);
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
        edtPswOne.setText("");
        edtPswTwo.setText("");
        edtPswThree.setText("");
        edtPswFour.setText("");
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
            case R.id.textOk:
                textNotify.setVisibility(View.VISIBLE);
                passWordView.setVisibility(View.INVISIBLE);
                SharedPreferences sharedPreferences = getSharedPreferences("configuration", 0);
                String psw = sharedPreferences.getString("PassWord", String.valueOf(0));
                if (psw.equals("0")) {
                    psw = "0000";
                }
                Log.d("psw", "" + psw);

                if (flag == SECONDPASSWORD) {
                    if (edtPswOne.getText().equals("")) {
                        Toast.makeText(ChangePassWordActivity.this, R.string.activity_changepsw_input_password_new_agin, Toast.LENGTH_SHORT).show();
                        textNotify.setText(R.string.activity_changepsw_input_password_new_agin);
                    } else if (passWordSecond.length() < 4 || edtPswFour.getText().equals("")) {
                        Toast.makeText(ChangePassWordActivity.this, R.string.activity_changepsw_input_password_four, Toast.LENGTH_SHORT).show();
                        textNotify.setVisibility(View.INVISIBLE);
                        passWordView.setVisibility(View.VISIBLE);
                    } else if (passWordFirst.equalsIgnoreCase(passWordSecond) == true) {
                        SharedPreferences sharedPreference = getSharedPreferences("configuration", 0);
                        SharedPreferences.Editor editor = sharedPreference.edit();
                        editor.putString("PassWord", passWordFirst);
                        editor.commit();
                        Toast.makeText(ChangePassWordActivity.this, R.string.activity_changepsw_success, Toast.LENGTH_SHORT).show();
                        ChangePassWordActivity.this.finish();
                    } else if (passWordFirst.equalsIgnoreCase(passWordSecond) == false) {
                        Toast.makeText(ChangePassWordActivity.this, R.string.activity_changepsw_password_pair_false, Toast.LENGTH_SHORT).show();
                        flag = FIRSTPASSWORD;
                        textNotify.setText(R.string.activity_changepsw_input_password_new);
                        setText();
                    }
                } else if (flag == FIRSTPASSWORD) {
                    if (edtPswOne.getText().equals("")) {
                        Toast.makeText(ChangePassWordActivity.this, R.string.activity_changepsw_input_password_new, Toast.LENGTH_SHORT).show();
                        textNotify.setText(R.string.activity_changepsw_input_password_new);
                    } else if (passWordFirst.length() < 4 || edtPswFour.getText().equals("")) {
                        Toast.makeText(ChangePassWordActivity.this, R.string.activity_changepsw_input_password_four, Toast.LENGTH_SHORT).show();
                        textNotify.setVisibility(View.INVISIBLE);
                        passWordView.setVisibility(View.VISIBLE);
                    } else if (passWordFirst.length() == 4) {
                        if (passWordFirst.equalsIgnoreCase(psw) == false) {
                            textNotify.setText(R.string.activity_changepsw_input_password_new_agin);
                            flag = SECONDPASSWORD;
                            setText();
                        } else {
                            Toast.makeText(ChangePassWordActivity.this, R.string.activity_changepsw_input_password_not_the_same, Toast.LENGTH_SHORT).show();
                            flag = FIRSTPASSWORD;
                            setText();
                        }
                    }
                } else if (flag == ORIGINALPASSWORD) {
                    if (edtPswOne.getText().equals("")) {
                        Toast.makeText(ChangePassWordActivity.this, R.string.activity_changepsw_input_password_ori, Toast.LENGTH_SHORT).show();
                        textNotify.setText(R.string.activity_changepsw_input_password_ori);
                    } else if (flag == ORIGINALPASSWORD) {
                        if (passWordOri.length() < 4 || edtPswFour.getText().equals("")) {
                            textNotify.setVisibility(View.INVISIBLE);
                            passWordView.setVisibility(View.VISIBLE);
                            Toast.makeText(ChangePassWordActivity.this, R.string.activity_changepsw_input_password_four, Toast.LENGTH_SHORT).show();
                        } else if (psw.equalsIgnoreCase(passWordOri) == true) {
                            Toast.makeText(ChangePassWordActivity.this, R.string.activity_changepsw_input_password_ori_true, Toast.LENGTH_SHORT).show();
                            flag = FIRSTPASSWORD;
                            textNotify.setText(R.string.activity_changepsw_input_password_new);
                            setText();
                        } else if (psw.equalsIgnoreCase(passWordOri) == false) {
                            Toast.makeText(ChangePassWordActivity.this, R.string.activity_changepsw_input_password_ori_false, Toast.LENGTH_SHORT).show();
                            textNotify.setText(R.string.activity_changepsw_input_password_ori);
                            flag = ORIGINALPASSWORD;
                            setText();
                        }
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

    public void createHandler() {
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 2:

                        textNotify.setVisibility(View.INVISIBLE);
                        passWordView.setVisibility(View.VISIBLE);
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
                            passWordOri = "" + edtPswOne.getText() + edtPswTwo.getText() + edtPswThree.getText() + edtPswFour.getText();
                            Log.d("passWordOri:", "" + passWordOri);
                        } else if (flag == 1) {
                            passWordFirst = "" + edtPswOne.getText() + edtPswTwo.getText() + edtPswThree.getText() + edtPswFour.getText();
                            Log.d("passWordFirst:", "" + passWordFirst);
                        } else if (flag == 2) {
                            passWordSecond = "" + edtPswOne.getText() + edtPswTwo.getText() + edtPswThree.getText() + edtPswFour.getText();
                            Log.d("passWordSecond:", "" + passWordSecond);
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
                            ChangePassWordActivity.this.finish();
                        }
                        break;
                    default:
                        break;
                }
            }
        };
    }
}
