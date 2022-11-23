package util;

import java.text.SimpleDateFormat;

public class DateHelper {
    static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static SimpleDateFormat getSimpleDateFormat(){
        return simpleDateFormat;
    }
}
