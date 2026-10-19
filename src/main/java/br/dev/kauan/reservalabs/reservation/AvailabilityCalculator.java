package br.dev.kauan.reservalabs.reservation;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Calcula os horários livres de um dia: expediente menos as reservas confirmadas. */
public final class AvailabilityCalculator {

    private AvailabilityCalculator() {
    }

    public static List<TimeRange> freeSlots(LocalTime opensAt, LocalTime closesAt, List<TimeRange> busy) {
        List<TimeRange> sorted = busy.stream().sorted(Comparator.comparing(TimeRange::start)).toList();
        List<TimeRange> free = new ArrayList<>();
        LocalTime cursor = opensAt;

        for (TimeRange reservation : sorted) {
            LocalTime start = max(reservation.start(), opensAt);
            LocalTime end = min(reservation.end(), closesAt);
            if (start.isAfter(cursor)) {
                free.add(new TimeRange(cursor, start));
            }
            cursor = max(cursor, end);
        }
        if (closesAt.isAfter(cursor)) {
            free.add(new TimeRange(cursor, closesAt));
        }
        return free;
    }

    private static LocalTime max(LocalTime a, LocalTime b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalTime min(LocalTime a, LocalTime b) {
        return a.isBefore(b) ? a : b;
    }
}
