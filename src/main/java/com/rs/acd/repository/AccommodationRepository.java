package com.rs.acd.repository;

import com.rs.acd.domain.Accommodation;
import com.rs.acd.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 숙소 엔티티에 대한 DB 접근 인터페이스 (JPA 사용)
 */
@Repository
public interface AccommodationRepository extends JpaRepository<Accommodation, Long> {

    // 특정 사용자(호스트)가 등록한 숙소만 조회
    List<Accommodation> findByOwner(User user);

    /**
     * 메인 검색: 키워드/위치 + 인원 + 날짜(가용성) 필터
     * - checkIn & checkOut 모두 있을 때만 예약 겹침 배제
     * - 날짜 겹침 판정: (r.checkOut > :checkIn) AND (r.checkIn < :checkOut)
     */
    @Query("""
        SELECT a FROM Accommodation a
        WHERE (:q IS NULL OR
               LOWER(a.name)        LIKE LOWER(CONCAT('%', :q, '%')) OR
               LOWER(a.description) LIKE LOWER(CONCAT('%', :q, '%')) OR
               LOWER(a.location)    LIKE LOWER(CONCAT('%', :q, '%')))
          AND (:location IS NULL OR LOWER(a.location) LIKE LOWER(CONCAT('%', :location, '%')))
          AND (:guests IS NULL OR a.maxGuests >= :guests)
          AND (
               :checkIn IS NULL OR :checkOut IS NULL OR
               NOT EXISTS (
                 SELECT r FROM com.rs.acd.domain.Reservation r
                  WHERE r.accommodation = a
                    AND r.checkOutDate > :checkIn
                    AND r.checkInDate  < :checkOut
               )
          )
        """)
    Page<Accommodation> searchAvailable(
            @Param("q") String q,
            @Param("location") String location,
            @Param("guests") Integer guests,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            Pageable pageable
    );
}
