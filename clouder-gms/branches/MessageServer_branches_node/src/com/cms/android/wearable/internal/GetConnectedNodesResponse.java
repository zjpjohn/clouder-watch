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

import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

import com.cms.android.common.internal.safeparcel.SafeParcelable;
import com.cms.android.wearable.Node;

/**
 * ClassName: GetConnectedNodesResponse
 *
 * @description
 * @author xing_peng
 * @Date 2015-8-17
 * 
 */
public class GetConnectedNodesResponse implements SafeParcelable {

	public static final Parcelable.Creator<GetConnectedNodesResponse> CREATOR = new Parcelable.Creator<GetConnectedNodesResponse>() {
		public GetConnectedNodesResponse createFromParcel(Parcel parcel) {
			return new GetConnectedNodesResponse(parcel);
		}

		public GetConnectedNodesResponse[] newArray(int size) {
			return new GetConnectedNodesResponse[size];
		}
	};

	private List<Node> nodes;

	public GetConnectedNodesResponse() {
		super();
	}
	
	public GetConnectedNodesResponse(Parcel parcel) {
		Parcelable[] arrayOfParcelable = parcel.readParcelableArray(NodeHolder.class.getClassLoader());
		this.nodes = new ArrayList<Node>(arrayOfParcelable.length);
		for (int i = 0; i < arrayOfParcelable.length; i++) {
			NodeHolder localNodeHolder = (NodeHolder) arrayOfParcelable[i];
			this.nodes.add(localNodeHolder);
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public List<Node> getNodes() {
		return this.nodes;
	}

	public void setNodes(List<Node> nodes) {
		this.nodes = nodes;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		NodeHolder[] arrayOfNodeHolder = new NodeHolder[this.nodes.size()];
		for (int i = 0; i < this.nodes.size(); i++)
			arrayOfNodeHolder[i] = new NodeHolder(((Node) this.nodes.get(i)).getId(),
					((Node) this.nodes.get(i)).getDisplayName());
		dest.writeParcelableArray(arrayOfNodeHolder, 0);
	}

}
