package com.rs.acd.repository;

import com.rs.acd.domain.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByAccommodation_Id(Long accommodationId);
    Optional<Image> findFirstByAccommodation_IdAndIsMainTrue(Long accommodationId);
}
