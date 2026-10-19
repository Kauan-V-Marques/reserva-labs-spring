package br.dev.kauan.reservalabs.reservation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.dev.kauan.reservalabs.common.Exceptions.BusinessRuleException;
import br.dev.kauan.reservalabs.lab.Laboratory;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ReservationRulesTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 16, 8, 0);
    private final Laboratory lab = new Laboratory("ELE-01", "Eletronica", "Bloco A", 24,
            LocalTime.of(7, 30), LocalTime.of(22, 30));

    private static LocalDateTime at(String dateTime) {
        return LocalDateTime.parse(dateTime);
    }

    @Test
    void acceptsAValidReservation() {
        assertThatCode(() -> ReservationRules.validate(lab, at("2026-11-17T14:00"), at("2026-11-17T16:00"), 20, NOW))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0} -> {1}: {2}")
    @CsvSource({
            "2026-11-17T16:00, 2026-11-17T14:00, end after it starts",
            "2026-11-17T21:00, 2026-11-18T01:00, same day",
            "2026-11-17T14:10, 2026-11-17T15:00, 15-minute slots",
            "2026-11-17T14:00, 2026-11-17T14:15, between 30 minutes and 4 hours",
            "2026-11-17T08:00, 2026-11-17T13:00, between 30 minutes and 4 hours",
            "2026-11-17T07:00, 2026-11-17T08:00, open from 07:30 to 22:30",
            "2026-11-17T22:00, 2026-11-17T23:00, open from 07:30 to 22:30",
            "2026-11-16T07:30, 2026-11-16T08:30, cannot start in the past",
            "2027-01-20T14:00, 2027-01-20T15:00, at most 60 days ahead",
    })
    void rejectsInvalidPeriods(String start, String end, String expectedMessage) {
        assertThatThrownBy(() -> ReservationRules.validate(lab, at(start), at(end), 10, NOW))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(expectedMessage);
    }

    @Test
    void rejectsMoreAttendeesThanCapacity() {
        assertThatThrownBy(() -> ReservationRules.validate(lab, at("2026-11-17T14:00"), at("2026-11-17T15:00"), 25, NOW))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("at most 24 people");
    }

    @Test
    void rejectsInactiveLaboratory() {
        lab.update(lab.getName(), lab.getBuilding(), lab.getCapacity(), lab.getOpensAt(), lab.getClosesAt(), false);

        assertThatThrownBy(() -> ReservationRules.validate(lab, at("2026-11-17T14:00"), at("2026-11-17T15:00"), 5, NOW))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not accepting reservations");
    }
}
