package br.dev.kauan.reservalabs.lab;

import br.dev.kauan.reservalabs.common.Exceptions.BusinessRuleException;
import br.dev.kauan.reservalabs.common.Exceptions.ConflictException;
import br.dev.kauan.reservalabs.common.Exceptions.NotFoundException;
import br.dev.kauan.reservalabs.lab.LaboratoryDtos.LaboratoryRequest;
import br.dev.kauan.reservalabs.lab.LaboratoryDtos.LaboratoryResponse;
import br.dev.kauan.reservalabs.reservation.ReservationDtos.AvailabilityResponse;
import br.dev.kauan.reservalabs.reservation.ReservationService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/labs")
public class LaboratoryController {

    private final LaboratoryRepository repository;
    private final ReservationService reservations;

    public LaboratoryController(LaboratoryRepository repository, ReservationService reservations) {
        this.repository = repository;
        this.reservations = reservations;
    }

    @GetMapping
    public List<LaboratoryResponse> list() {
        return repository.findByActiveTrueOrderByCode().stream().map(LaboratoryResponse::from).toList();
    }

    @GetMapping("/{id}")
    public LaboratoryResponse get(@PathVariable Long id) {
        return LaboratoryResponse.from(find(id));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<LaboratoryResponse> create(@Valid @RequestBody LaboratoryRequest request) {
        checkHours(request);
        String code = request.code().toUpperCase();
        if (repository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("Laboratory code %s is already in use.".formatted(code));
        }
        Laboratory lab = repository.save(new Laboratory(code, request.name().trim(), request.building().trim(),
                request.capacity(), request.opensAt(), request.closesAt()));
        return ResponseEntity.created(URI.create("/api/labs/" + lab.getId())).body(LaboratoryResponse.from(lab));
    }

    @PutMapping("/{id}")
    @Transactional
    public LaboratoryResponse update(@PathVariable Long id, @Valid @RequestBody LaboratoryRequest request) {
        checkHours(request);
        Laboratory lab = find(id);
        lab.update(request.name().trim(), request.building().trim(), request.capacity(), request.opensAt(),
                request.closesAt(), request.active() == null || request.active());
        return LaboratoryResponse.from(lab);
    }

    @GetMapping("/{id}/availability")
    public AvailabilityResponse availability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reservations.availability(id, date);
    }

    private Laboratory find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Laboratory %d not found.".formatted(id)));
    }

    private static void checkHours(LaboratoryRequest request) {
        if (!request.closesAt().isAfter(request.opensAt())) {
            throw new BusinessRuleException("Closing time must be after opening time.");
        }
    }
}
