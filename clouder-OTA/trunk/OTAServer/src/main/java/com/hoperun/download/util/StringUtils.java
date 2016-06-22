package com.hoperun.download.util;

/**
 * 
 * @author loven
 * 
 */
public final class StringUtils {

    /**
     * 
     * @param value
     * @return
     */
    public boolean isEmpty(String value) {
        return (value == null || value.equals(""));
    }
    
    /**
     * 
     * @param value
     * @return
     */
    public boolean isEmpty(Object value) {
        return (value == null || value.toString().equals(""));
    }
    
    /**
     * 
     * @param value
     * @return
     */
    public boolean isNotEmpty(String value) {
        return !isEmpty(value);
    }
    
    /**
     * 
     * @param value
     * @return
     */
    public boolean isNotEmpty(Object value) {
        return !isEmpty(value);
    }
    
    public static String replace(String str) {
    	str = str.replaceAll("\r\n", " ");
    	str = str.replaceAll("\n", " ");
    	str = str.replaceAll("\'", "\\\\'");
    	str = str.replaceAll("\"", "\\\\'\\\\'");
    	return str;
    }
}
