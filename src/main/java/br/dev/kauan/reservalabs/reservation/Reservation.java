package br.dev.kauan.reservalabs.reservation;

import br.dev.kauan.reservalabs.common.Exceptions.BusinessRuleException;
import br.dev.kauan.reservalabs.lab.Laboratory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
public class Reservation {

    public enum Status { CONFIRMED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "laboratory_id")
    private Laboratory laboratory;

    @Column(nullable = false, length = 100)
    private String requesterName;

    @Column(nullable = false, length = 120)
    private String requesterEmail;

    @Column(nullable = false, length = 200)
    private String purpose;

    @Column(nullable = false)
    private int attendees;

    @Column(nullable = false)
    private LocalDateTime startsAt;

    @Column(nullable = false)
    private LocalDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.CONFIRMED;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime cancelledAt;

    protected Reservation() {
        // exigido pelo JPA
    }

    public Reservation(Laboratory laboratory, String requesterName, String requesterEmail, String purpose,
                       int attendees, LocalDateTime startsAt, LocalDateTime endsAt, LocalDateTime createdAt) {
        this.laboratory = laboratory;
        this.requesterName = requesterName;
        this.requesterEmail = requesterEmail.toLowerCase();
        this.purpose = purpose;
        this.attendees = attendees;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.createdAt = createdAt;
    }

    public void cancel(LocalDateTime now) {
        if (status == Status.CANCELLED) {
            throw new BusinessRuleException("Reservation is already cancelled.");
        }
        if (!startsAt.isAfter(now)) {
            throw new BusinessRuleException("Reservations that already started cannot be cancelled.");
        }
        status = Status.CANCELLED;
        cancelledAt = now;
    }

    public boolean belongsTo(String email) {
        return requesterEmail.equalsIgnoreCase(email.trim());
    }

    public Long getId() {
        return id;
    }

    public Laboratory getLaboratory() {
        return laboratory;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public String getPurpose() {
        return purpose;
    }

    public int getAttendees() {
        return attendees;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public LocalDateTime getEndsAt() {
        return endsAt;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }
}
