/**
 * Copyright (c) 2013 Hoperun.
 * All rights reserved
 */

package com.hoperun.watch.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.util.Base64;
import android.util.Log;

/**
 * @version 1.0
 * @createDate 2013-8-7
 */
public final class SharePreUtils {

	// read & write authority
	private final static int MODE = Context.MODE_PRIVATE;

	private SharePreUtils() {
	}

	/**
	 * Save data to specific name of shared preferences
	 * 
	 * @param context
	 *            context to get sharedPreferences
	 * @param preferenceName
	 *            name of sharedpreferences
	 * @param data
	 *            data to be saved
	 * @return whether saved successfully ,return true or false
	 */
	public static boolean saveData(Context context, String preferenceName, Map<String, Object> data) {

		boolean bCommitted = false;

		Editor oEditor = null;

		while (true) {

			if (context == null || data == null || data.isEmpty() || preferenceName == null) {
				break; // while (true)
			}

			oEditor = context.getSharedPreferences(preferenceName, MODE).edit();

			Iterator<Entry<String, Object>> keys = data.entrySet().iterator();

			Entry<String, Object> key = null;

			while (keys.hasNext()) {

				key = keys.next();

				Object valueObject = data.get(key.getKey());

				SimpleObjectTypeEnum objType = validateObjectType(valueObject);

				switch (objType) {

				case Integer:

					oEditor.putInt(key.getKey(), (Integer) data.get(key.getKey()));

					break;

				case Long:

					oEditor.putLong(key.getKey(), (Long) data.get(key.getKey()));

					break;

				case Float:

					oEditor.putFloat(key.getKey(), (Float) data.get(key.getKey()));

					break;

				case Boolean:

					oEditor.putBoolean(key.getKey(), (Boolean) data.get(key.getKey()));

					break;

				case String:

					oEditor.putString(key.getKey(), (String) data.get(key.getKey()));

					break;

				case Object:

					oEditor.putString(key.getKey(), saveObjectToString((Object) data.get(key.getKey())));

					break;

				default:

					break;

				} // switch (objType)

			} // while (keys.hasNext())

			bCommitted = oEditor.commit();

			break; // while (true)

		} // while (true)

		return (bCommitted);
	}

	/**
	 * judge an object type
	 * 
	 * @param obj
	 *            object to be judged
	 * @return an object of <class>SimpleObjectTypeEnum</class> class
	 */
	private static SimpleObjectTypeEnum validateObjectType(Object obj) {

		SimpleObjectTypeEnum flag = SimpleObjectTypeEnum.UNKNOWN;

		if (obj instanceof Integer) {

			flag = SimpleObjectTypeEnum.Integer;

		} else if (obj instanceof Long) {

			flag = SimpleObjectTypeEnum.Long;

		} else if (obj instanceof Float) {

			flag = SimpleObjectTypeEnum.Float;

		} else if (obj instanceof Boolean) {

			flag = SimpleObjectTypeEnum.Boolean;

		} else if (obj instanceof String) {

			flag = SimpleObjectTypeEnum.String;

		} else if (obj instanceof Object) {

			flag = SimpleObjectTypeEnum.Object;
		}

		return (flag);
	}

	/**
	 * Save specific key & value to given named shared preferences
	 * 
	 * @param context
	 *            context to get sharedPreferences
	 * @param preferenceName
	 *            preferenceName name of sharedpreferences
	 * @param key
	 *            specific key
	 * @param value
	 *            specific value
	 * @return whether saved successfully ,return true or false
	 */
	public static boolean saveData(Context context, String preferenceName, String key, Object value) {

		boolean bCommitted = false;

		Editor oEditor = null;

		while (true) {

			if (context == null || preferenceName == null || key == null) {
				break; // while (true)
			}

			SimpleObjectTypeEnum objType = validateObjectType(value);

			oEditor = context.getSharedPreferences(preferenceName, MODE).edit();

			switch (objType) {

			case Integer:

				oEditor.putInt(key, (Integer) value);

				break;

			case Long:

				oEditor.putLong(key, (Long) value);

				break;

			case Float:

				oEditor.putFloat(key, (Float) value);

				break;

			case Boolean:

				oEditor.putBoolean(key, (Boolean) value);

				break;

			case String:

				oEditor.putString(key, (String) value);

				break;

			case Object:

				oEditor.putString(key, saveObjectToString(value));

				break;

			default:

				oEditor.putString(key, value.toString());

				break;

			} // switch (objType)

			bCommitted = oEditor.commit();

			break; // while (true)

		} // while (true)

		return (bCommitted);
	}

	/**
	 * get string value by given key from named shared preferences
	 * 
	 * @param context
	 * @param preferenceName
	 * @param key
	 *            given key
	 * @param defValue
	 *            default value
	 * @return value found
	 */
	public static String getDataString(Context context, String preferenceName, String key, String defValue) {

		String zRetVal = defValue;

		if (context != null && preferenceName != null) {

			zRetVal = context.getSharedPreferences(preferenceName, MODE).getString(key, defValue);
		}

		return (zRetVal);
	}

