package com.clouder.watch.common.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by yang_shoulai on 7/3/2015.
 */
public class DateUtils {

    private static final String PATTEN = "yyyy/MM/dd HH:mm E";

    public static String[] resolve(Date date) {
        return new SimpleDateFormat(PATTEN).format(date).split(" ");
    }

    public static void main(String[] args) {
        Date d = new Date();
        String[] array = resolve(d);
        System.out.println(array[0] + "\\" + array[1] + "\\" + array[2]);
    }
}
