package com.clouder.watch.mobile.handler;

import android.app.AlarmManager;
import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import com.clouder.watch.common.sync.IHandler;
import com.clouder.watch.common.sync.IMessageListener;
import com.clouder.watch.common.sync.SyncMessage;
import com.clouder.watch.common.sync.message.CurrentTimeSyncMessage;
import com.clouder.watch.common.utils.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by yang_shoulai on 11/18/2015.
 */
public class CurrentTimeHandler implements IHandler<CurrentTimeSyncMessage>, IMessageListener {

    private static final String TAG = "CurrentTimeHandler";

    private Context context;

    public CurrentTimeHandler(Context context) {
        this.context = context;
    }

    @Override
    public void handle(String path, CurrentTimeSyncMessage message) {

        if (message != null) {
            String timeZoneId = message.getTimeZoneId();
            TimeZone timeZone = TimeZone.getDefault();
            if (!StringUtils.isEmpty(timeZoneId) && !timeZone.getID().equals(timeZoneId)) {
                try {
                    AlarmManager mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    Log.i(TAG, "TimeZone Before Change = " + TimeZone.getDefault());
                    mAlarmManager.setTimeZone(timeZoneId);
                    Log.i(TAG, "System TimeZone After Change = " + TimeZone.getDefault());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            setCurrentTime(message.getYear01(), message.getYear02(), message.getMoth(), message.getDay(),
                    message.getHour(), message.getMinute(), message.getSeconds(), message.getFraction(), message.getTimeZoneId());
        }

    }

    @Override
    public void onMessageReceived(String path, SyncMessage message) {
        handle(path, (CurrentTimeSyncMessage) message);
    }


    private void setCurrentTime(int year01, int year02, int month, int day, int hour, int minute, int second, int fraction, String timeZone) {
        Log.i(TAG, String.format("year01 = %s, year02 = %s, month = %s, day = %s, hour = %s, minute = %s, second = %s, millisecond = %s",
                year01, year02, month, day, hour, minute, second, fraction));
        Calendar cal = Calendar.getInstance();
        int l1 = year01 < 0 ? year01 + 256 : year01;
        int l0 = year02 < 0 ? year02 + 256 : year02;
        int year = (l0 << 8) | l1;
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, second);
        //cal.set(Calendar.DAY_OF_WEEK, value[7]);
        // 无符号byte转化 MILLISECOND分为256份
        int millisecond = fraction & 0xff;
        cal.set(Calendar.MILLISECOND, (int) (1000 * millisecond / 256f));
        cal.setTimeZone(TimeZone.getTimeZone(timeZone));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");
        Log.i(TAG, "时间同步, 调整时间为" + sdf.format(cal.getTime()) + ",TimeZone = " + cal.getTimeZone());
        // 将调整的时间通过广播方式发送出去
        long currentTime = cal.getTime().getTime();
        try {
            boolean isSuccess = SystemClock.setCurrentTimeMillis(currentTime);
            if (isSuccess) {
                Log.d(TAG, "设置当前时间成功");
            } else {
                Log.e(TAG, "设置当前时间失败");
            }

        } catch (Exception e) {
            Log.e(TAG, "设置当前时间异常", e);
        }
    }

}
