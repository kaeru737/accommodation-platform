package com.rs.acd.service;

import com.rs.acd.domain.Accommodation;
import com.rs.acd.domain.Image;
import com.rs.acd.domain.User;
import com.rs.acd.domain.Wishlist;
import com.rs.acd.repository.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@Transactional
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final AccommodationService accommodationService;

    public WishlistService(WishlistRepository wishlistRepository,
                           AccommodationService accommodationService) {
        this.wishlistRepository = wishlistRepository;
        this.accommodationService = accommodationService;
    }

    /** 로그인 유저 기준 이 숙소가 이미 위시리스트인지 여부 */
    @Transactional(readOnly = true)
    public boolean isWishlisted(User user, Long accommodationId) {
        if (user == null || accommodationId == null) return false;
        return wishlistRepository.existsByUserUserIdAndAccommodationId(
                user.getUserId(), accommodationId
        );
    }

    /** ✅ 위시리스트 전체 목록 + mainImageUrl 강제 세팅 */
    @Transactional(readOnly = true)
    public List<Wishlist> getWishlist(User user) {
        if (user == null) return Collections.emptyList();

        List<Wishlist> list =
                wishlistRepository.findByUserUserIdOrderByCreatedAtDesc(user.getUserId());

        for (Wishlist item : list) {
            Accommodation acc = item.getAccommodation();
            if (acc == null) continue;

            // Lazy 로딩 강제 초기화
            if (acc.getImages() != null) acc.getImages().size();

            // 대표 이미지 선정
            String mainUrl = null;
            if (acc.getImages() != null && !acc.getImages().isEmpty()) {
                mainUrl = acc.getImages().stream()
                        .filter(Image::isMain)
                        .map(Image::getImageUrl)
                        .findFirst()
                        .orElse(acc.getImages().get(0).getImageUrl());
            }

            acc.setMainImageUrl(mainUrl);
        }

        return list;
    }

    /** 위시 추가 */
    public void add(User user, Long accommodationId) {
        if (user == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        if (wishlistRepository.existsByUserUserIdAndAccommodationId(user.getUserId(), accommodationId)) {
            return; // 이미 있으면 무시
        }

        Accommodation acc = accommodationService.findById(accommodationId);
        if (acc == null) {
            throw new IllegalArgumentException("숙소가 존재하지 않습니다.");
        }

        Wishlist wl = new Wishlist();
        wl.setUser(user);
        wl.setAccommodation(acc);
        wishlistRepository.save(wl);
    }

    /** 위시 삭제 */
    public void remove(User user, Long accommodationId) {
        if (user == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        wishlistRepository.deleteByUserUserIdAndAccommodationId(user.getUserId(), accommodationId);
    }

    /** 토글: 있으면 삭제, 없으면 추가 */
    public void toggle(User user, Long accommodationId) {
        if (isWishlisted(user, accommodationId)) {
            remove(user, accommodationId);
        } else {
            add(user, accommodationId);
        }
    }
}
