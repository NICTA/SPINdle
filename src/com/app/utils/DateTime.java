/**
 * SPINdle (version 2.2.0)
 * Copyright (C) 2009-2012 NICTA Ltd.
 *
 * This file is part of SPINdle project.
 * 
 * SPINdle is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * SPINdle is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with SPINdle.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @author H.-P. Lam (oleklam@gmail.com), National ICT Australia - Queensland Research Laboratory 
 */
package com.app.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.app.exception.IncorrectNoOfArgumentsException;

public class DateTime {
	private static SimpleDateFormat FILE_TIMESTAMP_FORMATTER = new SimpleDateFormat("_yyyyMMdd_HHmmss");
	private static final int[] DAYS_IN_MONTH = { 0, 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

	public static String getCurrentTimeAsFileTimestamp() {
		Date currentTime = Calendar.getInstance().getTime();
		return FILE_TIMESTAMP_FORMATTER.format(currentTime);
	}

	public static Calendar getCurrentDateTime() {
		return Calendar.getInstance();
	}

	public static Calendar getDate(int year, int month, int dayOfMonth) {
		return getDate(year, month, dayOfMonth, 0, 0, 0);
	}

	public static Calendar getDate(int year, int month, int dayOfMonth, int hourOfDay, int minute) {
		return getDate(year, month, dayOfMonth, hourOfDay, minute, 0);
	}

	public static Calendar getDate(int year, int month, int dayOfMonth, int hourOfDay, int minute, int second) {
		return new GregorianCalendar(year, month, dayOfMonth, hourOfDay, minute, second);
	}

	public static Calendar getToday() {
		Calendar cal = new GregorianCalendar();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		return cal;
	}

	/**
	 * Verify the input time values are validate.
	 * 
	 * @param args an array of input string representing the hour-min-sec of a particular time
	 * @return validated date time values
	 * @throws Exception Throws when the inputed value is not valid
	 * @see #verifyDateArguments(String[])
	 * @see #verifyDateTimeArguments(String[])
	 */
	public static String[] verifyTimeArguments(String[] args) throws Exception {
		if (null == args || args.length != 3) throw new IncorrectNoOfArgumentsException(3);

		String[] t = new String[3];
		int hour = Integer.parseInt(args[0]);
		int min = Integer.parseInt(args[1]);
		int ms = Integer.parseInt(args[2]);

		if (hour < 0 || hour > 23) throw new IllegalArgumentException("hour exceed range, [" + hour + "]");
		if (min < 0 || min > 60) throw new IllegalArgumentException("minute exceed range, [" + min + "]");
		if (ms < 0 || ms > 1000) throw new IllegalArgumentException("mini-second exceed range, [" + ms + "]");
		t[0] = "" + hour;
		t[1] = "" + min;
		t[2] = "" + ms;
		return t;
	}

	/**
	 * Verify the input date values are validate.
	 * 
	 * @param args an array of input string representing the year-month-day of a particular time
	 * @return validated date time values
	 * @throws Exception Throws when the inputed value is not valid
	 * @see #verifyTimeArguments(String[])
	 * @see #verifyDateTimeArguments(String[])
	 */
	public static String[] verifyDateArguments(String[] args) throws Exception {
		if (null == args || args.length != 3) throw new IncorrectNoOfArgumentsException(3);
		int year = Integer.parseInt(args[0]);
		if (year < 0) throw new IllegalArgumentException("month value [" + year + "] exceed range");
		int month = Integer.parseInt(args[1]);
		if (month < 1 || month > 12) throw new IllegalArgumentException("month value [" + month + "] exceed range");
		int days = Integer.parseInt(args[2]);
		if (days < 1 || days > DAYS_IN_MONTH[month]) throw new IllegalArgumentException("days in month value [" + days + "] exceed range");

		String[] d = new String[6];
		d[0] = "" + year;
		d[1] = "" + month;
		d[2] = "" + days;
		return d;
	}

	/**
	 * Verify the input date time values are validate.
	 * 
	 * @param args an array of input string representing the year-month-day-hour-min-sec of a particular time
	 * @return validated date time values
	 * @throws Exception Throws when the inputed value is not valid
	 * @see #verifyDateArguments(String[])
	 * @see #verifyTimeArguments(String[])
	 */
	public static String[] verifyDateTimeArguments(String[] args) throws Exception {
		if (null == args) throw new IncorrectNoOfArgumentsException(3);
		if (!(args.length == 3 || args.length == 6)) throw new IncorrectNoOfArgumentsException("3 or 6");

		String d[] = verifyDateArguments(new String[] { args[0], args[1], args[2] });
		String t[] = (args.length == 6) ? verifyTimeArguments(new String[] { args[3], args[4], args[5] }) : new String[] { "0", "0", "0" };
		String[] dt = new String[6];
		for (int i = 0; i < 3; i++) {
			dt[i] = d[i];
			dt[i + 3] = t[i];
		}
		return dt;
	}
}
