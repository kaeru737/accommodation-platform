package com.rs.acd.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class GeocodingService {

    @Value("${kakao.rest-key}")
    private String restKey;

    private final RestClient rest = RestClient.create();

    /** 주소 → (lat, lng)  [lat=위도, lng=경도] */
    public Optional<double[]> geocode(String address) {
        if (address == null || address.isBlank()) return Optional.empty();

        Map<?, ?> res = rest.get()
            .uri(uri -> uri.scheme("https").host("dapi.kakao.com")
                .path("/v2/local/search/address.json")
                .queryParam("query", address)
                .build())
            .header("Authorization", "KakaoAK " + restKey)
            .retrieve()
            .body(Map.class);

        if (res == null) return Optional.empty();

        List<Map<String, Object>> docs = (List<Map<String, Object>>) res.get("documents");
        if (docs == null || docs.isEmpty()) return Optional.empty();

        Map<String, Object> d = docs.get(0);
        // Kakao 응답: x=경도(lng), y=위도(lat)
        double lng = Double.parseDouble((String) d.get("x"));
        double lat = Double.parseDouble((String) d.get("y"));
        return Optional.of(new double[]{lat, lng});
    }
}
