package br.dev.kauan.reservalabs.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AvailabilityCalculatorTest {

    private static final LocalTime OPENS = LocalTime.of(8, 0);
    private static final LocalTime CLOSES = LocalTime.of(18, 0);

    private static TimeRange range(String start, String end) {
        return new TimeRange(LocalTime.parse(start), LocalTime.parse(end));
    }

    @Test
    void wholeDayIsFreeWithoutReservations() {
        assertThat(AvailabilityCalculator.freeSlots(OPENS, CLOSES, List.of()))
                .containsExactly(range("08:00", "18:00"));
    }

    @Test
    void reservationsSplitTheDay() {
        var busy = List.of(range("14:00", "16:00"), range("09:00", "10:30"));

        assertThat(AvailabilityCalculator.freeSlots(OPENS, CLOSES, busy)).containsExactly(
                range("08:00", "09:00"),
                range("10:30", "14:00"),
                range("16:00", "18:00"));
    }

    @Test
    void backToBackReservationsLeaveNoGap() {
        var busy = List.of(range("08:00", "10:00"), range("10:00", "12:00"));

        assertThat(AvailabilityCalculator.freeSlots(OPENS, CLOSES, busy))
                .containsExactly(range("12:00", "18:00"));
    }

    @Test
    void fullyBookedDayHasNoFreeSlots() {
        assertThat(AvailabilityCalculator.freeSlots(OPENS, CLOSES, List.of(range("08:00", "18:00")))).isEmpty();
    }

    @Test
    void overlappingBusyRangesAreHandled() {
        var busy = List.of(range("09:00", "12:00"), range("10:00", "11:00"), range("11:30", "13:00"));

        assertThat(AvailabilityCalculator.freeSlots(OPENS, CLOSES, busy)).containsExactly(
                range("08:00", "09:00"),
                range("13:00", "18:00"));
    }
}