	/**
	 * get Object value by given key from named shared preferences
	 * 
	 * @param context
	 * @param preferenceName
	 * @param key
	 *            given key
	 * @param defValue
	 *            default value
	 * @return value found
	 */
	public static Object getDataObject(Context context, String preferenceName, String key, Object defValue) {

		Object oRetVal = defValue;

		if (context != null && preferenceName != null) {

			oRetVal = getObjectFromString(context.getSharedPreferences(preferenceName, MODE).getString(key, ""));
		}

		return (oRetVal);
	}

	/**
	 * get boolean value by given key from named shared preferences
	 * 
	 * @param context
	 * @param preferenceName
	 * @param key
	 *            key
	 * @param defValue
	 *            default value
	 * @return value found
	 */
	public static boolean getDataBoolean(Context context, String preferenceName, String key, boolean defValue) {

		boolean bRetVal = defValue;

		if (context != null && preferenceName != null) {

			bRetVal = context.getSharedPreferences(preferenceName, MODE).getBoolean(key, defValue);
		}

		return (bRetVal);
	}

	/**
	 * get long value by given key from named shared preferences
	 * 
	 * @param context
	 * @param preferenceName
	 * @param key
	 *            key
	 * @param defValue
	 *            default value
	 * @return value found
	 */
	public static long getDataLong(Context context, String preferenceName, String key, long defValue) {

		long lRetVal = defValue;

		if (context != null && preferenceName != null) {

			lRetVal = context.getSharedPreferences(preferenceName, MODE).getLong(key, defValue);
		}

		return (lRetVal);
	}

	/**
	 * get long value by given key from named shared preferences
	 * 
	 * @param context
	 * @param preferenceName
	 * @param key
	 *            key
	 * @param defValue
	 *            default value
	 * @return value found
	 */
	public static float getFloat(Context context, String preferenceName, String key, float defValue) {

		float lRetVal = defValue;

		if (context != null && preferenceName != null) {

			lRetVal = context.getSharedPreferences(preferenceName, MODE).getFloat(key, defValue);
		}

		return (lRetVal);
	}

	/**
	 * get int value by given key from named shared preferences
	 * 
	 * @param context
	 * @param preferenceName
	 * @param key
	 *            key
	 * @param defValue
	 *            default value
	 * @return value found
	 */
	public static int getDataInt(Context context, String preferenceName, String key, int defValue) {

		int nRetVal = defValue;

		if (context != null && preferenceName != null) {

			nRetVal = context.getSharedPreferences(preferenceName, MODE).getInt(key, defValue);
		}

		return (nRetVal);
	}

	/**
	 * get all keys & values from given named sharedPreferences
	 * 
	 * @param context
	 * @param preferenceName
	 * @return map or null
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> getDataMap(Context context, String preferenceName) {

		Map<String, Object> mapRetVal = null;

		if (context != null && preferenceName != null) {

			mapRetVal = (Map<String, Object>) context.getSharedPreferences(preferenceName, MODE).getAll();
		}

		return (mapRetVal);
	}

	/**
	 * delData:del data from assigned preference by preferenceName
	 */
	public static boolean delData(Context context, String preferenceName, String key) {

		boolean bCommitted = false;

		Editor oEditor = null;

		if (context != null && preferenceName != null) {

			oEditor = context.getSharedPreferences(preferenceName, MODE).edit().remove(key);
		}

		if (oEditor != null) {

			bCommitted = oEditor.commit();
		}

		return (bCommitted);
	}

	/**
	 * inner class describing object type
	 * 
	 * @version 1.0
	 * @createDate 2013-8-7
	 */
	private enum SimpleObjectTypeEnum {
		Object, Integer, Long, Float, Boolean, String, UNKNOWN
	}

	private static String saveObjectToString(Object object) {

		String zPersonBase64 = null;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		ObjectOutputStream oos;

		try {

			oos = new ObjectOutputStream(baos);

			oos.writeObject(object);

			zPersonBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
			baos.close();
			oos.close();
		} catch (IOException exIO) {

			exIO.printStackTrace();
			Log.e("SharePreferencesHelper", Log.getStackTraceString(new Throwable()));
		}

		return (zPersonBase64);
	}

	private static Object getObjectFromString(String objString) {

		Object oRetVal = null;

		while (true) {

			if ("".equalsIgnoreCase(objString)) {
				break; // while (true)
			}

			byte[] base64Bytes = Base64.decode(objString.getBytes(), Base64.DEFAULT);

			ByteArrayInputStream bais = new ByteArrayInputStream(base64Bytes);

			ObjectInputStream ois = null;

			try {

				ois = new ObjectInputStream(bais);

				oRetVal = ois.readObject();

				bais.close();
				ois.close();

			} catch (StreamCorruptedException exSC) {
				Log.e("SharePreferenceHelper", Log.getStackTraceString(new Throwable()));
				exSC.printStackTrace();
			} catch (IOException exIO) {
				Log.e("SharePreferenceHelper", Log.getStackTraceString(new Throwable()));
				exIO.printStackTrace();
			} catch (ClassNotFoundException exCNF) {
				Log.e("SharePreferenceHelper", Log.getStackTraceString(new Throwable()));
				exCNF.printStackTrace();
			}

			break; // while (true)

		} // while (true)

		return (oRetVal);
	}
}
