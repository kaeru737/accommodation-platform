package com.rs.acd.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Getter @Setter
@Table(name = "accommodation")
public class Accommodation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;        // 숙소 이름
    private String location;    // 위치

    @Column(columnDefinition = "TEXT")
    private String description; // 설명

    private int price;          // 가격
    private Integer maxGuests;  // 최대 인원
    private int bedrooms;       // 침실 수
    private int beds;           // 침대 수
    private int bathrooms;      // 욕실 수
    private String checkIn;     // 체크인 시간
    private String checkOut;    // 체크아웃 시간

    // ✅ 위경도(서버 지오코딩 결과 저장)
    // MySQL DECIMAL(10,7) <-> Java BigDecimal 로 매핑
    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;    // 위도(lat)

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;   // 경도(lng)

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User owner;

    @OneToMany(mappedBy = "accommodation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Image> images;

    // ✅ 템플릿에서 사용하기 위한 대표 이미지 URL 필드
    @Transient
    private String mainImageUrl;

    public String getThumbnailUrl() {
        if (images == null || images.isEmpty()) return null;
        return images.stream()
                .filter(Image::isMain)
                .map(Image::getImageUrl)
                .findFirst()
                .orElse(images.get(0).getImageUrl());
    }
}
