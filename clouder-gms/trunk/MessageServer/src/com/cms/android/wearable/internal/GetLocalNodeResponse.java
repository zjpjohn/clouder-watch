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
 * ClassName: GetLocalNodeResponse
 *
 * @description
 * @author xing_peng
 * @Date 2015-8-18
 * 
 */
public class GetLocalNodeResponse implements SafeParcelable {

	public static final Parcelable.Creator<GetLocalNodeResponse> CREATOR = new Parcelable.Creator<GetLocalNodeResponse>() {
		public GetLocalNodeResponse createFromParcel(Parcel parcel) {
			return new GetLocalNodeResponse(parcel);
		}

		public GetLocalNodeResponse[] newArray(int size) {
			return new GetLocalNodeResponse[size];
		}
	};

	private Node node;

	public GetLocalNodeResponse() {
		super();
	}
	
	public GetLocalNodeResponse(Parcel parcel) {
		this.node = (NodeHolder) parcel.readParcelable(NodeHolder.class.getClassLoader());
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public Node getNode() {
		return node;
	}

	public void setNode(Node node) {
		this.node = node;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(new NodeHolder(this.node.getId(), this.node.getDisplayName()), 0);
	}

}
