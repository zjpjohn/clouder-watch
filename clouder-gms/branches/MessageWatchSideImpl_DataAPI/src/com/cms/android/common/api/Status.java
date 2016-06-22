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
package com.cms.android.common.api;

import android.app.PendingIntent;
import android.os.Parcel;
import android.os.Parcelable;

import com.cms.android.common.internal.safeparcel.SafeParcelable;

/**
 * ClassName: CommonStatusCodes
 * 
 * @description CommonStatusCodes
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public class Status extends CommonStatusCodes implements Result, SafeParcelable {
	public static final Parcelable.Creator<Status> CREATOR;

	public static final Status ST_INTERNAL_ERROR;

	public static final Status ST_SUCCESS = new Status(0);

	private String mCode;

	private final PendingIntent mPendingIntent;

	private int mRequestId;

	private int mStatus;

	static {
		ST_INTERNAL_ERROR = new Status(8);
		CREATOR = new Parcelable.Creator<Status>() {
			public Status createFromParcel(Parcel parcel) {
				return new Status(parcel);
			}

			public Status[] newArray(int size) {
				return new Status[size];
			}
		};
	}

	public Status(int status) {
		this(1, status, null, null);
	}

	Status(int requestId, int status, String code, PendingIntent pendingIntent) {
		this.mRequestId = requestId;
		this.mStatus = status;
		this.mCode = code;
		this.mPendingIntent = pendingIntent;
	}

	public Status(int status, String code, PendingIntent pendingIntent) {
		this(1, status, code, pendingIntent);
	}

	private Status(Parcel parcel) {
		this.mRequestId = parcel.readInt();
		this.mStatus = parcel.readInt();
		this.mCode = parcel.readString();
		this.mPendingIntent = ((PendingIntent) parcel.readParcelable(PendingIntent.class.getClassLoader()));
	}

	public String getCode() {
		return CommonStatusCodes.getStatusCodeString(this.mStatus);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public Status getStatus() {
		return this;
	}

	public boolean isSuccess() {
		return this.mStatus <= 0;
	}

	@Override
	public String toString() {
		return "Status [mCode=" + mCode + ", mPendingIntent=" + mPendingIntent + ", mRequestId=" + mRequestId
				+ ", mStatus=" + mStatus + "]";
	}

	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeInt(this.mRequestId);
		parcel.writeInt(this.mStatus);
		parcel.writeString(this.mCode);
		parcel.writeParcelable(this.mPendingIntent, flags);
	}

}