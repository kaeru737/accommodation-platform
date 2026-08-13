package com.rs.acd.repository;

import com.rs.acd.domain.Review;
import com.rs.acd.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // ✅ 숙소 상세에서 사용: 숙소별 후기 (최신순)
    List<Review> findByAccommodationIdOrderByCreatedAtDesc(Long accommodationId);

    // ✅ "내가 쓴 후기" (게스트)
    List<Review> findByAuthorOrderByCreatedAtDesc(User author);

    // ✅ 숙소별 후기 개수
    long countByAccommodationId(Long accommodationId);

    // ✅ 호스트가 소유한 숙소들에 달린 모든 후기 (최신순)
    // Accommodation 엔티티의 필드명이 owner 이므로 a.owner.userId 를 사용해야 함
    @Query("""
           select distinct r
             from Review r
             join fetch r.accommodation a
             left join fetch r.author au
            where a.owner.userId = :hostId
            order by r.createdAt desc
           """)
    List<Review> findAllByHostId(@Param("hostId") Long hostId);

    /*  ── 대안(선호 시): 파생 쿼리만으로도 가능 (fetch join은 안 됨)
        List<Review> findByAccommodationOwnerUserIdOrderByCreatedAtDesc(Long hostId);
     */
}
