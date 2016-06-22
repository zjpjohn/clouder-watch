package com.cms.android.wearable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

public class DataMap {

	public static final String TAG = "DataMap";

	public static final String PATH_SEPARATOR = "_@_";
	public static final String INDEX_SEPARATOR = "_#_";
	private final Map<String, Object> map = new HashMap<String, Object>();

	public DataMap() {
	}

	@SuppressWarnings("rawtypes")
	private DataMap(Bundle bundle) {
		if (bundle == null)
			return;
		Iterator<String> iterator = bundle.keySet().iterator();
		while (iterator.hasNext()) {
			String str = (String) iterator.next();
			Object obj1 = bundle.get(str);
			if ((obj1 instanceof Bundle)) {
				this.map.put(str, fromBundle((Bundle) obj1));
			} else if (((obj1 instanceof ArrayList)) && (!((ArrayList) obj1).isEmpty())
					&& ((((ArrayList) obj1).get(0) instanceof Bundle))) {
				ArrayList<DataMap> dataMapArrayList = new ArrayList<DataMap>();
				Iterator iterator2 = ((ArrayList) obj1).iterator();
				while (iterator2.hasNext()) {
					Object obj2 = iterator2.next();
					dataMapArrayList.add(fromBundle((Bundle) obj2));
				}
				this.map.put(str, dataMapArrayList);
			} else {
				this.map.put(str, obj1);
			}
		}
	}

	public static DataMap fromBundle(Bundle bundle) {
		bundle.setClassLoader(Asset.class.getClassLoader());
		return new DataMap(bundle);
	}

