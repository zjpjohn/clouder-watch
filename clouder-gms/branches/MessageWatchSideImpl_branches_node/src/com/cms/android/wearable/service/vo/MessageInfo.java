/*****************************************************************************
 *
 *                      HOPERUN PROPRIETARY INFORMATION
 *
 *          The information contained herein is proprietary to HopeRun
 *           and shall not be reproduced or disclosed in whole or in part
 *                    or used for any design or manufacture
 *              without direct written authorization from HopeRun.
 *
 *            Copyright (coffee) 2015 by HopeRun.  All rights reserved.
 *
 *****************************************************************************/
package com.cms.android.wearable.service.vo;

import java.util.Arrays;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * ClassName: MessageInfo
 * 
 * @description MessageInfo
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public class MessageInfo implements Parcelable {

	private String packageName;

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
				+ Arrays.toString(content) + "]";
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
