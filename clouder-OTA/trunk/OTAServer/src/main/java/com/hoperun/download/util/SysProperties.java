/*
 * jp.co.arksystems.framework.utils.SysProperties.java
 * 
 * Created on 2012-02-25 12:36:14
 *
 * Version 1.0.0
 * 
 * Copyright 2012 by Arksystems Co., Ltd 
 *                        
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of Arksystems Co., Ltd. You shall not disclose such Confidential Information and shall use it only in accordance with the terms of the license agreement you entered into with Arksystems Co., Ltd.; 
 */
package com.hoperun.download.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * 
 * @author hu_wg
 * 
 */
public class SysProperties {

	private static Logger log = Logger.getLogger(SysProperties.class);
	private static boolean IS_AUTO_RELOAD = false;

	public static String BASE_URL = "/";

	/** System properties **/
	private static SysProperties SysPropertiesObject = null;

	/** Singleton local property instance **/
	private static Properties SysLocalPropObject = null;

	/** Property file default **/
	private static String defaultpropfilename = "system.properties";

	/** Other files base directory (for files specified relative) **/
	protected long lastModifiedData = -1;

	/** Create instance of the WOProperties class */
	public static void init() {
		init(defaultpropfilename);
	}

	public static void main(String[] args) {
		SysProperties.init();
		System.out.println(SysProperties.getProperty("jms.username"));
	}

	public static void init(String filename) {
		init(filename, SysProperties.class.getClassLoader());
	}

	public static void init(ClassLoader loader) {
		init(defaultpropfilename, loader);
	}

	public synchronized static void init(String filename, ClassLoader loader) {
		if (SysPropertiesObject == null) {
			SysPropertiesObject = new SysProperties(filename, loader);
		}
	}

	/** Private Constructor for Singleton Instance */
	private SysProperties(String propfilename, ClassLoader loader) {

		SysLocalPropObject = new Properties();

		final String filePath = SysProperties.class.getResource("/" + propfilename).getFile();
		final SysProperties self = this;

		File propertyFile = new File(filePath);
		reloadFile(propertyFile);

		// Loop to detect file changes
		if (IS_AUTO_RELOAD) {
			Thread t = new Thread() {
				public void run() {
					while (true) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
						}
						try {
							File propertyFile = new File(filePath);
							if (self.lastModifiedData != propertyFile.lastModified()) {
								log.info("Property file is changed to reload!");
								self.reloadFile(propertyFile);
								self.lastModifiedData = propertyFile.lastModified();
							}
						} catch (Exception e) {

						}
					}
				}
			};
			t.start();
		}
	}

	protected void reloadFile(File propertyFile) {
		FileInputStream inputStreamLocal = null;
		try {
			inputStreamLocal = new FileInputStream(propertyFile);
			if (inputStreamLocal != null) {
				SysLocalPropObject.load(inputStreamLocal);
			}
		} catch (Exception e) {
			if (e instanceof FileNotFoundException) {
				log.info("No Local Properties File Found");
				SysLocalPropObject = null;
			} else {
				e.printStackTrace();
			}
		} finally {
			try {
				if (inputStreamLocal != null)
					inputStreamLocal.close();
			} catch (IOException e) {
				log.info("Exception is happened when to close file stream");
			}
		}
	}

	/**
	 * Get a property from the Properties file (uppercase Get to avoid conflict)
	 */
	public static String getProperty(String property) {
		String val = null;

		if (SysLocalPropObject != null)
			val = SysLocalPropObject.getProperty(property);

		return (val);

	} // getProperty()

	/** Get a property, allowing for a default value specification */
	public static String getProperty(String property, String defaultValue) {
		String val = null;

		if (SysLocalPropObject != null) {
			val = SysLocalPropObject.getProperty(property, defaultValue);
		} else {
			val = defaultValue;
		}

		return (val);
	}

	/**
	 * Get a property from the Properties file (uppercase Get to avoid conflict)
	 */
	public static Integer getIntProperty(String property) {
		String val = getProperty(property);
		Integer nVal = null;
		try {
			nVal = Integer.parseInt(val);
		} catch (Exception e) {

		}
		return nVal;

	} // getProperty()

	/** Get a property, allowing for a default value specification */
	public static Integer getIntProperty(String property, Integer defaultValue) {
		Integer val = getIntProperty(property);

		if (val == null) {
			val = defaultValue;
		}

		return (val);
	}

	/**
	 * Get a property from the Properties file (uppercase Get to avoid conflict)
	 */
	public static Long getLongProperty(String property) {
		String val = getProperty(property);
		Long nVal = null;
		try {
			nVal = Long.parseLong(val);
		} catch (Exception e) {

		}
		return nVal;

	} // getProperty()

	/** Get a property, allowing for a default value specification */
	public static Long getLongProperty(String property, Long defaultValue) {
		Long val = getLongProperty(property);

		if (val == null) {
			val = defaultValue;
		}

		return (val);
	}

	public static boolean getBooleanProperty(String property, boolean defaultValue) {
		boolean retval = false;
		String val = getProperty(property);

		if (val == null || val.equals(""))
			retval = defaultValue;
		else if (val.trim().equalsIgnoreCase("true") || val.trim().equals("1"))
			retval = true;

		return (retval);
	}
	
	/**
	    * 写入properties信息
	    * @param filePath  绝对路径（包括文件名和后缀名）
	    * @param parameterName  名称
	    * @param parameterValue 值
	    */
    public static void writeSystemProperties(String parameterName, String parameterValue) {
    	String filePath = SysProperties.class.getResource("/" + defaultpropfilename).getFile();
    	Properties props = new Properties();
        try {
        
            //如果文件不存在，创建一个新的
        	File file=new File(filePath);
        	
            if (!file.exists()) {
            	file.createNewFile();
            }
 
	        InputStream fis = new FileInputStream(filePath);
	        // 从输入流中读取属性列表（键和元素对）
	        props.load(fis);
	        fis.close();
	        OutputStream fos = new FileOutputStream(filePath);
	        props.setProperty(parameterName, parameterValue);
	        // 以适合使用 load 方法加载到 Properties 表中的格式，
	        // 将此 Properties 表中的属性列表（键和元素对）写入输出流
	        props.store(fos, parameterName);
	        fos.close(); // 关闭流
	        SysPropertiesObject = new SysProperties(defaultpropfilename, SysProperties.class.getClassLoader());
        } catch (IOException e) {
        	System.err.println("Visit "+filePath+" for updating "+parameterName+" value error");
        }
   }
}
