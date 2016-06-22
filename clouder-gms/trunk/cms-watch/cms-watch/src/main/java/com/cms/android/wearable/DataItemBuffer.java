package com.cms.android.wearable;

import java.util.ArrayList;

import com.cms.android.common.api.Result;
import com.cms.android.common.api.Status;
import com.cms.android.common.data.DataBuffer;
import com.cms.android.wearable.internal.DataHolder;

public class DataItemBuffer extends DataBuffer<DataItem> implements Result {
	private final Status status;

	public DataItemBuffer(Status status) {
		this.status = status;
		this.list = new ArrayList<DataItem>();
	}

	public DataItemBuffer(DataHolder dataHolder) {
		super(dataHolder);
		this.status = new Status(dataHolder.getStatusCode());
		this.list = dataHolder.getDataItems();
		if (this.list == null) {
			this.list = new ArrayList<DataItem>();
		}
	}

	public Status getStatus() {
		return this.status;
	}
}