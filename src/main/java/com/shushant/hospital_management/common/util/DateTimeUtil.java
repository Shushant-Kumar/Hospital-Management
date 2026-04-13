package com.shushant.hospital_management.common.util;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class DateTimeUtil {

    private DateTimeUtil() {
    }

    public static OffsetDateTime nowUtc() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
