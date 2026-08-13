package com.rs.acd.repository;

import com.rs.acd.domain.BlockedDate;
import com.rs.acd.domain.Accommodation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BlockedDateRepository extends JpaRepository<BlockedDate, Long> {

    // 특정 숙소의 모든 예약 불가일
    List<BlockedDate> findByAccommodation(Accommodation accommodation);

    // 일정 기간 사이에 예약 불가일이 하나라도 있는지
    boolean existsByAccommodationAndDateBetween(
            Accommodation accommodation,
            LocalDate start,
            LocalDate end
    );

    // 특정 날짜가 이미 예약 불가일인지
    boolean existsByAccommodationAndDate(Accommodation accommodation, LocalDate date);

    // 기간 내 모든 예약 불가일 조회 (달력에 표시용)
    List<BlockedDate> findByAccommodationAndDateBetween(
            Accommodation accommodation,
            LocalDate start,
            LocalDate end
    );
}
