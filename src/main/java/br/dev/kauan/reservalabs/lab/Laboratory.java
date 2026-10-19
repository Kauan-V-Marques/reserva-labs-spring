package br.dev.kauan.reservalabs.lab;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;

@Entity
@Table(name = "laboratories")
public class Laboratory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 60)
    private String building;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private LocalTime opensAt;

    @Column(nullable = false)
    private LocalTime closesAt;

    @Column(nullable = false)
    private boolean active = true;

    protected Laboratory() {
        // exigido pelo JPA
    }

    public Laboratory(String code, String name, String building, int capacity, LocalTime opensAt, LocalTime closesAt) {
        this.code = code;
        update(name, building, capacity, opensAt, closesAt, true);
    }

    public void update(String name, String building, int capacity, LocalTime opensAt, LocalTime closesAt,
                       boolean active) {
        this.name = name;
        this.building = building;
        this.capacity = capacity;
        this.opensAt = opensAt;
        this.closesAt = closesAt;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getBuilding() {
        return building;
    }

    public int getCapacity() {
        return capacity;
    }

    public LocalTime getOpensAt() {
        return opensAt;
    }

    public LocalTime getClosesAt() {
        return closesAt;
    }

    public boolean isActive() {
        return active;
    }
}
