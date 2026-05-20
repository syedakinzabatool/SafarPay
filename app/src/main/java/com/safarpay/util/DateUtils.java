package com.safarpay.util;
import java.text.SimpleDateFormat; import java.util.*;
public class DateUtils {
    public static String today() {
        return new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(new Date());
    }
    public static long daysUntil(String dateStr) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).parse(dateStr);
            return (d.getTime() - System.currentTimeMillis()) / 86400000L;
        } catch (Exception e) { return 0; }
    }
}