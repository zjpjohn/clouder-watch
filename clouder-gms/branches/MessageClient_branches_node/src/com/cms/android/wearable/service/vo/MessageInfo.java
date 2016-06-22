package com.cms.android.wearable.service.vo;

import java.util.Arrays;

import android.os.Parcel;
import android.os.Parcelable;

public class MessageInfo implements Parcelable {

	private String packageName;

	private String path;

	private long timeStamp;

	private byte[] content;

	public MessageInfo() {
		super();
	}

	private MessageInfo(Parcel in) {
		packageName = in.readString();
		timeStamp = in.readLong();
		in.readByteArray(content);
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public byte[] getContent() {
		return content;
	}

	public void setContent(byte[] content) {
		this.content = content;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(packageName);
		dest.writeLong(timeStamp);
		dest.writeByteArray(content);
	}

	@Override
	public String toString() {
		return "MessageInfo [packageName=" + packageName + ", timeStamp=" + timeStamp + ", content="
				+ new String(content) + "]";
	}

	public static final Parcelable.Creator<MessageInfo> CREATOR = new Parcelable.Creator<MessageInfo>() {
		public MessageInfo createFromParcel(Parcel in) {
			return new MessageInfo(in);
		}

		public MessageInfo[] newArray(int size) {
			return new MessageInfo[size];
		}
	};
}
