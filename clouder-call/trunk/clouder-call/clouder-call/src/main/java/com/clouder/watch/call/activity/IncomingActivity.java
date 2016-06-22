/**
 * **************************************************************************
 * <p/>
 * HOPERUN PROPRIETARY INFORMATION
 * <p/>
 * The information contained herein is proprietary to HopeRun
 * and shall not be reproduced or disclosed in whole or in part
 * or used for any design or manufacture
 * without direct written authorization from HopeRun.
 * <p/>
 * Copyright (c) 2014 by HopeRun.  All rights reserved.
 * <p/>
 * ***************************************************************************
 */
package com.clouder.watch.call.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.TextView;

import com.clouder.watch.call.R;
import com.clouder.watch.call.ui.RoundImageView;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * ClassName: IncomingActivity
 *
 * @author xing_peng
 * @description
 * @Date 2015-7-27
 */
public class IncomingActivity extends Activity implements OnClickListener {

    private static final String TAG = "IncomingActivity";

    private ImageButton btnAccept, btnTerminate, btnReject;
    private View callView, acceptView, headBgView;
    private RoundImageView head, accHead;
    private Chronometer timer;
    private TextView nameTextView, namingTextView;
    private ContentResolver resolver;

    private int[] images = {0, R.drawable.bg_call_light_1, R.drawable.bg_call_light_2, R.drawable.bg_call_light_3};
    private int SIGN = 0, num = 0;
    private String mName, mNum;

    private boolean isActive = false;

    private Timer animationTimer = new Timer();

    private Bitmap bm;

    private String command;