	public static DataMap fromByteArray(byte[] data) {
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(data);
			DataInputStream localDataInputStream = new DataInputStream(bis);
			DataMap dataMap = new DataMap();
			dataMap.readFields(localDataInputStream);
			bis.close();
			return dataMap;
		} catch (IOException e) {
			Log.d(TAG, "IOException", e);
		}
		return null;
	}

	public static ArrayList<DataMap> arrayListFromBundleArrayList(ArrayList<Bundle> arrayList) {
		ArrayList<DataMap> dataMapArrayList = new ArrayList<DataMap>();
		Iterator<Bundle> iterator = arrayList.iterator();
		while (iterator.hasNext()) {
			Bundle bundle = (Bundle) iterator.next();
			dataMapArrayList.add(fromBundle(bundle));
		}
		return dataMapArrayList;
	}

	public void readFields(DataInput dataInput) throws IOException {
		int i = dataInput.readInt();
		for (int j = 0; j < i; j++) {
			String str = dataInput.readUTF();
			int k = dataInput.readByte();
			if (k == -1) {
				this.map.put(str, null);
			} else if (k == 0) {
				this.map.put(str, Boolean.valueOf(dataInput.readBoolean()));
			} else if (k == 1) {
				this.map.put(str, Byte.valueOf(dataInput.readByte()));
			} else if (k == 2) {
				this.map.put(str, Integer.valueOf(dataInput.readInt()));
			} else if (k == 3) {
				this.map.put(str, Long.valueOf(dataInput.readLong()));
			} else if (k == 4) {
				this.map.put(str, Float.valueOf(dataInput.readFloat()));
			} else if (k == 5) {
				this.map.put(str, Double.valueOf(dataInput.readDouble()));
			} else if (k == 6) {
				this.map.put(str, dataInput.readUTF());
			} else if (k == 7) {
				DataMap dataMap = new DataMap();
				dataMap.readFields(dataInput);
				this.map.put(str, dataMap);
			} else {
				int count;
				if (k == 8) {
					count = dataInput.readInt();
					byte[] bytes = new byte[count];
					dataInput.readFully((byte[]) bytes);
					this.map.put(str, bytes);
				} else {
					int n;
					if (k == 9) {
						count = dataInput.readInt();
						long[] longs = new long[count];
						for (n = 0; n < count; n++)
							longs[n] = dataInput.readLong();
						this.map.put(str, longs);
					} else if (k == 10) {
						count = dataInput.readInt();
						float[] floats = new float[count];
						for (n = 0; n < count; n++)
							floats[n] = dataInput.readFloat();
						this.map.put(str, floats);
					} else if (k == 11) {
						count = dataInput.readInt();
						String[] strings = new String[count];
						for (n = 0; n < count; n++)
							strings[n] = dataInput.readUTF();
						this.map.put(str, strings);
					} else if (k == 12) {
						count = dataInput.readInt();
						ArrayList<String> list = new ArrayList<String>();
						for (n = 0; n < count; n++)
							list.add(dataInput.readUTF());
						this.map.put(str, list);
					} else if (k == 13) {
						count = dataInput.readInt();
						ArrayList<DataMap> dataMaps = new ArrayList<DataMap>();
						for (n = 0; n < count; n++) {
							DataMap dataMap = new DataMap();
							dataMap.readFields(dataInput);
							dataMaps.add(dataMap);
						}
						this.map.put(str, dataMaps);
					} else {
						if (k != 14)
							continue;
						count = dataInput.readInt();
						ArrayList<Integer> ints = new ArrayList<Integer>();
						for (n = 0; n < count; n++)
							ints.add(dataInput.readInt());
						this.map.put(str, ints);
					}
				}
			}
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Bundle toBundle() {
		Bundle bundle = new Bundle();
		Iterator<String> iterator1 = this.map.keySet().iterator();
		while (iterator1.hasNext()) {
			String str = (String) iterator1.next();
			Object obj1 = this.map.get(str);
			if (obj1 == null)
				continue;
			if ((obj1 instanceof Boolean)) {
				bundle.putBoolean(str, ((Boolean) obj1).booleanValue());
			} else if ((obj1 instanceof Byte)) {
				bundle.putByte(str, ((Byte) obj1).byteValue());
			} else if ((obj1 instanceof Integer)) {
				bundle.putInt(str, ((Integer) obj1).intValue());
			} else if ((obj1 instanceof Long)) {
				bundle.putLong(str, ((Long) obj1).longValue());
			} else if ((obj1 instanceof Float)) {
				bundle.putFloat(str, ((Float) obj1).floatValue());
			} else if ((obj1 instanceof Double)) {
				bundle.putDouble(str, ((Double) obj1).doubleValue());
			} else if ((obj1 instanceof String)) {
				bundle.putString(str, (String) obj1);
			} else if ((obj1 instanceof Asset)) {
				bundle.putParcelable(str, (Asset) obj1);
			} else if ((obj1 instanceof DataMap)) {
				bundle.putBundle(str, ((DataMap) obj1).toBundle());
			} else if ((obj1 instanceof byte[])) {
				bundle.putByteArray(str, (byte[]) (byte[]) obj1);
			} else if ((obj1 instanceof long[])) {
				bundle.putLongArray(str, (long[]) (long[]) obj1);
			} else if ((obj1 instanceof float[])) {
				bundle.putFloatArray(str, (float[]) (float[]) obj1);
			} else if ((obj1 instanceof String[])) {
				bundle.putStringArray(str, (String[]) (String[]) obj1);
			} else if ((obj1 instanceof ArrayList)) {
				ArrayList arrayList = (ArrayList) obj1;
				if ((arrayList.size() == 0) || ((arrayList.get(0) instanceof String))) {
					bundle.putStringArrayList(str, arrayList);
				} else if ((arrayList.get(0) instanceof DataMap)) {
					ArrayList dataMapList = new ArrayList();
					Iterator dataMapIterator = arrayList.iterator();
					while (dataMapIterator.hasNext()) {
						Object dataMap = dataMapIterator.next();
						dataMapList.add(((DataMap) dataMap).toBundle());
					}
					bundle.putParcelableArrayList(str, dataMapList);
				} else if ((arrayList.get(0) instanceof Integer)) {
					bundle.putIntegerArrayList(str, arrayList);
				}
			}
		}
		return bundle;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void writeFields(DataOutput dataOutput) throws IOException {
		dataOutput.writeInt(this.map.size());
		Iterator<String> iterator = this.map.keySet().iterator();
		while (iterator.hasNext()) {
			String key = (String) iterator.next();
			dataOutput.writeUTF(key);
			Object obj = this.map.get(key);
			if (obj == null) {
				dataOutput.writeByte(-1);
			} else if ((obj instanceof Boolean)) {
				dataOutput.writeByte(0);
				dataOutput.writeBoolean(((Boolean) obj).booleanValue());
			} else if ((obj instanceof Byte)) {
				dataOutput.writeByte(1);
				dataOutput.writeByte(((Byte) obj).byteValue());
			} else if ((obj instanceof Integer)) {
				dataOutput.writeByte(2);
				dataOutput.writeInt(((Integer) obj).intValue());
			} else if ((obj instanceof Long)) {
				dataOutput.writeByte(3);
				dataOutput.writeLong(((Long) obj).longValue());
			} else if ((obj instanceof Float)) {
				dataOutput.writeByte(4);
				dataOutput.writeFloat(((Float) obj).floatValue());
			} else if ((obj instanceof Double)) {
				dataOutput.writeByte(5);
				dataOutput.writeDouble(((Double) obj).doubleValue());
			} else if ((obj instanceof String)) {
				dataOutput.writeByte(6);
				dataOutput.writeUTF((String) obj);
			} else if ((obj instanceof DataMap)) {
				dataOutput.writeByte(7);
				((DataMap) obj).writeFields(dataOutput);
			} else {
				Object dest;
				if ((obj instanceof byte[])) {
					dataOutput.writeByte(8);
					dest = (byte[]) obj;
					dataOutput.writeInt(((byte[]) dest).length);
					dataOutput.write((byte[]) dest);
				} else if ((obj instanceof long[])) {
					dataOutput.writeByte(9);
					dest = (long[]) obj;
					dataOutput.writeInt(((long[]) dest).length);
					for (long l : ((long[]) obj))
						dataOutput.writeLong(l);
				} else if ((obj instanceof float[])) {
					dataOutput.writeByte(10);
					dest = (float[]) obj;
					dataOutput.writeInt(((float[]) dest).length);
					for (float f : ((float[]) dest))
						dataOutput.writeFloat(f);
				} else if ((obj instanceof String[])) {
					dataOutput.writeByte(11);
					dest = (String[]) obj;
					dataOutput.writeInt(((String[]) dest).length);
					for (String str2 : ((String[]) dest)) {
						if (str2 != null) {
							dataOutput.writeUTF(str2);
						} else {
							dataOutput.writeUTF("");
						}
					}
				} else if ((obj instanceof ArrayList)) {
					dest = (ArrayList) obj;
					int i;
					if ((((ArrayList) dest).size() == 0) || ((((ArrayList) dest).get(0) instanceof String))) {
						dataOutput.writeByte(12);
						dataOutput.writeInt(((ArrayList<String>) dest).size());
						for (i = 0; i < ((ArrayList<String>) dest).size(); i++) {
							if (((ArrayList<String>) dest).get(i) != null) {
								dataOutput.writeUTF((String) ((ArrayList<String>) dest).get(i));
							} else {
								dataOutput.writeUTF("");
							}
						}
					} else if ((((ArrayList) dest).get(0) instanceof DataMap)) {
						dataOutput.writeByte(13);
						dataOutput.writeInt(((ArrayList) dest).size());
						for (i = 0; i < ((ArrayList) dest).size(); i++) {
							if (((ArrayList) dest).get(i) != null) {
								((DataMap) ((ArrayList) dest).get(i)).writeFields(dataOutput);
							} else {
								new DataMap().writeFields(dataOutput);
							}
						}
					} else if ((((ArrayList) dest).get(0) instanceof Integer)) {
						dataOutput.writeByte(14);
						dataOutput.writeInt(((ArrayList) dest).size());
						for (i = 0; i < ((ArrayList) dest).size(); i++) {
							if (((ArrayList) dest).get(i) != null) {
								dataOutput.writeInt(((Integer) ((ArrayList) dest).get(i)).intValue());
							} else {
								dataOutput.writeInt(0);
							}
						}
					}
				}
			}
		}
	}

	public byte[] toByteArray() {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(bos);
			writeFields(dos);
			bos.flush();
			bos.close();
			byte[] data = bos.toByteArray();
			return data;
		} catch (IOException e) {
			Log.d(TAG, "IOException", e);
		}
		return null;
	}

	public int size() {
		return this.map.size();
	}

	public boolean isEmpty() {
		return this.map.isEmpty();
	}

	public void clear() {
		this.map.clear();
	}

	public boolean containsKey(String key) {
		return this.map.containsKey(key);
	}

	public Object get(String key) {
		return this.map.get(key);
	}

	public Object remove(String key) {
		Object obj = this.map.get(key);
		this.map.remove(key);
		return obj;
	}

	public void putAll(DataMap dataMap) {
		Iterator<String> iterator = dataMap.keySet().iterator();
		while (iterator.hasNext()) {
			String str = (String) iterator.next();
			this.map.put(str, dataMap.get(str));
		}
	}

	public Set<String> keySet() {
		return this.map.keySet();
	}

	public void putBoolean(String key, boolean value) {
		this.map.put(key, Boolean.valueOf(value));
	}

	public void putByte(String key, byte value) {
		this.map.put(key, Byte.valueOf(value));
	}

	public void putInt(String key, int value) {
		this.map.put(key, Integer.valueOf(value));
	}

	public void putLong(String key, long value) {
		this.map.put(key, Long.valueOf(value));
	}

	public void putFloat(String key, float value) {
		this.map.put(key, Float.valueOf(value));
	}

	public void putDouble(String key, double value) {
		this.map.put(key, Double.valueOf(value));
	}

	public void putString(String key, String value) {
		this.map.put(key, value);
	}

	public void putAsset(String key, Asset asset) {
		this.map.put(key, asset);
	}

	public void putDataMap(String key, DataMap dataMap) {
		this.map.put(key, dataMap);
	}

	public void putDataMapArrayList(String key, ArrayList<DataMap> arrayList) {
		this.map.put(key, arrayList);
	}

	public void putIntegerArrayList(String key, ArrayList<Integer> arrayList) {
		this.map.put(key, arrayList);
	}

	public void putStringArrayList(String key, ArrayList<String> arrayList) {
		this.map.put(key, arrayList);
	}

	public void putByteArray(String key, byte[] bytes) {
		this.map.put(key, bytes);
	}

	public void putLongArray(String key, long[] array) {
		this.map.put(key, array);
	}

	public void putFloatArray(String key, float[] array) {
		this.map.put(key, array);
	}

	public void putStringArray(String key, String[] array) {
		this.map.put(key, array);
	}

	public boolean getBoolean(String key) {
		return getBoolean(key, false);
	}

	public boolean getBoolean(String key, boolean defaultValue) {
		Object obj = this.map.get(key);
		if (obj == null)
			return defaultValue;
		try {
			return ((Boolean) obj).booleanValue();
		} catch (ClassCastException e) {
			log(key, obj, "Boolean", Boolean.valueOf(defaultValue), e);
		}
		return defaultValue;
	}

	public byte getByte(String key) {
		return getByte(key, (byte) 0);
	}

	public byte getByte(String key, byte defaultByte) {
		Object localObject = this.map.get(key);
		if (localObject == null)
			return defaultByte;
		try {
			return ((Byte) localObject).byteValue();
		} catch (ClassCastException e) {
			log(key, localObject, "Byte", Byte.valueOf(defaultByte), e);
		}
		return defaultByte;
	}

	public int getInt(String key) {
		return getInt(key, 0);
	}

	public int getInt(String key, int defaultValue) {
		Object obj = this.map.get(key);
		if (obj == null)
			return defaultValue;
		try {
			return ((Integer) obj).intValue();
		} catch (ClassCastException e) {
			log(key, obj, "Integer", Integer.valueOf(defaultValue), e);
		}
		return defaultValue;
	}

	public long getLong(String key) {
		return getLong(key, 0L);
	}

	public long getLong(String key, long defaultValue) {
		Object localObject = this.map.get(key);
		if (localObject == null)
			return defaultValue;
		try {
			return ((Long) localObject).longValue();
		} catch (ClassCastException e) {
			log(key, localObject, "Long", Long.valueOf(defaultValue), e);
		}
		return defaultValue;
	}

	public float getFloat(String key) {
		return getFloat(key, 0.0F);
	}

	public float getFloat(String key, float defaultValue) {
		Object localObject = this.map.get(key);
		if (localObject == null)
			return defaultValue;
		try {
			return ((Float) localObject).floatValue();
		} catch (ClassCastException e) {
			log(key, localObject, "Float", Float.valueOf(defaultValue), e);
		}
		return defaultValue;
	}

	public double getDouble(String key) {
		return getDouble(key, 0.0D);
	}

	public double getDouble(String key, double defaultValue) {
		Object localObject = this.map.get(key);
		if (localObject == null)
			return defaultValue;
		try {
			return ((Double) localObject).doubleValue();
		} catch (ClassCastException e) {
			log(key, localObject, "Double", Double.valueOf(defaultValue), e);
		}
		return defaultValue;
	}

	public String getString(String key, String defaultValue) {
		Object localObject = this.map.get(key);
		if (localObject == null)
			return defaultValue;
		try {
			return (String) localObject;
		} catch (ClassCastException e) {
			log(key, localObject, "String", defaultValue, e);
		}
		return defaultValue;
	}

	public String getString(String key) {
		return getString(key, null);
	}

	public Asset getAsset(String key) {
		Object localObject = this.map.get(key);
		if (localObject == null)
			return null;
		try {
			return (Asset) localObject;
		} catch (ClassCastException e) {
			log(key, localObject, "Asset", e);
		}
		return null;
	}

	public DataMap getDataMap(String key) {
		Object obj = this.map.get(key);
		if (obj == null)
			return null;
		try {
			return (DataMap) obj;
		} catch (ClassCastException e) {
			log(key, obj, "DataMap", e);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public ArrayList<Integer> getIntegerArrayList(String key) {
		Object obj = this.map.get(key);
		if (obj == null)
			return null;
		try {
			return (ArrayList<Integer>) obj;
		} catch (ClassCastException e) {
			log(key, obj, "ArrayList<Integer>", e);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public ArrayList<String> getStringArrayList(String key) {
		Object obj = this.map.get(key);
		if (obj == null)
			return null;
		try {
			return (ArrayList<String>) obj;
		} catch (ClassCastException e) {
			log(key, obj, "ArrayList<String>", e);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public ArrayList<DataMap> getDataMapArrayList(String key) {
		Object obj = this.map.get(key);
		if (obj == null)
			return null;
		try {
			return (ArrayList<DataMap>) obj;
		} catch (ClassCastException e) {
			log(key, obj, "ArrayList<DataMap>", e);
		}
		return null;
	}

	public byte[] getByteArray(String key) {
		Object obj = this.map.get(key);
		if (obj == null)
			return null;
		try {
			return (byte[]) (byte[]) obj;
		} catch (ClassCastException e) {
			log(key, obj, "byte[]", e);
		}
		return null;
	}

	public long[] getLongArray(String key) {
		Object obj = this.map.get(key);
		if (obj == null)
			return null;
		try {
			return (long[]) (long[]) obj;
		} catch (ClassCastException e) {
			log(key, obj, "long[]", e);
		}
		return null;
	}

	public float[] getFloatArray(String key) {
		Object obj = this.map.get(key);
		if (obj == null)
			return null;
		try {
			return (float[]) (float[]) obj;
		} catch (ClassCastException e) {
			log(key, obj, "float[]", e);
		}
		return null;
	}

	public String[] getStringArray(String key) {
		Object obj = this.map.get(key);
		if (obj == null)
			return null;
		try {
			return (String[]) (String[]) obj;
		} catch (ClassCastException e) {
			log(key, obj, "String[]", e);
		}
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof DataMap))
			return false;
		return equals(this, (DataMap) obj);
	}

	public int hashCode() {
		return 29 * this.map.hashCode();
	}

	public String toString() {
		return this.map.toString();
	}

	private void log(String key, Object obj, String value, ClassCastException e) {
		log(key, obj, value, "[null]", e);
	}

	private static boolean equals(DataMap dataMap1, DataMap dataMap2) {
		if (dataMap1.size() != dataMap2.size())
			return false;
		Iterator<String> iterator = dataMap1.keySet().iterator();
		while (iterator.hasNext()) {
			String str = (String) iterator.next();
			Object obj1 = dataMap1.get(str);
			Object obj2 = dataMap2.get(str);
			if ((obj1 instanceof Asset)) {
				if (!(obj2 instanceof Asset))
					return false;
				if (!equals((Asset) obj1, (Asset) obj2))
					return false;
			} else {
				if ((obj1 instanceof Bundle)) {
					if (!(obj2 instanceof Bundle))
						return false;
					return ((Bundle) obj1).equals(obj2);
				}
				if ((obj1 instanceof String[])) {
					if (!(obj2 instanceof String[]))
						return false;
					if (!Arrays.equals((String[]) (String[]) obj1, (String[]) (String[]) obj2))
						return false;
				} else if ((obj1 instanceof long[])) {
					if (!(obj2 instanceof long[]))
						return false;
					if (!Arrays.equals((long[]) (long[]) obj1, (long[]) (long[]) obj2))
						return false;
				} else if ((obj1 instanceof float[])) {
					if (!(obj2 instanceof float[]))
						return false;
					if (!Arrays.equals((float[]) (float[]) obj1, (float[]) (float[]) obj2))
						return false;
				} else if ((obj1 instanceof byte[])) {
					if (!(obj2 instanceof byte[]))
						return false;
					if (!Arrays.equals((byte[]) (byte[]) obj1, (byte[]) (byte[]) obj2))
						return false;
				} else {
					if ((obj1 == null) || (obj2 == null))
						return obj1 == obj2;
					if (!obj1.equals(obj2))
						return false;
				}
			}
		}
		return true;
	}

	private void log(String key, Object obj, String value, Object defaultObj, ClassCastException e) {
		StringBuffer sb = new StringBuffer();
		sb.append("Expected key ");
		sb.append(key);
		sb.append(" with value ");
		sb.append(value);
		sb.append(" but got the actual value ");
		sb.append(obj.getClass().getName());
		sb.append(".  So return the default ");
		sb.append(defaultObj);
		sb.append(" .");
		Log.w("DataMap", sb.toString());
		Log.w("DataMap", "Internal exception: ", e);
	}

	private static boolean equals(Asset asset1, Asset asset2) {
		if ((asset1 == null) || (asset2 == null))
			return asset1 == asset2;
		if (!TextUtils.isEmpty(asset1.getDigest()))
			return asset1.getDigest().equals(asset2.getDigest());
		return Arrays.equals(asset1.getData(), asset2.getData());
	}
}