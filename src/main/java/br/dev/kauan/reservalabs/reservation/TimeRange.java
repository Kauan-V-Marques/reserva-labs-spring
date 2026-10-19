package br.dev.kauan.reservalabs.reservation;

import java.time.Duration;
import java.time.LocalTime;

/** Intervalo de horário dentro de um dia: [start, end). */
public record TimeRange(LocalTime start, LocalTime end) {

    public TimeRange {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("end must be after start");
        }
    }

    public long minutes() {
        return Duration.between(start, end).toMinutes();
    }
}
