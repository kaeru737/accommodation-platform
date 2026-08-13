package com.rs.acd.service;

import com.rs.acd.domain.Accommodation;
import com.rs.acd.domain.BlockedDate;
import com.rs.acd.domain.User;
import com.rs.acd.repository.AccommodationRepository;
import com.rs.acd.repository.BlockedDateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class BlockedDateService {

    private final BlockedDateRepository blockedDateRepository;
    private final AccommodationRepository accommodationRepository;

    public BlockedDateService(BlockedDateRepository blockedDateRepository,
                              AccommodationRepository accommodationRepository) {
        this.blockedDateRepository = blockedDateRepository;
        this.accommodationRepository = accommodationRepository;
    }

    /**
     * 로그인한 사용자가 소유한 숙소인지 검증
     * - Accommodation.owner 를 기준으로 체크
     */
    private Accommodation getAccommodationOwnedBy(Long accommodationId, User loginUser) {
        Accommodation accommodation = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new IllegalArgumentException("숙소를 찾을 수 없습니다."));

        if (loginUser == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }

        User owner = accommodation.getOwner();
        if (owner == null || owner.getUserId() == null ||
                !owner.getUserId().equals(loginUser.getUserId())) {
            throw new IllegalStateException("해당 숙소의 호스트만 예약 불가일을 설정할 수 있습니다.");
        }

        return accommodation;
    }

    /**
     * 예약 불가일 추가
     */
    public void addBlockedDate(Long accommodationId, LocalDate date, User loginUser) {
        Accommodation accommodation = getAccommodationOwnedBy(accommodationId, loginUser);

        // 이미 같은 날짜가 예약 불가로 등록되어 있으면 무시
        if (blockedDateRepository.existsByAccommodationAndDate(accommodation, date)) {
            return;
        }

        BlockedDate blockedDate = new BlockedDate(accommodation, date);
        blockedDateRepository.save(blockedDate);
    }

    /**
     * 예약 불가일 삭제
     */
    public void deleteBlockedDate(Long blockedDateId, User loginUser) {
        BlockedDate blockedDate = blockedDateRepository.findById(blockedDateId)
                .orElseThrow(() -> new IllegalArgumentException("예약 불가일 정보를 찾을 수 없습니다."));

        Accommodation accommodation = blockedDate.getAccommodation();
        if (accommodation == null || accommodation.getOwner() == null ||
                loginUser == null ||
                !accommodation.getOwner().getUserId().equals(loginUser.getUserId())) {
            throw new IllegalStateException("해당 숙소의 호스트만 예약 불가일을 삭제할 수 있습니다.");
        }

        blockedDateRepository.delete(blockedDate);
    }

    /**
     * 특정 숙소의 모든 예약 불가일 조회 (호스트 마이페이지용)
     */
    @Transactional(readOnly = true)
    public List<BlockedDate> getBlockedDates(Long accommodationId, User loginUser) {
        Accommodation accommodation = getAccommodationOwnedBy(accommodationId, loginUser);
        return blockedDateRepository.findByAccommodation(accommodation);
    }

    /**
     * ✅ 캘린더(달력)용: 어떤 유저던지 사용 가능하게
     *  - 예약 상세 페이지 / 게스트 예약 폼에서 "막힌 날짜" 를 얻기 위한 용도
     *  - yyyy-MM-dd 형식 문자열 목록으로 반환
     */
    @Transactional(readOnly = true)
    public List<String> getBlockedDateStrings(Long accommodationId) {
        Accommodation accommodation = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new IllegalArgumentException("숙소를 찾을 수 없습니다."));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return blockedDateRepository.findByAccommodation(accommodation).stream()
                .map(BlockedDate::getDate)
                .filter(Objects::nonNull)
                .map(d -> d.format(formatter))
                .collect(Collectors.toList());
    }

    /**
     * 예약하려는 기간에 예약 불가일이 포함되어 있는지 체크
     * - ReservationService에서 예약 생성 시 호출
     * - 여기서는 소유자 체크는 하지 않고, 단순히 숙소 + 기간 기준으로만 검사
     */
    @Transactional(readOnly = true)
    public boolean hasBlockedDatesInRange(Long accommodationId,
                                          LocalDate checkIn,
                                          LocalDate checkOut) {

        Accommodation accommodation = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new IllegalArgumentException("숙소를 찾을 수 없습니다."));

        // 체크아웃 당일은 숙박하지 않으므로 checkOut.minusDays(1) 까지만 검사
        LocalDate end = checkOut.minusDays(1);
        if (end.isBefore(checkIn)) {
            end = checkIn;
        }

        return blockedDateRepository.existsByAccommodationAndDateBetween(
                accommodation, checkIn, end
        );
    }
}
