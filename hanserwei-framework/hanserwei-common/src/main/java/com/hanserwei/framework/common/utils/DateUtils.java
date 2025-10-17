package com.hanserwei.framework.common.utils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class DateUtils {

    /**
     * LocalDateTime 转时间戳
     *
     * @param localDateTime LocalDateTime
     * @return 时间戳
     */
    public static long localDateTime2Timestamp(LocalDateTime localDateTime) {
        return localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}
