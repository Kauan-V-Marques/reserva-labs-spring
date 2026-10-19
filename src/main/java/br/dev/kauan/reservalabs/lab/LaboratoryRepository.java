package br.dev.kauan.reservalabs.lab;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface LaboratoryRepository extends JpaRepository<Laboratory, Long> {

    List<Laboratory> findByActiveTrueOrderByCode();

    boolean existsByCodeIgnoreCase(String code);

    /** Trava o laboratório até o fim da transação para serializar reservas concorrentes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Laboratory l where l.id = :id")
    Optional<Laboratory> findByIdForUpdate(Long id);
}
