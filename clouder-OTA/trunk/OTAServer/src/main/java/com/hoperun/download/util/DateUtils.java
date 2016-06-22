/*****************************************************************************
 *
 *                      HOPERUN PROPRIETARY INFORMATION
 *
 *          The information contained herein is proprietary to HopeRun
 *           and shall not be reproduced or disclosed in whole or in part
 *                    or used for any design or manufacture
 *              without direct written authorization from HopeRun.
 *
 *            Copyright (coffee) 2013 by HopeRun.  All rights reserved.
 *
 *****************************************************************************/

package com.hoperun.download.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * ClassName:DateUtils
 * 
 * @description
 * @author he_chen
 * @Date 2013-3-13
 */
public class DateUtils {

	private static final Logger	log			= Logger.getLogger(DateUtils.class);

    public static final String dateFormat = "yyyy-MM-dd HH:mm:ss";
    
    public static final String formatUS = "MMM dd, yyyy";
    
    public static final String formatCN = "yyyy-MM-dd";

    public enum DateUnits {
        day, hour, minute, second
    };

    public static long computeDate(Date startDate, Date endDate, DateUnits dateUnits) {
        long countDate = 0;
        try {

            SimpleDateFormat df = new SimpleDateFormat(dateFormat);
            String startDateStr = df.format(startDate);
            String endDateStr = df.format(endDate);

            java.util.Date startDateFormat = df.parse(startDateStr);
            java.util.Date endDateFormat = df.parse(endDateStr);

            long l = endDateFormat.getTime() - startDateFormat.getTime();

            long day = l / (24 * 60 * 60 * 1000);
            long hour = (l / (60 * 60 * 1000) - day * 24);
            long min = ((l / (60 * 1000)) - day * 24 * 60 - hour * 60);
            long secd = (l / 1000 - day * 24 * 60 * 60 - hour * 60 * 60 - min * 60);

            switch (dateUnits) {
                case day:
                    countDate = day;
                    break;
                case hour:
                    countDate = day * 24 + hour;
                    break;
                case minute:
                    countDate = day * 24 + hour * 60 + min;
                    break;
                case second:
                    countDate = day * 24 + hour * 60 + min * 60 + secd;
                    break;
                default:
                    break;
            }

        } catch (Exception e) {
			log.error("computeDate error.");
        }

        return countDate;
    }

    public static final Date getCurrentDate() {
        Date currentDate = new Date();
        SimpleDateFormat df = new SimpleDateFormat(dateFormat);
        df.format(currentDate);
        currentDate.getTime();
        return currentDate;
    }

    public static final long getCurrentLongDate() {
        return System.currentTimeMillis();
    }

    public static final Date getCurrentDate(String pattern) {
        Date currentDate = new Date();
        SimpleDateFormat df = new SimpleDateFormat(pattern);
        df.format(currentDate);
        currentDate.getTime();
        return currentDate;
    }

	public static List<Long> getFirstday_Lastday_Month() throws ParseException {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		long currentTime = System.currentTimeMillis();
		Date date = new Date(currentTime);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.MONTH, -1);
		Date theDate = calendar.getTime();

		// 上个月第一天
		GregorianCalendar gcLast = (GregorianCalendar) Calendar.getInstance();
		gcLast.setTime(theDate);
		gcLast.set(Calendar.DAY_OF_MONTH, 1);
		String day_first = df.format(gcLast.getTime());
		StringBuffer str = new StringBuffer().append(day_first).append(" 00:00:00");
		day_first = str.toString();
		long dayFirst = df2.parse(day_first).getTime();

		// 上个月最后一天
		calendar.add(Calendar.MONTH, 1); // 加一个月
		calendar.set(Calendar.DATE, 1); // 设置为该月第一天
		calendar.add(Calendar.DATE, -1); // 再减一天即为上个月最后一天
		String day_last = df.format(calendar.getTime());
		StringBuffer endStr = new StringBuffer().append(day_last).append(" 23:59:59");
		day_last = endStr.toString();
		long dayLast = df2.parse(day_last).getTime();
		List<Long> list = new ArrayList<Long>();
		list.add(0, dayFirst);
		list.add(1, dayLast);
		log.info("-------------------------------------------dayFirst:" + dayFirst);
		log.info("-------------------------------------------dayLast:" + dayLast);
		return list;
	}

	public static List<Long> getFirstday_Lastday_Week() throws ParseException {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		Calendar calendar = Calendar.getInstance();
		int w = calendar.get(Calendar.DAY_OF_WEEK);
		calendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH) - w);
		String day_last = df.format(calendar.getTime());
		StringBuffer str = new StringBuffer().append(day_last).append(" 23:59:59");
		day_last = str.toString();
		long dayLast = df2.parse(day_last).getTime();

		Calendar c2 = Calendar.getInstance();
		c2.set(Calendar.DAY_OF_MONTH, c2.get(Calendar.DAY_OF_MONTH) - w - 6);
		String day_first = df.format(c2.getTime());
		StringBuffer str2 = new StringBuffer().append(day_first).append(" 00:00:00");
		day_first = str2.toString();
		long dayFirst = df2.parse(day_first).getTime();

		List<Long> list = new ArrayList<Long>();
		list.add(0, dayFirst);
		list.add(1, dayLast);
		return list;
	}
	
    public static final Date getUSDate(String dateString) {
    	SimpleDateFormat df = new SimpleDateFormat(formatUS);
        Date date = null;
		try {
			date = df.parse(dateString);
		} catch (ParseException e) {
			e.printStackTrace();
		}
        return date;
    }
    
    public static final Date getCNDate(String dateString) {
    	SimpleDateFormat df = new SimpleDateFormat(formatCN);
        Date date = null;
		try {
			date = df.parse(dateString);
		} catch (ParseException e) {
			e.printStackTrace();
		}
        return date;
    }
    
    public static final String getCNDateString(String dateString) {
    	SimpleDateFormat df = new SimpleDateFormat(formatUS);
    	SimpleDateFormat sdf = new SimpleDateFormat(formatCN);
    	String newDateString = null;
        Date date = null;
		try {
			date = df.parse(dateString);
			newDateString = sdf.format(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
        return newDateString;
    }
}