    private BroadcastReceiver mReceivers = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("state_zero")) {
                Log.d(TAG, "接收到无SIM卡广播，开始挂断电话");
                finish();
            } else if (intent.getAction().equals("clouder.watch.ACTION_CALL_STATE_CHANGE")) {
                int state = intent.getIntExtra("extra_state", -1);
                if (state == 7) {
                    Log.d(TAG, "接收到电话挂断广播");
                    finish();
                }

            }
        }
    };

    private Messenger myMessenger = new Messenger(new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
                case 1:
                    onCallStateActive(msg.getData().getString("phoneNumber"));
                    break;
                case 2:
                    onCallStateTerminated(msg.getData().getString("phoneNumber"));
                    break;
                case 3:
                    Object state = msg.getData().get("state");
                    if (state != null && state instanceof Integer) {
                        onHFPConnectionChanged(((Integer) state).intValue());
                    }
                    break;
                case 4:
                    onCallStateDialing(msg.getData().getString("phoneNumber"));
                    break;
                case 5:
                    onCallStateIncoming(msg.getData().getString("phoneNumber"));
                    break;
            }

        }
    });

    //去电
    private void onCallStateDialing(String phoneNumber) {
        Log.i(TAG, "手机去电，不做处理");
    }

    //来电
    private void onCallStateIncoming(String phoneNumber) {
        Log.i(TAG, "手机来电，不做处理");
    }

    //HF连接状态改变
    private void onHFPConnectionChanged(int state) {
        if (BluetoothProfile.STATE_CONNECTED == state) {
            Log.i(TAG, "HF已经连接");

        } else if (BluetoothProfile.STATE_DISCONNECTED == state) {
            Log.e(TAG, "HF已经断开连接");
        } else {
            Log.i(TAG, "HF Connection State Change, State = " + state);
        }

    }

    //挂断
    private void onCallStateTerminated(String phoneNumber) {
        Log.d(TAG, "挂断, number : " + phoneNumber);
        finish();
    }

    //接通
    private void onCallStateActive(String phoneNumber) {
        Log.d(TAG, "接通, number : " + phoneNumber);
        if (!isActive) {
            callingPage();
            acceptPage();
            if (timer != null) {
                timer.setBase(SystemClock.elapsedRealtime());
                timer.start();
            }
            Log.d(TAG, "电话接听");
            isActive = true;
        } else {
            Log.d(TAG, "电话呼叫保持恢复");
        }
    }

    private Messenger serverMessenger = null;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            Message msg = Message.obtain();
            msg.what = 1;
            msg.replyTo = myMessenger;
            try {
                serverMessenger = new Messenger(service);
                serverMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming);
        head = (RoundImageView) findViewById(R.id.head);
        accHead = (RoundImageView) findViewById(R.id.accHead);
        callView = findViewById(R.id.top);
        headBgView = findViewById(R.id.headBg);
        acceptView = findViewById(R.id.acceptCall);
        nameTextView = (TextView) findViewById(R.id.name);
        namingTextView = (TextView) findViewById(R.id.naming);
        btnAccept = (ImageButton) findViewById(R.id.callAccept);
        btnTerminate = (ImageButton) findViewById(R.id.callTerminate);
        btnReject = (ImageButton) findViewById(R.id.callReject);
        btnAccept.setOnClickListener(IncomingActivity.this);
        btnTerminate.setOnClickListener(IncomingActivity.this);
        btnReject.setOnClickListener(IncomingActivity.this);
        timer = (Chronometer) findViewById(R.id.chronometer);

        Intent callListenerServiceIntent = new Intent();
        callListenerServiceIntent.setComponent(new ComponentName("com.clouder.watch.mobile", "com.clouder.watch.mobile.CallMessageListenerService"));
        bindService(callListenerServiceIntent, serviceConnection, BIND_AUTO_CREATE);


        IntentFilter filter = new IntentFilter();
        filter.addAction("state_zero");
        filter.addAction("clouder.watch.ACTION_CALL_STATE_CHANGE");
        registerReceiver(mReceivers, filter);
        Log.d(TAG, "添加SIM卡信息相关广播成功");

        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == SIGN) {
                    headBgView.setBackgroundResource(images[num++]);
                    if (num >= images.length) {
                        num = 0;
                    }
                }
            }
        };
        animationTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = SIGN;
                handler.sendMessage(msg);
            }
        }, 0, 200);

        initViews();
    }

    @SuppressLint("HandlerLeak")
    private void initViews() {
        Intent intent = getIntent();
        mNum = intent.getStringExtra("number");
        command = intent.getStringExtra("command");
        if (nameTextView != null && nameTextView.getTag() == null) {
            nameTextView.setText(mNum);
        }
        if (namingTextView != null && namingTextView.getTag() == null) {
            namingTextView.setText(mNum);
        }
        if (head != null && head.getTag() == null) {
            head.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.img_call_list_head));
        }
        if (accHead != null && accHead.getTag() == null) {
            accHead.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.img_call_list_head));
        }
        if ("incomingCall".equals(command)) {
            Log.d(TAG, "incoming call");
            incomingPage();
        } else if ("dialCall".equals(command)) {
            Log.d(TAG, "dial call");
            callingPage();
        } else if ("activeCall".equals(command)) {
            onCallStateActive(mNum);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                mName = queryContactNameByPhoneNumber(mNum);
                bm = getPhoto(mNum);
                Log.d(TAG, "姓名：" + mName + "电话：" + mNum);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (nameTextView != null) {
                            nameTextView.setText(mName);
                            nameTextView.setTag(mName);
                        }
                        if (namingTextView != null) {
                            namingTextView.setText(mName);
                            namingTextView.setTag(mName);
                        }
                        if (bm != null) {
                            if (head != null) {
                                head.setImageBitmap(bm);
                                head.setTag(mNum);
                            }
                            if (accHead != null) {
                                accHead.setImageBitmap(bm);
                                accHead.setTag(mNum);
                            }
                        }
                    }
                });
            }
        }).start();
    }

    private void incomingPage() {
        if (btnAccept != null) {
            btnAccept.setVisibility(View.VISIBLE);
        }
        if (btnReject != null) {
            btnReject.setVisibility(View.VISIBLE);
        }
        if (btnTerminate != null) {
            btnTerminate.setVisibility(View.INVISIBLE);
        }
    }

    private void callingPage() {
        if (btnTerminate != null) {
            btnTerminate.setVisibility(View.VISIBLE);
        }
        if (btnAccept != null) {
            btnAccept.setVisibility(View.INVISIBLE);
        }
        if (btnReject != null) {
            btnReject.setVisibility(View.INVISIBLE);
        }

    }

    private void acceptPage() {
        if (acceptView != null) {
            acceptView.setVisibility(View.VISIBLE);
        }
        if (callView != null) {
            callView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.callAccept:
                callingPage();
                acceptPage();
                acceptPhoneCall();
                // The timer is reset
                timer.setBase(SystemClock.elapsedRealtime());
                timer.start();
                break;
            case R.id.callTerminate:
                terminatePhoneCall();
                finish();
                break;
            case R.id.callReject:
                rejectPhoneCall();
                finish();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart......");
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop......");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy......");
        super.onDestroy();
        if (animationTimer != null) {
            animationTimer.cancel();
        }
        if (timer != null) {
            timer.stop();
        }
        unregisterReceiver(mReceivers);

        if (serverMessenger != null) {
            Message msg = Message.obtain();
            msg.what = 2;
            try {
                serverMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        unbindService(serviceConnection);
    }

    /**
     * acceptPhoneCall
     */
    public void acceptPhoneCall() {
        Log.d(TAG, "acceptPhoneCall");
        if (serverMessenger != null) {
            Message msg = Message.obtain();
            msg.what = 5;
            try {
                serverMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * rejectPhoneCall
     */
    public void rejectPhoneCall() {
        Log.d(TAG, "rejectPhoneCall");
        if (serverMessenger != null) {
            Message msg = Message.obtain();
            msg.what = 6;
            try {
                serverMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * terminatePhoneCall
     */
    public void terminatePhoneCall() {
        Log.d(TAG, "terminatePhoneCall");
        if (serverMessenger != null) {
            Message msg = Message.obtain();
            msg.what = 4;
            try {
                serverMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        initViews();
    }

    //寻找联系人姓名
    private String queryContactNameByPhoneNumber(String phoneNumber) {
        String contactName = phoneNumber;
        Cursor cursorOriginal = null;
        try {
            cursorOriginal = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME},
                    ContactsContract.CommonDataKinds.Phone.NUMBER + "='" + phoneNumber + "'", null, null);
            if (null != cursorOriginal && cursorOriginal.moveToFirst()) {
                contactName = cursorOriginal.getString(cursorOriginal
                        .getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursorOriginal != null) {
                cursorOriginal.close();
            }

        }
        return contactName;
    }

    //获取联系人照片
    private Bitmap getPhoto(String phoneNumber) {
        Bitmap bitmap = null;
        resolver = getContentResolver();
        InputStream input = null;
        try {
            Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, getContactId(phoneNumber));
            input = ContactsContract.Contacts.openContactPhotoInputStream(resolver, uri);
            if (input != null) {
                bitmap = BitmapFactory.decodeStream(input);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bitmap;
    }

    //获取联系人ID
    private long getContactId(String phoneNumber) {
        resolver = getContentResolver();
        long contactId = 0;
        Cursor cursor = null;
        try {
            Uri uri = Uri.parse("content://com.android.contacts/" + "data/phones/filter/" + phoneNumber);
            cursor = resolver.query(uri, null, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    contactId = cursor.getLong(cursor.getColumnIndex("contact_id"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return contactId;
    }
}
