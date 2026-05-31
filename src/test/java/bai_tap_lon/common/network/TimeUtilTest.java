package bai_tap_lon.common.network;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TimeUtilTest {

    @Test
    void toIsoAndFromIsoRoundTripVietnamTime() {
        LocalDateTime time = LocalDateTime.of(2026, 5, 31, 13, 45, 12);

        String iso = TimeUtil.toIso(time);
        LocalDateTime parsed = TimeUtil.fromIso(iso);

        assertEquals("2026-05-31T13:45:12+07:00", iso);
        assertEquals(time, parsed);
    }

    @Test
    void fromIsoAcceptsLegacyDatabaseFormats() {
        assertEquals(
                LocalDateTime.of(2026, 5, 31, 13, 45, 12),
                TimeUtil.fromIso("2026-05-31 13:45:12")
        );
        assertEquals(
                LocalDateTime.of(2026, 5, 31, 13, 45, 12),
                TimeUtil.fromIso("2026-05-31T13:45:12")
        );
    }

    @Test
    void fromIsoReturnsNullForBlankOrInvalidInput() {
        assertNull(TimeUtil.fromIso(null));
        assertNull(TimeUtil.fromIso(""));
        assertNull(TimeUtil.fromIso("not-a-date"));
    }

    @Test
    void epochSecondsRoundTripUsesConfiguredZone() {
        LocalDateTime time = LocalDateTime.of(2026, 5, 31, 13, 45, 12);

        double epoch = TimeUtil.toEpochSeconds(time);

        assertEquals(time, TimeUtil.fromEpochSeconds(epoch));
    }
}
