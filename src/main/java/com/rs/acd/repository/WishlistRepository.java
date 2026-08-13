package com.rs.acd.repository;

import com.rs.acd.domain.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    boolean existsByUserUserIdAndAccommodationId(Long userId, Long accommodationId);

    Optional<Wishlist> findByUserUserIdAndAccommodationId(Long userId, Long accommodationId);

    List<Wishlist> findByUserUserIdOrderByCreatedAtDesc(Long userId);

    void deleteByUserUserIdAndAccommodationId(Long userId, Long accommodationId);
}
