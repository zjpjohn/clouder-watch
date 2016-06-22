package com.clouder.watch.locker;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

//import com.clouder.watch.common.utils.SettingSharedPreferences;

/**
 * Created by zhou_wenchong on 7/16/2015.
 */
public class LockService extends Service {

    private String TAG = "LockService";
    private Intent zdLockIntent = null;
    private TelephonyManager tm;
    private boolean doLock = true;
    private int onHour, onMin, onSec, offHour, offMin, offSec, timeIndex;
    public static final String SCREEN_LOCKER_ENABLE = "com.clouder.watch.settings.screen_locker_enable";

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        zdLockIntent = new Intent(LockService.this, UnLockActivity.class);
        zdLockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        IntentFilter mScreenOnFilter = new IntentFilter("android.intent.action.SCREEN_ON");
        LockService.this.registerReceiver(mScreenOnReceiver, mScreenOnFilter);

        IntentFilter mScreenOffFilter = new IntentFilter("android.intent.action.SCREEN_OFF");
        LockService.this.registerReceiver(mScreenOffReceiver, mScreenOffFilter);


        tm = (TelephonyManager) this
                .getSystemService(Context.TELEPHONY_SERVICE);
        LockPhoneCallListener myPhoneCallListener = new LockPhoneCallListener();
        tm.listen(myPhoneCallListener, PhoneStateListener.LISTEN_CALL_STATE);
    }


    @Override
    public void onDestroy() {
        Log.i(TAG, "----------------- onDestroy------");
        super.onDestroy();
        this.unregisterReceiver(mScreenOnReceiver);
        this.unregisterReceiver(mScreenOffReceiver);
        this.unregisterReceiver(mScreenOffReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand..........");
        String passWord = intent.getStringExtra("newPassword");
        Log.d(TAG, "newPassWord:" + passWord);
        if (passWord != null) {
            SharedPreferences sharedPreference = getSharedPreferences("configuration", 0);
            SharedPreferences.Editor editor = sharedPreference.edit();
            editor.putString("PassWord", passWord);
            editor.commit();
        }
        return START_REDELIVER_INTENT;
    }

    private BroadcastReceiver mScreenOnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.SCREEN_ON")) {
                Log.i(TAG, "----------------- android.intent.action.SCREEN_ON------");
                Calendar c = Calendar.getInstance();
                onHour = c.get(Calendar.HOUR);
                onMin = c.get(Calendar.MINUTE);
                onSec = c.get(Calendar.SECOND);
                if (TimeIndex() <= 2) {
                    doLock = false;
                }
                Log.i("TimeIndex", "" + TimeIndex());
            }
        }
    };

    private BroadcastReceiver mScreenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Settings.System.getInt(getContentResolver(), SCREEN_LOCKER_ENABLE, 1) == 1) {
                doLock = true;
            }
            if (Settings.System.getInt(getContentResolver(), SCREEN_LOCKER_ENABLE, 1) == 0) {
                doLock = false;
            }
            if (action.equals("android.intent.action.SCREEN_OFF")) {
                Log.i(TAG, "----------------- android.intent.action.SCREEN_OFF------");
                Calendar c = Calendar.getInstance();
                offHour = c.get(Calendar.HOUR);
                offMin = c.get(Calendar.MINUTE);
                offSec = c.get(Calendar.SECOND);
                TimerTask task = new TimerTask() {
                    public void run() {
                        if (doLock == true) {
                            startActivity(zdLockIntent);
                        }
                    }
                };
                Timer timer = new Timer();
                timer.schedule(task, 2000);

            }
        }
    };

    public class LockPhoneCallListener extends PhoneStateListener {
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:
                    doLock = true;
                    Log.v(TAG, "=============CALL_STATE_IDLE:================");
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    Log.v(TAG, "=============CALL_STATE_OFFHOOK:================");
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    Log.v(TAG, "=============CALL_STATE_RINGING:================");
                    doLock = false;
                    break;
                default:
                    break;
            }
        }
    }

    private int TimeIndex() {

        if (onHour == 0 && onMin == 0 && onSec == 0) {
            timeIndex = 2;
        } else {
            timeIndex = (onHour - offHour) * 60 * 60 + (onMin - offMin) * 60 + (onSec - offSec);
        }
        return timeIndex;
    }
}
