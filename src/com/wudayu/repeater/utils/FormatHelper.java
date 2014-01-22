package com.wudayu.repeater.utils;


/**
 *
 * @author: Wu Dayu
 * @En_Name: David Wu
 * @E-mail: wudayu@gmail.com
 * @version: 1.0
 * @Created Time: Jan 22, 2014 5:21:53 PM
 * @Description: This is David Wu's property.
 *
 **/
public class FormatHelper {

	public static String timeFormatter(int val) {
		int hour, min, sec;
		val /= 1000;
		sec = val % 60;
		val /= 60;
		min = val % 60;
		val /= 60;
		hour = val % 60;

		return makeFormatFull(hour) + ":" + makeFormatFull(min) + ":" + makeFormatFull(sec);
	}

	private static String makeFormatFull(int num) {
		if (num < 10)
			return "0" + String.valueOf(num);
		else
			return String.valueOf(num);
	}

}
