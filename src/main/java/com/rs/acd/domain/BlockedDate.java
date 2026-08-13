package com.rs.acd.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "blocked_date")
public class BlockedDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 숙소의 예약 불가일인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private Accommodation accommodation;

    // 예약을 막을 날짜 (하루 단위)
    @Column(nullable = false)
    private LocalDate date;

    public BlockedDate() {
    }

    public BlockedDate(Accommodation accommodation, LocalDate date) {
        this.accommodation = accommodation;
        this.date = date;
    }

    // === getter / setter ===

    public Long getId() {
        return id;
    }

    public Accommodation getAccommodation() {
        return accommodation;
    }

    public void setAccommodation(Accommodation accommodation) {
        this.accommodation = accommodation;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
