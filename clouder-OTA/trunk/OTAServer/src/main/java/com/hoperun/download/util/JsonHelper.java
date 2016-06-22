package com.hoperun.download.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonHelper {
	
	public static <T> T fromJson(String json, Class<T> classOfT) {
		return fromJson(json, classOfT, null);
	}

	public static <T> T fromJson(String json, Class<T> classOfT, Map<Class<?>, Object> adapters) {
		Gson gson = getGson(adapters);
		return gson.fromJson(json, classOfT);
	}

	public static String toJson(Object jsonElement) {
		return toJson(jsonElement, null);
	}

	public static String toJson(Object jsonElement, Map<Class<?>, Object> adapters) {
		Gson gson = getGson(adapters);
		return gson.toJson(jsonElement);
	}

	public static Gson getGson(Map<Class<?>, Object> adapters) {
		Gson gson = null;
		if (adapters != null) {
			GsonBuilder gsonBuilder = new GsonBuilder();
			for (Map.Entry<Class<?>, Object> entry : adapters.entrySet()) {
				gsonBuilder.registerTypeAdapter(entry.getKey(), entry.getValue());
			}
			gson = gsonBuilder.create();
		} else {
			gson = new Gson();
		}
		return gson;
	}
	
	public static void main(String[] args) {
		JsonHelper l = new JsonHelper();
		Gson gson = new Gson();
		B b = l.new B();
		b.setA("a");
		b.setB("b");
		List <String >c = new ArrayList<String>();
		c.add("c1");
		c.add("c2");
		b.setC(c);
		A a = b;
		System.out.println(gson.toJson(a));
	}

	interface A {
		String getA();
	}

	class B implements A {

		private String a;
		private String b;
		private List<String > c;

		/**
		 * @return the c
		 */
		public List<String> getC() {
			return c;
		}

		/**
		 * @param c the c to set
		 */
		public void setC(List<String> c) {
			this.c = c;
		}

		@Override
		public String getA() {

			return a;
		}

		/**
		 * @return the b
		 */
		public String getB() {
			return b;
		}

		/**
		 * @param b
		 *            the b to set
		 */
		public void setB(String b) {
			this.b = b;
		}

		/**
		 * @param a
		 *            the a to set
		 */
		public void setA(String a) {
			this.a = a;
		}

	}
}
