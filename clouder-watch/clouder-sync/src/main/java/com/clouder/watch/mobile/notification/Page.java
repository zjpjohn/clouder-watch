package com.clouder.watch.mobile.notification;

import java.io.Serializable;

/**
 * Created by zhou_wenchong on 9/9/2015.
 */
public class Page implements Serializable {

    private String titleRes;
    private String textRes;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    private String id;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    private String uuid;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    private String date;

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    private String time;


    public byte[] getIconRes() {
        return iconRes;
    }

    public void setIconRes(byte[] iconRes) {
        this.iconRes = iconRes;
    }

    private byte[] iconRes;

    public Page() {
    }

    public String getTitleRes() {
        return titleRes;
    }

    public void setTitleRes(String titleRes) {
        this.titleRes = titleRes;
    }

    public String getTextRes() {
        return textRes;
    }

    public void setTextRes(String textRes) {
        this.textRes = textRes;
    }


    public Page(String titleRes, String textRes, String date, String time, byte[] iconRes, String uuid, String id) {
        this.titleRes = titleRes;
        this.textRes = textRes;
        this.date = date;
        this.time = time;
        this.iconRes = iconRes;
        this.uuid = uuid;
        this.id = id;
    }
}
