package com.tn.wallet.util;

import android.content.Context;
import android.text.format.DateUtils;

import com.tn.wallet.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtil {

    private Context context = null;

    public DateUtil(Context context) {
        this.context = context;
    }

    public String formatted(long ts) {
        String ret;

        Date localTime = new Date(ts/1000);
        long date = localTime.getTime();

        date *= 1000L;
        long hours24 = 60L * 60L * 24L * 1000L;
        long now = System.currentTimeMillis();

        Calendar calNow = Calendar.getInstance();
        calNow.setTime(new Date(now));

        Calendar calThen = Calendar.getInstance();
        calThen.setTime(new Date(date));
        int thenDay = calThen.get(Calendar.DAY_OF_MONTH);

        long yesterdayEnd = getDayEnd(now - hours24);
        long yesterdayStart = getDayStart(now - hours24);
        long yearStart = getYearStart(now);

        if (date > yesterdayEnd) {
            //today
            ret = (String) DateUtils.getRelativeTimeSpanString(date, now, DateUtils.SECOND_IN_MILLIS, 0);

        } else if (date <= yesterdayEnd && date >= yesterdayStart) {
            //yesterday
            ret = context.getString(R.string.YESTERDAY);

        } else if (date < yearStart) {
            //previous years
            int year = calThen.get(Calendar.YEAR);
            String month = calThen.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
            ret = month + " " + thenDay + ", " + year;

        } else {
            //this year
            String month = calThen.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
            String day = calThen.getDisplayName(Calendar.DAY_OF_MONTH, Calendar.LONG, Locale.getDefault());
            ret = month + " " + thenDay;
        }

        return ret;
    }

    public String timestampToDateTime
            (long timestamp) {
        Date date = new Date (timestamp);
        return new SimpleDateFormat("HH:mm:ss").format(date);
    }

    public String timestampToDate
            (long timestamp) {
        Date date = new Date (timestamp);
        return new SimpleDateFormat("dd.MM.yy").format(date);
    }

    public static String formattedCurrentDate(){
        Date date = new Date();
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    private long parseDateTime(String time) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(time).getTime();
        } catch (Exception e) {
        }
        return 0;
    }

    private long getDayStart(long time) {
        return parseDateTime(new SimpleDateFormat("yyyy-MM-dd").format(new Date(time)) + " 00:00:00");
    }

    private long getDayEnd(long time) {
        return parseDateTime(new SimpleDateFormat("yyyy-MM-dd").format(new Date(time)) + " 23:59:59");
    }

    private long getYearStart(long time) {
        return parseDateTime(new SimpleDateFormat("yyyy").format(new Date(time)) + "-01-01 00:00:00");
    }
}
