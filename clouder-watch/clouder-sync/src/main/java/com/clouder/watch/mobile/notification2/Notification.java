package com.clouder.watch.mobile.notification2;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by yang_shoulai on 12/8/2015.
 */
public class Notification implements Parcelable {

    private String uuid;

    private String id;

    private String title;

    private Bitmap icon;

    private String content;

    private String date;

    private String time;

    private String packageName;

    private boolean shock;

    protected Notification() {

    }


    protected Notification(Parcel in) {
        uuid = in.readString();
        id = in.readString();
        title = in.readString();
        icon = in.readParcelable(Bitmap.class.getClassLoader());
        content = in.readString();
        date = in.readString();
        time = in.readString();
        packageName = in.readString();
        shock = in.readByte() != 0;
    }

    public static final Creator<Notification> CREATOR = new Creator<Notification>() {
        @Override
        public Notification createFromParcel(Parcel in) {
            return new Notification(in);
        }

        @Override
        public Notification[] newArray(int size) {
            return new Notification[size];
        }
    };

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[uuid = ").append(uuid).append(", ");
        stringBuilder.append("id = ").append(id).append(", ");
        stringBuilder.append("package = ").append(packageName).append(", ");
        stringBuilder.append("shock = ").append(shock).append(", ");
        stringBuilder.append("title = ").append(title).append(", ");
        stringBuilder.append("content = ").append(content).append(", ");
        stringBuilder.append("date = ").append(date).append(", ");
        stringBuilder.append("time = ").append(time).append("]");
        return stringBuilder.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uuid);
        dest.writeString(id);
        dest.writeString(title);
        dest.writeParcelable(icon, flags);
        dest.writeString(content);
        dest.writeString(date);
        dest.writeString(time);
        dest.writeString(packageName);
        dest.writeByte((byte) (shock ? 1 : 0));
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isShock() {
        return shock;
    }

    public void setShock(boolean shock) {
        this.shock = shock;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Bitmap getIcon() {
        return icon;
    }

    public void setIcon(Bitmap icon) {
        this.icon = icon;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
}
