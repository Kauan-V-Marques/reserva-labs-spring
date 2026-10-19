package br.dev.kauan.reservalabs.reservation;

import br.dev.kauan.reservalabs.common.Exceptions.BusinessRuleException;
import br.dev.kauan.reservalabs.lab.Laboratory;
import java.time.Duration;
import java.time.LocalDateTime;

/** Regras para aceitar um pedido de reserva (sem acesso a banco, fáceis de testar). */
public final class ReservationRules {

    static final Duration MIN_DURATION = Duration.ofMinutes(30);
    static final Duration MAX_DURATION = Duration.ofHours(4);
    static final int SLOT_MINUTES = 15;
    static final int MAX_DAYS_AHEAD = 60;

    private ReservationRules() {
    }

    public static void validate(Laboratory lab, LocalDateTime startsAt, LocalDateTime endsAt, int attendees,
                                LocalDateTime now) {
        if (!lab.isActive()) {
            throw new BusinessRuleException("Laboratory %s is not accepting reservations.".formatted(lab.getCode()));
        }
        if (!endsAt.isAfter(startsAt)) {
            throw new BusinessRuleException("The reservation must end after it starts.");
        }
        if (!startsAt.toLocalDate().equals(endsAt.toLocalDate())) {
            throw new BusinessRuleException("A reservation must start and end on the same day.");
        }
        if (!onSlotBoundary(startsAt) || !onSlotBoundary(endsAt)) {
            throw new BusinessRuleException("Times must be on %d-minute slots (e.g. 14:00, 14:15).".formatted(SLOT_MINUTES));
        }

        Duration duration = Duration.between(startsAt, endsAt);
        if (duration.compareTo(MIN_DURATION) < 0 || duration.compareTo(MAX_DURATION) > 0) {
            throw new BusinessRuleException("A reservation must last between 30 minutes and 4 hours.");
        }
        if (startsAt.toLocalTime().isBefore(lab.getOpensAt()) || endsAt.toLocalTime().isAfter(lab.getClosesAt())) {
            throw new BusinessRuleException("Laboratory %s is open from %s to %s."
                    .formatted(lab.getCode(), lab.getOpensAt(), lab.getClosesAt()));
        }
        if (!startsAt.isAfter(now)) {
            throw new BusinessRuleException("Reservations cannot start in the past.");
        }
        if (startsAt.isAfter(now.plusDays(MAX_DAYS_AHEAD))) {
            throw new BusinessRuleException("Reservations can be made at most %d days ahead.".formatted(MAX_DAYS_AHEAD));
        }
        if (attendees > lab.getCapacity()) {
            throw new BusinessRuleException("Laboratory %s holds at most %d people."
                    .formatted(lab.getCode(), lab.getCapacity()));
        }
    }

    private static boolean onSlotBoundary(LocalDateTime time) {
        return time.getMinute() % SLOT_MINUTES == 0 && time.getSecond() == 0 && time.getNano() == 0;
    }
}
