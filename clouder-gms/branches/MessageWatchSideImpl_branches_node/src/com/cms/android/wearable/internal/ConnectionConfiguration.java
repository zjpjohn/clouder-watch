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

import java.util.Arrays;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.cms.android.common.internal.safeparcel.SafeParcelable;

/**
 * ClassName: ConnectionConfiguration
 * 
 * @description ConnectionConfiguration
 * @author xing_pengfei
 * @Date 2015-7-29
 * 
 */
public class ConnectionConfiguration implements SafeParcelable {

	public static final Parcelable.Creator<ConnectionConfiguration> CREATOR = new Parcelable.Creator<ConnectionConfiguration>() {
		public ConnectionConfiguration createFromParcel(Parcel parcel) {
			int id = parcel.readInt();
			String name = parcel.readString();
			String address = parcel.readString();
			int type = parcel.readInt();
			int role = parcel.readInt();
			boolean enabled = parcel.readInt() == 1;
			return new ConnectionConfiguration(id, name, address, type, role, enabled);
		}

		public ConnectionConfiguration[] newArray(int size) {
			return new ConnectionConfiguration[size];
		}
	};
	private final String mAddress;
	private final boolean mEnabled;
	final int mId;
	private final String mName;
	private final int mRole;
	private final int mType;

	private ConnectionConfiguration(int id, String name, String address, int type, int role, boolean enabled) {
		this.mId = id;
		this.mName = name;
		this.mAddress = address;
		this.mType = type;
		this.mRole = role;
		this.mEnabled = enabled;
	}

	public int describeContents() {
		return 0;
	}

	public boolean equals(Object obj) {
		if (obj instanceof ConnectionConfiguration) {
			ConnectionConfiguration config = (ConnectionConfiguration) obj;
			return config.mId == this.mId && !TextUtils.isEmpty(config.mName) && config.mName.equals(this.mName)
					&& !TextUtils.isEmpty(config.mAddress) && config.mAddress.equals(this.mAddress)
					&& config.mRole == this.mRole && config.mType == this.mType && config.mEnabled == this.mEnabled;
		}
		return false;
	}

	public String getAddress() {
		return this.mAddress;
	}

	public int hashCode() {
		Object[] arrayOfObject = new Object[6];
		arrayOfObject[0] = Integer.valueOf(this.mId);
		arrayOfObject[1] = this.mName;
		arrayOfObject[2] = this.mAddress;
		arrayOfObject[3] = Integer.valueOf(this.mType);
		arrayOfObject[4] = Integer.valueOf(this.mRole);
		arrayOfObject[5] = Boolean.valueOf(this.mEnabled);
		return Arrays.hashCode(arrayOfObject);
	}

	public String toString() {
		return "ConnectionConfiguration[ " + "mName=" + this.mName + ", mAddress=" + this.mAddress + ", mType="
				+ this.mType + ", mRole=" + this.mRole + ", mEnabled=" + this.mEnabled + "]";
	}

	public void writeToParcel(Parcel paramParcel, int paramInt) {
		paramParcel.writeInt(this.mId);
		paramParcel.writeString(this.mName);
		paramParcel.writeString(this.mAddress);
		paramParcel.writeInt(this.mType);
		paramParcel.writeInt(this.mRole);
		paramParcel.writeInt(this.mEnabled ? 1 : 0);
	}
}