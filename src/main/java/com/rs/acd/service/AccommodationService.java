package com.rs.acd.service;

import com.rs.acd.domain.Accommodation;
import com.rs.acd.domain.Image;
import com.rs.acd.domain.User;
import com.rs.acd.repository.AccommodationRepository;
import com.rs.acd.repository.ImageRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class AccommodationService {

    private final AccommodationRepository accommodationRepository;
    private final ImageRepository imageRepository;
    private final FileUploadService fileUploadService;
    private final GeocodingService geocodingService;

    public AccommodationService(AccommodationRepository accommodationRepository,
                                ImageRepository imageRepository,
                                FileUploadService fileUploadService,
                                GeocodingService geocodingService) {
        this.accommodationRepository = accommodationRepository;
        this.imageRepository = imageRepository;
        this.fileUploadService = fileUploadService;
        this.geocodingService = geocodingService;
    }

    public Accommodation save(Accommodation accommodation) {
        return accommodationRepository.save(accommodation);
    }

    public List<Accommodation> findAll() {
        return accommodationRepository.findAll();
    }

    public Accommodation findById(Long id) {
        Accommodation accommodation = accommodationRepository.findById(id).orElse(null);
        if (accommodation != null) {
            accommodation.getImages().size();

            accommodation.setMainImageUrl(
                    accommodation.getImages().stream()
                            .filter(Image::isMain)
                            .map(Image::getImageUrl)
                            .findFirst()
                            .orElse(accommodation.getImages().isEmpty()
                                    ? null
                                    : accommodation.getImages().get(0).getImageUrl())
            );
        }
        return accommodation;
    }

    public void deleteById(Long id) {
        accommodationRepository.deleteById(id);
    }

    public List<Accommodation> findByOwner(User owner) {
        return accommodationRepository.findByOwner(owner);
    }

    @Transactional
    public void saveWithImages(Accommodation accommodation,
                               MultipartFile[] imageFiles,
                               int mainImageIndex) {

        if (accommodation.getLocation() != null
                && (accommodation.getLatitude() == null || accommodation.getLongitude() == null)) {
            geocodingService.geocode(accommodation.getLocation())
                    .ifPresent(latlng -> {
                        accommodation.setLatitude(BigDecimal.valueOf(latlng[0]));
                        accommodation.setLongitude(BigDecimal.valueOf(latlng[1]));
                    });
        }

        accommodationRepository.save(accommodation);
        List<Image> imageList = new ArrayList<>();

        if (imageFiles != null) {
            for (int i = 0; i < imageFiles.length; i++) {
                MultipartFile file = imageFiles[i];
                if (file.isEmpty()) continue;

                String savedPath = fileUploadService.save(file);
                Image img = new Image();
                img.setAccommodation(accommodation);
                img.setImageUrl(savedPath);
                img.setMain(i == mainImageIndex);
                imageList.add(img);
            }
        }

        accommodation.setImages(imageList);
    }

    @Transactional
    public void updateAccommodation(Long id,
                                    String name,
                                    String location,
                                    String description,
                                    int price,
                                    Long mainImageId,
                                    MultipartFile[] newImages,
                                    User user) {

        Accommodation accommodation = accommodationRepository.findById(id).orElseThrow();

        boolean locationChanged = (location != null && !location.equals(accommodation.getLocation()));

        accommodation.setName(name);
        accommodation.setLocation(location);
        accommodation.setDescription(description);
        accommodation.setPrice(price);
        accommodation.setOwner(user);

        if (locationChanged) {
            geocodingService.geocode(location)
                    .ifPresentOrElse(latlng -> {
                        accommodation.setLatitude(BigDecimal.valueOf(latlng[0]));
                        accommodation.setLongitude(BigDecimal.valueOf(latlng[1]));
                    }, () -> {
                        accommodation.setLatitude(null);
                        accommodation.setLongitude(null);
                    });
        }

        if (accommodation.getImages() != null) {
            for (Image img : accommodation.getImages()) {
                boolean isMain = (mainImageId != null && img.getId() != null && img.getId().equals(mainImageId));
                img.setMain(isMain);
            }
        }

        if (newImages != null) {
            for (MultipartFile file : newImages) {
                if (file.isEmpty()) continue;

                String path = fileUploadService.save(file);
                Image newImg = new Image();
                newImg.setAccommodation(accommodation);
                newImg.setImageUrl(path);
                newImg.setMain(false);
                accommodation.getImages().add(newImg);
            }
        }

        accommodationRepository.save(accommodation);
    }

    /* =========================
       🔎 기본 검색
       ========================= */
    public Page<Accommodation> searchMain(
            String q,
            String location,
            Integer guests,
            LocalDate checkIn,
            LocalDate checkOut,
            Pageable pageable
    ) {
        return accommodationRepository.searchAvailable(
                nullIfBlank(q),
                nullIfBlank(location),
                guests,
                checkIn,
                checkOut,
                pageable
        );
    }

    private String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
