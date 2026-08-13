package com.rs.acd.repository;

import com.rs.acd.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // 특정 숙소의 모든 예약
    List<Reservation> findByAccommodationId(Long accommodationId);

    // 예약된 날짜 계산용(오늘 이후 예약만)
    List<Reservation> findByAccommodationIdAndCheckOutDateAfter(Long accommodationId, LocalDate date);

    // 호스트 예약 현황 테이블용
    List<Reservation> findAllByAccommodationIdOrderByCheckInDateDesc(Long accommodationId);

    // '다가오는' 예약
    List<Reservation> findAllByAccommodationIdAndCheckOutDateAfterOrderByCheckInDateAsc(
            Long accommodationId, LocalDate dateAfterExclusive
    );

    // 특정 사용자의 모든 예약
    List<Reservation> findByUser_UserId(Long userId);

    // 중복 예약 검사용
    List<Reservation> findByAccommodationIdAndCheckOutDateAfterAndCheckInDateBefore(
            Long accommodationId, LocalDate checkInDate, LocalDate checkOutDate
    );

    // 숙소 정보 Fetch Join
    @Query("SELECT r FROM Reservation r JOIN FETCH r.accommodation WHERE r.id = :id")
    Optional<Reservation> findByIdWithAccommodation(@Param("id") Long id);

    // 후기 가능 여부(체크아웃 초과)
    boolean existsByAccommodationIdAndUserUserIdAndCheckOutDateBefore(
            Long accommodationId,
            Long userId,
            LocalDate date
    );
}
