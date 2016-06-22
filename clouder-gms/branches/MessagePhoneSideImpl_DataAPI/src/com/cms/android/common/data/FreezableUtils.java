package com.cms.android.common.data;

import java.util.ArrayList;
import java.util.Iterator;

public class FreezableUtils {
	
	public static <T, E extends Freezable<T>> ArrayList<T> freeze(ArrayList<E> arrayList) {
		ArrayList<T> localArrayList = new ArrayList<T>(arrayList.size());
		Iterator<E> iterator = arrayList.iterator();
		while (iterator.hasNext()) {
			Freezable<T> freezable = (Freezable<T>) iterator.next();
			localArrayList.add(freezable.freeze());
		}
		return localArrayList;
	}

	public static <T, E extends Freezable<T>> ArrayList<T> freeze(E[] array) {
		ArrayList<T> arrayList = new ArrayList<T>(array.length);
		for (E e : array)
			arrayList.add(e.freeze());
		return arrayList;
	}

	public static <T, E extends Freezable<T>> ArrayList<T> freezeIterable(Iterable<E> iterable) {
		ArrayList<T> arrayList = new ArrayList<T>();
		Iterator<E> iterator = iterable.iterator();
		while (iterator.hasNext())
			arrayList.add((iterator.next()).freeze());
		return arrayList;
	}
}
