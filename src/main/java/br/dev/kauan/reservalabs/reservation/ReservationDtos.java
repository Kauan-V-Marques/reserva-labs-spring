package br.dev.kauan.reservalabs.reservation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class ReservationDtos {

    private ReservationDtos() {
    }

    public record ReservationRequest(
            @NotNull Long laboratoryId,
            @NotBlank @Size(max = 100) String requesterName,
            @NotBlank @Email @Size(max = 120) String requesterEmail,
            @NotBlank @Size(max = 200) String purpose,
            @NotNull @Positive Integer attendees,
            @NotNull LocalDateTime startsAt,
            @NotNull LocalDateTime endsAt) {
    }

    public record CancelRequest(@NotBlank @Email String requesterEmail) {
    }

    public record LabSummary(Long id, String code, String name) {
    }

    public record ReservationResponse(
            Long id, LabSummary laboratory, String requesterName, String requesterEmail, String purpose,
            int attendees, LocalDateTime startsAt, LocalDateTime endsAt, Reservation.Status status,
            LocalDateTime createdAt, LocalDateTime cancelledAt) {

        public static ReservationResponse from(Reservation r) {
            var lab = r.getLaboratory();
            return new ReservationResponse(r.getId(), new LabSummary(lab.getId(), lab.getCode(), lab.getName()),
                    r.getRequesterName(), r.getRequesterEmail(), r.getPurpose(), r.getAttendees(),
                    r.getStartsAt(), r.getEndsAt(), r.getStatus(), r.getCreatedAt(), r.getCancelledAt());
        }
    }

    public record Slot(LocalTime start, LocalTime end, long minutes) {

        static Slot from(TimeRange range) {
            return new Slot(range.start(), range.end(), range.minutes());
        }
    }

    public record AvailabilityResponse(
            LabSummary laboratory, LocalDate date, LocalTime opensAt, LocalTime closesAt,
            List<Slot> reserved, List<Slot> free) {
    }
}
