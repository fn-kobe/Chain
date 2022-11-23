package util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;

public class TimeHelper {
    static public long getEpoch() {
        return Instant.now().toEpochMilli();
    }

    static public long getEpochSeconds() {
        return getEpoch() / 1000;
    }

    static public String getCurrentTimeUsingCalendar() {
        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SS");
        return dateFormat.format(date);
    }

    static public String getCurrentDataStringByDot() {
        Date date =  new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd_hh.mm.ss");
        return format.format(date);
    }
}
