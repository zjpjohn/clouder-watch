package com.clouder.watch.common.sync.message;

import com.clouder.watch.common.sync.SyncMessage;

import java.util.Calendar;

/**
 * Created by yang_shoulai on 11/18/2015.
 */
public class CurrentTimeSyncMessage extends SyncMessage {

    private int year01;

    private int year02;

    private int moth;

    private int day;

    private int hour;

    private int minute;

    private int seconds;

    private int dayOfWeek;

    private int fraction;

    private String timeZoneId;


    public CurrentTimeSyncMessage(String path, long currentTime) {
        super(path);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(currentTime);
        int year = cal.get(Calendar.YEAR);
        year01 = (byte) (year & 0xff);
        year02 = (byte) (year >> 8 & 0xff);
        // month
        moth = cal.get(Calendar.MONTH);
        // day
        day = (byte) cal.get(Calendar.DAY_OF_MONTH);
        // hour
        hour = (byte) cal.get(Calendar.HOUR_OF_DAY);
        // minute
        minute = (byte) cal.get(Calendar.MINUTE);
        // second
        seconds = (byte) cal.get(Calendar.SECOND);
        // day of week
        dayOfWeek = (byte) cal.get(Calendar.DAY_OF_WEEK);
        // fraction
        fraction = (byte) (cal.get(Calendar.MILLISECOND) * 256 / 1000f);
    }

    public String getTimeZoneId() {
        return timeZoneId;
    }

    public void setTimeZoneId(String timeZoneId) {
        this.timeZoneId = timeZoneId;
    }

    public int getYear01() {
        return year01;
    }

    public void setYear01(int year01) {
        this.year01 = year01;
    }

    public int getYear02() {
        return year02;
    }

    public void setYear02(int year02) {
        this.year02 = year02;
    }

    public int getMoth() {
        return moth;
    }

    public void setMoth(int moth) {
        this.moth = moth;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public int getSeconds() {
        return seconds;
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public int getFraction() {
        return fraction;
    }

    public void setFraction(int fraction) {
        this.fraction = fraction;
    }
}
