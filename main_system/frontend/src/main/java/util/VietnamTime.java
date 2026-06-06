package util;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public final class VietnamTime {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private VietnamTime() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    public static LocalTime timeNow() {
        return LocalTime.now(ZONE);
    }
}
