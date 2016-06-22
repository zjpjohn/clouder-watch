package com.example.testasset;


public abstract interface DataEvent extends Freezable<DataEvent> {
	public static final int TYPE_CHANGED = 1;
	public static final int TYPE_DELETED = 2;

	public abstract DataItem getDataItem();

	public abstract int getType();
}