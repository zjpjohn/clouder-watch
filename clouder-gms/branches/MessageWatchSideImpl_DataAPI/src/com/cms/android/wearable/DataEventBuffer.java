package com.cms.android.wearable;

import java.util.ArrayList;

import com.cms.android.common.api.Result;
import com.cms.android.common.api.Status;
import com.cms.android.common.data.DataBuffer;
import com.cms.android.wearable.internal.DataEventParcelable;
import com.cms.android.wearable.internal.DataHolder;

public class DataEventBuffer extends DataBuffer<DataEvent> implements Result {
	private Status status;

	public DataEventBuffer(DataHolder dataHolder) {
		super(dataHolder);
		this.status = new Status(dataHolder.getStatusCode());
		this.list = dataHolder.getDataEvents();
		if (this.list == null)
			this.list = new ArrayList<DataEventParcelable>();
	}

	public Status getStatus() {
		return this.status;
	}
}