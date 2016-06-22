package com.cms.android.common.data;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class DataBufferIterator<T> implements Iterator<T> {

	protected final DataBuffer<T> dataBuffer;

	protected int index;

	public DataBufferIterator(DataBuffer<T> dataBuffer) {
		this.dataBuffer = dataBuffer;
		this.index = -1;
	}

	public boolean hasNext() {
		return this.index < -1 + this.dataBuffer.getCount();
	}

	public T next() {
		if (!hasNext())
			throw new NoSuchElementException("no next element, index = " + this.index);
		DataBuffer<T> localDataBuffer = this.dataBuffer;
		int i = 1 + this.index;
		this.index = i;
		return localDataBuffer.get(i);
	}

	public void remove() {
		throw new UnsupportedOperationException("unsupported.");
	}
}