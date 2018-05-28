package com.gec.mmall.util;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Date;

//基于joda-time
public class DateTimeUtil {


	public static final String STANDARD_FORMAT = "YYYY-mm-dd HH:mm:ss";

	/**
	 * 字符串转时间
	 * @param dateTimeStr
	 * @param formatStr
	 * @return
	 */
	public static Date strToDate(String dateTimeStr,String formatStr){
		DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(formatStr);
		DateTime dateTime = dateTimeFormatter.parseDateTime(dateTimeStr);
		return dateTime.toDate();
	}

	/**
	 * 时间转字符串
	 * @param date
	 * @param formatStr
	 * @return
	 */
	public static String dateToStr(Date date,String formatStr){
		if (date == null){
			return StringUtils.EMPTY;
		}
		DateTime dateTime = new DateTime(date);
		return dateTime.toString(formatStr);
	}

	public static Date strToDate(String dateTimeStr){
		DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(STANDARD_FORMAT);
		DateTime dateTime = dateTimeFormatter.parseDateTime(dateTimeStr);
		return dateTime.toDate();
	}



	public static String dateToStr(Date date){
		if (date == null){
			return StringUtils.EMPTY;
		}
		DateTime dateTime = new DateTime(date);
		return dateTime.toString(STANDARD_FORMAT);
	}

	public static void main(String[] args) {
		Date date = new Date();
		System.out.println(DateTimeUtil.dateToStr(date));
		System.out.println(DateTimeUtil.dateToStr(date,"YYYY-mm-dd HH:mm:ss"));
		System.out.println(DateTimeUtil.strToDate("2012-12-01 13:00:00"));
		System.out.println(DateTimeUtil.strToDate("2012-12-01 13:00:00","YYYY-mm-dd HH:mm:ss"));
	}
}
