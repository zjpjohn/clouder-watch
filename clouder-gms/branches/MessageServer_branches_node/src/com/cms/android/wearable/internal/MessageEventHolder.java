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
package com.cms.android.wearable.internal;

import android.os.Parcel;
import android.os.Parcelable;

import com.cms.android.common.internal.safeparcel.SafeParcelable;
import com.cms.android.wearable.MessageEvent;

/**
 * ClassName: WearableAdapter
 * 
 * @description WearableAdapter
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public class MessageEventHolder implements SafeParcelable, MessageEvent {

	private int mRequestId;

	private String mNodeId;

	private String mPath;

	private byte[] mData;

	public static Parcelable.Creator<MessageEventHolder> CREATOR = new Parcelable.Creator<MessageEventHolder>() {
		public MessageEventHolder createFromParcel(Parcel parcel) {
			return new MessageEventHolder(parcel);
		}

		public MessageEventHolder[] newArray(int size) {
			return new MessageEventHolder[size];
		}
	};

	public MessageEventHolder() {
		super();
	}

	private MessageEventHolder(Parcel in) {
		this.mRequestId = in.readInt();
		this.mNodeId = in.readString();
		this.mPath = in.readString();
		this.mData = new byte[in.readInt()];
		in.readByteArray(this.mData);

	}

	public int describeContents() {
		return 0;
	}

	public int getRequestId() {
		return mRequestId;
	}

	public void setRequestId(int mRequestId) {
		this.mRequestId = mRequestId;
	}

	public void setNodeId(String mNodeId) {
		this.mNodeId = mNodeId;
	}

	public String getPath() {
		return mPath;
	}

	public void setPath(String mPath) {
		this.mPath = mPath;
	}

	public byte[] getData() {
		return mData;
	}

	public void setData(byte[] mData) {
		this.mData = mData;
	}

	public String getSourceNodeId() {
		return this.mNodeId;
	}

	@Override
	public void writeToParcel(Parcel dest, int flag) {
		dest.writeInt(this.mRequestId);
		dest.writeString(this.mNodeId);
		dest.writeString(this.mPath);
		dest.writeInt(this.mData.length);
		dest.writeByteArray(this.mData);
	}

	@Override
	public String toString() {
		return "MessageEventHolder [mRequestId=" + mRequestId + ", mNodeId=" + mNodeId + ", mPath=" + mPath
				+ ", mData=" + new String(mData) + "]";
	}
}