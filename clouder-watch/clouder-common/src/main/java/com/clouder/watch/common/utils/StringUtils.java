package com.clouder.watch.common.utils;

/**
 * Created by yang_shoulai on 8/17/2015.
 */
public class StringUtils {

    /**
     * 判断一个字符串是否为空
     *
     * @param val
     * @return true 当字符串为null或者字符串的长度为0；false,别的情况
     */
    public static boolean isEmpty(String val) {
        return val == null || val.trim().length() == 0;
    }
}
