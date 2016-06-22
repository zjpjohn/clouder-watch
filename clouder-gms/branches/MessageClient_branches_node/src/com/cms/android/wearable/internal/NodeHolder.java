/*****************************************************************************
 *
 *                      HOPERUN PROPRIETARY INFORMATION
 *
 *          The information contained herein is proprietary to HopeRun
 *           and shall not be reproduced or disclosed in whole or in part
 *                    or used for any design or manufacture
 *              without direct written authorization from HopeRun.
 *
 *            Copyright (c) 2014 by HopeRun.  All rights reserved.
 *
 *****************************************************************************/
package com.cms.android.wearable.internal;

import android.os.Parcel;
import android.os.Parcelable;

import com.cms.android.common.internal.safeparcel.SafeParcelable;
import com.cms.android.wearable.Node;

/**
 * ClassName: NodeHolder
 *
 * @description
 * @author xing_peng
 * @Date 2015-8-14
 * 
 */
public class NodeHolder implements SafeParcelable, Node {

	private String mDisplayName;

	private String mId;

	public static final Parcelable.Creator<NodeHolder> CREATOR = new Parcelable.Creator<NodeHolder>() {
		public NodeHolder createFromParcel(Parcel parcel) {
			return new NodeHolder(parcel);
		}

		public NodeHolder[] newArray(int paramInt) {
			return new NodeHolder[paramInt];
		}
	};

	private NodeHolder(Parcel parcel) {
		this.mId = parcel.readString();
		this.mDisplayName = parcel.readString();
	}

	public NodeHolder(String id, String displayName) {
		this.mId = id;
		this.mDisplayName = displayName;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.mId);
		dest.writeString(this.mDisplayName);
	}

	@Override
	public String getDisplayName() {
		return this.mDisplayName;
	}

	@Override
	public String getId() {
		return this.mId;
	}

	public int hashCode() {
		return this.mId.hashCode();
	}

	public boolean equals(Object obj) {
		return (obj instanceof NodeHolder) && this.mId.equals(((NodeHolder) obj).getId());
	}

	@Override
	public String toString() {
		return "NodeHolder [mId=" + mId + ", mDisplayName=" + mDisplayName + "]";
	}
	
}
