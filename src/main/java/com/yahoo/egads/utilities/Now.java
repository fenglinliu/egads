package com.yahoo.egads.utilities;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Now
 *
 * @author Flynn
 * @version 1.0
 * @description 时间工具类
 * @email liufenglin@163.com
 * @date 2019/1/31
 */
public class Now {
    private static Date date;


    public static String minute() {
        date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return sdf.format(date);
    }

    public static String today() {
        Calendar calendar = Calendar.getInstance();
        Date date = calendar.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        return sdf.format(date);
    }

    public static String yestoday() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        Date date = calendar.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        return sdf.format(date);
    }
//
    public static String month() {
        Calendar calendar = Calendar.getInstance();
        Date date = calendar.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
        return sdf.format(date);
    }

    public static String lastMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        Date date = calendar.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
        return sdf.format(date);
    }



    /**
     * Java将Unix时间戳(毫秒)转换成指定格式日期字符串
     * @param timestampString 时间戳 如："1473048265";
     *
     * @return 返回结果 如："2016-09-05 16:06:42";
     */
    public static String TimeStampMillisecond2Date(String timestampString, String formats) {
        Long timestamp = Long.parseLong(timestampString);
        String date = new SimpleDateFormat(formats, Locale.CHINA).format(new Date(timestamp));
        return date;
    }

    /**
     * Java将Unix时间戳(秒)转换成指定格式日期字符串
     * @param timestampString 时间戳 如："1473048265";
     *
     * @return 返回结果 如："2016-09-05 16:06:42";
     */
    public static String TimeStampSecond2Date(String timestampString, String formats) {
        Long timestamp = Long.parseLong(timestampString) * 1000;
        String date = new SimpleDateFormat(formats, Locale.CHINA).format(new Date(timestamp));
        return date;
    }
}
