package br.dev.kauan.reservalabs.reservation;

import br.dev.kauan.reservalabs.reservation.Reservation.Status;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    /** Duas faixas se sobrepõem quando uma começa antes de a outra terminar, e vice-versa. */
    @Query("""
            select count(r) > 0 from Reservation r
             where r.laboratory.id = :laboratoryId
               and r.status = :status
               and r.startsAt < :endsAt
               and r.endsAt > :startsAt
            """)
    boolean existsOverlapWithStatus(Long laboratoryId, LocalDateTime startsAt, LocalDateTime endsAt, Status status);

    @Query("""
            select r from Reservation r
             where r.laboratory.id = :laboratoryId
               and r.status = :status
               and r.startsAt >= :from and r.startsAt < :to
             order by r.startsAt
            """)
    List<Reservation> findByStatusBetween(Long laboratoryId, LocalDateTime from, LocalDateTime to, Status status);

    default boolean existsConfirmedOverlap(Long laboratoryId, LocalDateTime startsAt, LocalDateTime endsAt) {
        return existsOverlapWithStatus(laboratoryId, startsAt, endsAt, Status.CONFIRMED);
    }

    default List<Reservation> findConfirmedBetween(Long laboratoryId, LocalDateTime from, LocalDateTime to) {
        return findByStatusBetween(laboratoryId, from, to, Status.CONFIRMED);
    }

    // Filtros opcionais da listagem; cada um vira "sem restrição" quando o valor não é informado

    static Specification<Reservation> laboratory(Long laboratoryId) {
        return laboratoryId == null
                ? Specification.unrestricted()
                : (root, query, cb) -> cb.equal(root.get("laboratory").get("id"), laboratoryId);
    }

    static Specification<Reservation> requester(String email) {
        return email == null || email.isBlank()
                ? Specification.unrestricted()
                : (root, query, cb) -> cb.equal(root.get("requesterEmail"), email.trim().toLowerCase());
    }

    static Specification<Reservation> startsBetween(LocalDateTime from, LocalDateTime to) {
        return from == null
                ? Specification.unrestricted()
                : (root, query, cb) -> cb.and(
                        cb.greaterThanOrEqualTo(root.get("startsAt"), from), cb.lessThan(root.get("startsAt"), to));
    }

    static Specification<Reservation> status(Status status) {
        return status == null
                ? Specification.unrestricted()
                : (root, query, cb) -> cb.equal(root.get("status"), status);
    }
}
