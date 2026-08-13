package com.rs.acd.dto;

import java.time.LocalDate;

public class ReservationCalendarDto {
    private final Long id;
    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;
    private final String guestName;
    private final Integer guestCount;

    public ReservationCalendarDto(Long id, LocalDate checkInDate, LocalDate checkOutDate,
                                  String guestName, Integer guestCount) {
        this.id = id;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.guestName = guestName;
        this.guestCount = guestCount;
    }
    public Long getId() { return id; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public String getGuestName() { return guestName; }
    public Integer getGuestCount() { return guestCount; }
}
