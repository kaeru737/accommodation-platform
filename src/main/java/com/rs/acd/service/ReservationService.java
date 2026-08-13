package com.rs.acd.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.time.format.DateTimeFormatter;

import com.rs.acd.domain.Reservation;
import com.rs.acd.domain.Accommodation;
import com.rs.acd.domain.User;
import com.rs.acd.repository.ReservationRepository;
import com.rs.acd.repository.UserRepository;
import com.rs.acd.repository.AccommodationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final AccommodationRepository accommodationRepository;
    private final UserRepository userRepository;
    private final BlockedDateService blockedDateService;

    // ✅ 예약 생성
    public Reservation createReservation(Long accommodationId, Long userId,
                                         LocalDate checkIn, LocalDate checkOut,
                                         int guests) {

        // 1) 기존 중복 예약 체크
        List<Reservation> overlapping = reservationRepository
                .findByAccommodationIdAndCheckOutDateAfterAndCheckInDateBefore(
                        accommodationId, checkIn, checkOut
                );

        if (!overlapping.isEmpty()) {
            throw new IllegalStateException("해당 날짜에는 이미 예약이 존재합니다.");
        }

        // 2) 호스트가 지정한 예약 불가일 체크
        if (blockedDateService.hasBlockedDatesInRange(accommodationId, checkIn, checkOut)) {
            throw new IllegalStateException("선택한 기간 중 호스트가 예약 불가로 지정한 날짜가 포함되어 있습니다.");
        }

        // 3) 숙소 / 유저 조회
        Accommodation accommodation = accommodationRepository.findById(accommodationId)
                .orElseThrow();
        User user = userRepository.findById(userId)
                .orElseThrow();

        // 4) 숙박일수 & 금액 계산
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        int totalPrice = nights * accommodation.getPrice();

        // 5) 예약 엔티티 생성
        Reservation reservation = new Reservation();
        reservation.setAccommodation(accommodation);
        reservation.setUser(user);
        reservation.setCheckInDate(checkIn);
        reservation.setCheckOutDate(checkOut);
        reservation.setGuestCount(guests);
        reservation.setTotalPrice(totalPrice);

        return reservationRepository.save(reservation);
    }

    // ✅ 숙소 정보까지 함께 로딩
    public Reservation findById(Long id) {
        return reservationRepository.findByIdWithAccommodation(id)
                .orElseThrow(() -> new RuntimeException("해당 예약을 찾을 수 없습니다."));
    }

    public List<Reservation> findByUserId(Long userId) {
        return reservationRepository.findByUser_UserId(userId);
    }

    public List<Reservation> findOverlappingReservations(Long accommodationId) {
        return reservationRepository.findByAccommodationId(accommodationId);
    }

    // ✅ 예약 취소
    public void cancelReservation(Long reservationId) {
        reservationRepository.deleteById(reservationId);
    }

    // ✅ 호스트 상세 페이지용: 특정 숙소의 모든 예약(체크인 역순)
    public List<Reservation> getReservationsForAccommodation(Long accommodationId) {
        if (accommodationId == null) return Collections.emptyList();
        return reservationRepository.findAllByAccommodationIdOrderByCheckInDateDesc(accommodationId);
    }

    // ✅ 필요 시: '다가오는' 예약만 (퇴실일이 오늘 이후)
    public List<Reservation> getUpcomingReservationsForAccommodation(Long accommodationId) {
        if (accommodationId == null) return Collections.emptyList();
        LocalDate today = LocalDate.now();
        return reservationRepository
                .findAllByAccommodationIdAndCheckOutDateAfterOrderByCheckInDateAsc(
                        accommodationId, today.minusDays(1) // today 포함 효과
                );
    }

    // ✅ 예약된 날짜 목록(yyyy-MM-dd) — 읽기 전용 캘린더에 사용
    public List<String> getBookedDatesForAccommodation(Long accommodationId) {

        List<String> bookedDates = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 1) 오늘 이후 예약만 가져와서 날짜 막기
        List<Reservation> reservations =
                reservationRepository.findByAccommodationIdAndCheckOutDateAfter(
                        accommodationId,
                        LocalDate.now()
                );

        for (Reservation reservation : reservations) {
            LocalDate d = reservation.getCheckInDate();
            LocalDate last = reservation.getCheckOutDate().minusDays(1); // 체크아웃 전날까지만 막음

            while (!d.isAfter(last)) {
                bookedDates.add(d.format(formatter));
                d = d.plusDays(1);
            }
        }

        // 2) 호스트가 지정한 예약 불가일(BlockedDate)도 같이 막기
        List<String> blocked = blockedDateService.getBlockedDateStrings(accommodationId);
        bookedDates.addAll(blocked);

        return bookedDates;
    }

    // ✅ 중복 존재 여부만 빠르게 체크하고 싶을 때
    public boolean hasOverlap(Long accommodationId, LocalDate checkIn, LocalDate checkOut) {
        List<Reservation> overlapping = reservationRepository
                .findByAccommodationIdAndCheckOutDateAfterAndCheckInDateBefore(
                        accommodationId, checkIn, checkOut
                );
        return !overlapping.isEmpty();
    }

    // ✅ 후기 작성 자격: 체크아웃 완료 이력 보유 여부
    public boolean hasCompletedStay(Long accommodationId, Long userId) {
        if (accommodationId == null || userId == null) return false;
        return reservationRepository
                .existsByAccommodationIdAndUserUserIdAndCheckOutDateBefore(
                        accommodationId, userId, LocalDate.now()
                );
    }
}
