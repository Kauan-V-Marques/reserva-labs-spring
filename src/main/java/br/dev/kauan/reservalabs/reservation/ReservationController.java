package br.dev.kauan.reservalabs.reservation;

import br.dev.kauan.reservalabs.reservation.ReservationDtos.CancelRequest;
import br.dev.kauan.reservalabs.reservation.ReservationDtos.ReservationRequest;
import br.dev.kauan.reservalabs.reservation.ReservationDtos.ReservationResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationRequest request) {
        ReservationResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/reservations/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public ReservationResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping
    public List<ReservationResponse> search(
            @RequestParam(required = false) Long laboratoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Reservation.Status status) {
        return service.search(laboratoryId, date, email, status);
    }

    @PostMapping("/{id}/cancel")
    public ReservationResponse cancel(@PathVariable Long id, @Valid @RequestBody CancelRequest request) {
        return service.cancel(id, request.requesterEmail());
    }
}
