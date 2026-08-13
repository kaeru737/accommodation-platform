package com.rs.acd.controller;

import com.rs.acd.domain.Accommodation;
import com.rs.acd.service.AccommodationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
public class HomeController {

    private final AccommodationService accommodationService;

    public HomeController(AccommodationService accommodationService) {
        this.accommodationService = accommodationService;
    }

    /**
     * 메인 페이지 + 검색
     * - location, q(키워드), guests, checkIn, checkOut을 받아 필터링
     * - 날짜는 ISO(yyyy-MM-dd)로 받습니다. (flatpickr의 "Y-m-d"와 호환)
     * - 페이지네이션: page(0부터), size 기본 12
     */
    @GetMapping("/")
    public String index(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer guests,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model,
            HttpSession session
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        // 필터가 없더라도 searchMain은 null 파라미터를 허용하므로
        // 항상 하나의 코드 경로로 처리합니다.
        Page<Accommodation> result = accommodationService.searchMain(
                q, location, guests, checkIn, checkOut, pageable
        );

        // 목록(카드 렌더링용) + 페이징(옵션)
        model.addAttribute("accommodations", result.getContent());
        model.addAttribute("result", result);

        // 폼 값 유지용(Echo)
        model.addAttribute("location", location);
        model.addAttribute("q", q);
        model.addAttribute("guests", guests);
        model.addAttribute("checkIn", checkIn);
        model.addAttribute("checkOut", checkOut);

        // 로그인 유저 정보 전달(기존 유지)
        model.addAttribute("session", session);

        return "index";
    }
}
