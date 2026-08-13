package com.rs.acd.service;

import com.rs.acd.domain.Accommodation;
import com.rs.acd.domain.Review;
import com.rs.acd.domain.User;
import com.rs.acd.repository.ReservationRepository;
import com.rs.acd.repository.ReviewRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.DoubleSummaryStatistics;
import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final AccommodationService accommodationService;
    private final ReservationRepository reservationRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         AccommodationService accommodationService,
                         ReservationRepository reservationRepository) {
        this.reviewRepository = reviewRepository;
        this.accommodationService = accommodationService;
        this.reservationRepository = reservationRepository;
    }

    /** 숙소별 후기 목록 (최신순) */
    public List<Review> getForAccommodation(Long accommodationId) {
        return reviewRepository.findByAccommodationIdOrderByCreatedAtDesc(accommodationId);
    }

    /** 숙소별 평균 평점 (소수 1자리 반올림) */
    public double getAverageRating(Long accommodationId) {
        var list = getForAccommodation(accommodationId);
        if (list.isEmpty()) return 0.0;
        DoubleSummaryStatistics stat = list.stream()
                .mapToDouble(r -> r.getRating() == null ? 0 : r.getRating())
                .summaryStatistics();
        return Math.round(stat.getAverage() * 10) / 10.0;
    }

    /** 후기 작성 (체크아웃 완료한 게스트만 가능) */
    @Transactional
    public Review write(Long accommodationId, User author, int rating, String content) {
        Accommodation acc = accommodationService.findById(accommodationId);
        if (acc == null) throw new IllegalArgumentException("숙소가 존재하지 않습니다.");

        boolean hasStayed = reservationRepository
                .existsByAccommodationIdAndUserUserIdAndCheckOutDateBefore(
                        accommodationId,
                        author.getUserId(),
                        LocalDate.now()
                );
        if (!hasStayed) {
            throw new IllegalStateException("체크아웃 완료한 게스트만 후기를 작성할 수 있습니다.");
        }

        Review r = new Review();
        r.setAccommodation(acc);
        r.setAuthor(author);
        r.setRating(Math.max(1, Math.min(5, rating)));
        r.setContent(content);
        return reviewRepository.save(r);
    }

    /** 후기 삭제 (작성자 본인만) */
    @Transactional
    public void delete(Long reviewId, User requester) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰가 존재하지 않습니다."));
        if (review.getAuthor() == null || requester == null ||
            !review.getAuthor().getUserId().equals(requester.getUserId())) {
            throw new SecurityException("본인이 작성한 리뷰만 삭제할 수 있습니다.");
        }
        reviewRepository.delete(review);
    }

    /** 내가 쓴 후기 목록 (게스트) */
    public List<Review> myReviews(User author) {
        return reviewRepository.findByAuthorOrderByCreatedAtDesc(author);
    }

    /** ✅ 호스트: 내가 소유한 숙소들에 달린 모든 후기 */
    public List<Review> hostReviews(User host) {
        if (host == null) return List.of();
        return reviewRepository.findAllByHostId(host.getUserId());
    }

    /** ✅ 리뷰 단건 조회 (컨트롤러 redirect용) */
    public Review getById(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("리뷰가 존재하지 않습니다."));
    }

    /* ==============================
       ⭐ 호스트 답변 작성 / 수정
       ============================== */
    @Transactional
    public void addHostReply(Long reviewId, String replyContent, User host) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰가 존재하지 않습니다."));

        // 이 리뷰의 숙소 호스트인지 검증
        if (review.getAccommodation() == null ||
            review.getAccommodation().getOwner() == null ||
            !review.getAccommodation().getOwner().getUserId().equals(host.getUserId())) {
            throw new SecurityException("해당 숙소의 호스트만 답변할 수 있습니다.");
        }

        review.setHostReply(replyContent);
        review.setHostReplyCreatedAt(LocalDateTime.now());

        reviewRepository.save(review);
    }

    /* ==============================
       ⭐ 호스트 답변 삭제
       ============================== */
    @Transactional
    public void deleteHostReply(Long reviewId, User host) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰가 존재하지 않습니다."));

        if (review.getAccommodation() == null ||
            review.getAccommodation().getOwner() == null ||
            !review.getAccommodation().getOwner().getUserId().equals(host.getUserId())) {
            throw new SecurityException("해당 숙소의 호스트만 답변을 삭제할 수 있습니다.");
        }

        review.setHostReply(null);
        review.setHostReplyCreatedAt(null);

        reviewRepository.save(review);
    }
}
