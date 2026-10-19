package br.dev.kauan.reservalabs.reservation;

import br.dev.kauan.reservalabs.common.Exceptions.ConflictException;
import br.dev.kauan.reservalabs.common.Exceptions.ForbiddenException;
import br.dev.kauan.reservalabs.common.Exceptions.NotFoundException;
import br.dev.kauan.reservalabs.lab.Laboratory;
import br.dev.kauan.reservalabs.lab.LaboratoryRepository;
import br.dev.kauan.reservalabs.reservation.ReservationDtos.AvailabilityResponse;
import br.dev.kauan.reservalabs.reservation.ReservationDtos.LabSummary;
import br.dev.kauan.reservalabs.reservation.ReservationDtos.ReservationRequest;
import br.dev.kauan.reservalabs.reservation.ReservationDtos.ReservationResponse;
import br.dev.kauan.reservalabs.reservation.ReservationDtos.Slot;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {

    private final ReservationRepository reservations;
    private final LaboratoryRepository laboratories;
    private final Clock clock;

    public ReservationService(ReservationRepository reservations, LaboratoryRepository laboratories, Clock clock) {
        this.reservations = reservations;
        this.laboratories = laboratories;
        this.clock = clock;
    }

    @Transactional
    public ReservationResponse create(ReservationRequest request) {
        // A trava no laboratório faz pedidos simultâneos para o mesmo lab esperarem a vez
        Laboratory lab = laboratories.findByIdForUpdate(request.laboratoryId())
                .orElseThrow(() -> new NotFoundException("Laboratory %d not found.".formatted(request.laboratoryId())));

        LocalDateTime now = LocalDateTime.now(clock);
        ReservationRules.validate(lab, request.startsAt(), request.endsAt(), request.attendees(), now);

        if (reservations.existsConfirmedOverlap(lab.getId(), request.startsAt(), request.endsAt())) {
            throw new ConflictException("Laboratory %s is already booked in this period.".formatted(lab.getCode()));
        }

        Reservation reservation = new Reservation(lab, request.requesterName().trim(), request.requesterEmail().trim(),
                request.purpose().trim(), request.attendees(), request.startsAt(), request.endsAt(), now);
        return ReservationResponse.from(reservations.saveAndFlush(reservation));
    }

    @Transactional(readOnly = true)
    public ReservationResponse get(Long id) {
        return ReservationResponse.from(find(id));
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> search(Long laboratoryId, LocalDate date, String email, Reservation.Status status) {
        Specification<Reservation> filter = Specification.allOf(
                ReservationRepository.laboratory(laboratoryId),
                ReservationRepository.requester(email),
                ReservationRepository.startsBetween(
                        date == null ? null : date.atStartOfDay(),
                        date == null ? null : date.plusDays(1).atStartOfDay()),
                ReservationRepository.status(status));

        return reservations.findAll(filter, Sort.by("startsAt")).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    @Transactional
    public ReservationResponse cancel(Long id, String requesterEmail) {
        Reservation reservation = find(id);
        if (!reservation.belongsTo(requesterEmail)) {
            throw new ForbiddenException("Only the person who made the reservation can cancel it.");
        }
        reservation.cancel(LocalDateTime.now(clock));
        return ReservationResponse.from(reservation);
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse availability(Long laboratoryId, LocalDate date) {
        Laboratory lab = laboratories.findById(laboratoryId)
                .orElseThrow(() -> new NotFoundException("Laboratory %d not found.".formatted(laboratoryId)));

        List<TimeRange> busy = reservations
                .findConfirmedBetween(lab.getId(), date.atStartOfDay(), date.plusDays(1).atStartOfDay())
                .stream()
                .map(r -> new TimeRange(r.getStartsAt().toLocalTime(), r.getEndsAt().toLocalTime()))
                .toList();

        List<Slot> free = AvailabilityCalculator.freeSlots(lab.getOpensAt(), lab.getClosesAt(), busy).stream()
                .filter(slot -> slot.minutes() >= ReservationRules.MIN_DURATION.toMinutes())
                .map(Slot::from)
                .toList();

        return new AvailabilityResponse(new LabSummary(lab.getId(), lab.getCode(), lab.getName()), date,
                lab.getOpensAt(), lab.getClosesAt(), busy.stream().map(Slot::from).toList(), free);
    }

    private Reservation find(Long id) {
        return reservations.findById(id)
                .orElseThrow(() -> new NotFoundException("Reservation %d not found.".formatted(id)));
    }
}
