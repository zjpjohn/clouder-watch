package com.cms.android.common.data;

import java.util.Iterator;
import java.util.List;

import com.cms.android.common.api.Releasable;
import com.cms.android.wearable.internal.DataHolder;

public class DataBuffer<T> implements Releasable, Iterable<T> {

	protected DataHolder dataHolder;

	protected List<? extends T> list;

	protected DataBuffer() {
	}

	protected DataBuffer(DataHolder dataHolder) {
		this.dataHolder = dataHolder;
	}

	public T get(int index) {
		return this.list.get(index);
	}

	public int getCount() {
		return this.list.size();
	}

	public Iterator<T> iterator() {
		return new DataBufferIterator<T>(this);
	}

	public void release() {
	}
}