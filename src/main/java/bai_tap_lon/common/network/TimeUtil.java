package bai_tap_lon.common.network;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class TimeUtil {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private static final DateTimeFormatter TIME_SHORT_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static String toIso(LocalDateTime ldt) {
        if (ldt == null) return null;
        return ldt.atZone(ZONE).format(FMT);
    }

    public static LocalDateTime fromIso(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            // Thử parse chuẩn ISO có offset (2026-05-07T14:17:12+07:00)
            return ZonedDateTime.parse(iso, FMT).withZoneSameInstant(ZONE).toLocalDateTime();
        } catch (Exception e1) {
            try {
                // Fallback: DB cũ lưu "yyyy-MM-dd HH:mm:ss" hoặc "yyyy-MM-dd'T'HH:mm:ss" (không offset)
                String normalized = iso.contains("T") ? iso : iso.replace(' ', 'T');
                return LocalDateTime.parse(normalized).atZone(ZONE).toLocalDateTime();
            } catch (Exception e2) {
                return null;
            }
        }
    }

    /** Wall-clock now in VN — dùng thay {@code LocalDateTime.now()} để
     *  timestamp không phụ thuộc zone của JVM. */
    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    /** Format thành "HH:mm:ss" (giờ VN). */
    public static String formatTime(LocalDateTime ldt) {
        return ldt == null ? "--:--:--" : ldt.format(TIME_FMT);
    }
    public static String formatTimeShort(LocalDateTime ldt) {
        return ldt == null ? "--:--:--" : ldt.format(TIME_SHORT_FMT);
    }

    /** Epoch seconds tính theo zone VN — dùng cho trục thời gian tuyến tính. */
    public static double toEpochSeconds(LocalDateTime ldt) {
        return ldt.atZone(ZONE).toEpochSecond();
    }

    /** Ngược lại của {@link #toEpochSeconds(LocalDateTime)}. */
    public static LocalDateTime fromEpochSeconds(double epochSec) {
        long sec = (long) Math.floor(epochSec);
        return LocalDateTime.ofInstant(java.time.Instant.ofEpochSecond(sec), ZONE);
    }

}
