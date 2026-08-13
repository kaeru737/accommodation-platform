package com.rs.acd.controller;

import com.rs.acd.domain.Accommodation;
import com.rs.acd.domain.User;
import com.rs.acd.dto.ReservationCalendarDto;
import com.rs.acd.service.AccommodationService;
import com.rs.acd.service.ReservationService;
import com.rs.acd.service.ReviewService;
import com.rs.acd.service.WishlistService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Controller
public class AccommodationController {

    private final AccommodationService accommodationService;
    private final ReservationService reservationService;
    private final ReviewService reviewService;
    private final WishlistService wishlistService;

    public AccommodationController(AccommodationService accommodationService,
                                   ReservationService reservationService,
                                   ReviewService reviewService,
                                   WishlistService wishlistService) {
        this.accommodationService = accommodationService;
        this.reservationService = reservationService;
        this.reviewService = reviewService;
        this.wishlistService = wishlistService;
    }

    /* =========================
       🔍 메인 페이지 (검색 + 정렬)
       ========================= */
    @GetMapping("/accommodations")
    public String mainPage(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String checkIn,
            @RequestParam(required = false) String checkOut,
            @RequestParam(required = false) Integer guests,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model
    ) {

        /* 🔽 정렬 설정 */
        Sort sortOption = switch (sort) {
            case "priceAsc" -> Sort.by("price").ascending();
            case "priceDesc" -> Sort.by("price").descending();
            case "recent" -> Sort.by("id").descending();
            default -> Sort.by("id").descending();
        };

        Pageable pageable = PageRequest.of(page, size, sortOption);

        LocalDate ci = (checkIn != null && !checkIn.isBlank()) ? LocalDate.parse(checkIn) : null;
        LocalDate co = (checkOut != null && !checkOut.isBlank()) ? LocalDate.parse(checkOut) : null;

        Page<Accommodation> result =
                accommodationService.searchMain(q, location, guests, ci, co, pageable);

        model.addAttribute("result", result);
        model.addAttribute("accommodations", result.getContent());

        // 검색 조건 유지
        model.addAttribute("location", location);
        model.addAttribute("checkIn", checkIn);
        model.addAttribute("checkOut", checkOut);
        model.addAttribute("guests", guests);
        model.addAttribute("q", q);
        model.addAttribute("sort", sort);

        return "index";
    }

    /* =========================
       생성/수정/삭제
       ========================= */
    @GetMapping("/accommodations/new")
    public String newForm(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null || !"host".equals(user.getRole())) return "redirect:/login";

        model.addAttribute("accommodation", new Accommodation());
        return "accommodation/form";
    }

    @PostMapping("/accommodations/new")
    public String registerAccommodation(
            @RequestParam String name,
            @RequestParam String location,
            @RequestParam String description,
            @RequestParam int price,
            @RequestParam int maxGuests,
            @RequestParam int bedrooms,
            @RequestParam int beds,
            @RequestParam int bathrooms,
            @RequestParam String checkIn,
            @RequestParam String checkOut,
            @RequestParam(value = "latitude", required = false) BigDecimal latitude,
            @RequestParam(value = "longitude", required = false) BigDecimal longitude,
            @RequestParam("images") MultipartFile[] imageFiles,
            @RequestParam("mainImageIndex") int mainImageIndex,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null || !"host".equals(user.getRole())) return "redirect:/login";

        Accommodation accommodation = new Accommodation();
        accommodation.setName(name);
        accommodation.setLocation(location);
        accommodation.setDescription(description);
        accommodation.setPrice(price);
        accommodation.setMaxGuests(maxGuests);
        accommodation.setBedrooms(bedrooms);
        accommodation.setBeds(beds);
        accommodation.setBathrooms(bathrooms);
        accommodation.setCheckIn(checkIn);
        accommodation.setCheckOut(checkOut);
        accommodation.setOwner(user);
        accommodation.setLatitude(latitude);
        accommodation.setLongitude(longitude);

        accommodationService.saveWithImages(accommodation, imageFiles, mainImageIndex);
        return "redirect:/host/mypage";
    }

    @GetMapping("/accommodations/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null || !"host".equals(loginUser.getRole())) return "redirect:/login";

        Accommodation accommodation = accommodationService.findById(id);
        if (accommodation == null ||
                !accommodation.getOwner().getUserId().equals(loginUser.getUserId())) {
            return "redirect:/";
        }

        model.addAttribute("accommodation", accommodation);
        return "accommodation/edit";
    }

    @PostMapping("/accommodations/{id}/edit")
    public String updateAccommodation(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam String location,
            @RequestParam String description,
            @RequestParam int price,
            @RequestParam(value = "mainImageId", required = false) Long mainImageId,
            @RequestParam(value = "newImages", required = false) MultipartFile[] newImages,
            HttpSession session
    ) {
        User user = (User) session.getAttribute("loginUser");
        accommodationService.updateAccommodation(id, name, location, description, price,
                mainImageId, newImages, user);
        return "redirect:/host/mypage";
    }

    @PostMapping("/accommodations/{id}/delete")
    public String delete(@PathVariable Long id) {
        accommodationService.deleteById(id);
        return "redirect:/host/mypage";
    }

    /* =========================
       상세 보기
       ========================= */
    @GetMapping("/accommodations/{id}")
    public String detail(
            @PathVariable Long id,
            @RequestParam(value = "month", required = false) String selectedMonth,
            Model model,
            HttpSession session
    ) {
        Accommodation acc = accommodationService.findById(id);
        if (acc == null) return "redirect:/";

        model.addAttribute("accommodation", acc);

        model.addAttribute("bookedDates",
                reservationService.getBookedDatesForAccommodation(id));

        model.addAttribute("reviews", reviewService.getForAccommodation(id));
        model.addAttribute("avgRating", reviewService.getAverageRating(id));

        Long hostId = (acc.getOwner() != null ? acc.getOwner().getUserId() : null);
        String hostNickname = (acc.getOwner() != null ? acc.getOwner().getNickname() : null);
        model.addAttribute("hostId", hostId);
        model.addAttribute("hostNickname", hostNickname);

        User loginUser = (User) session.getAttribute("loginUser");

        boolean isOwner =
                (loginUser != null)
                        && "host".equals(loginUser.getRole())
                        && acc.getOwner() != null
                        && acc.getOwner().getUserId().equals(loginUser.getUserId());

        model.addAttribute("isOwner", isOwner);

        boolean isWish = false;
        if (loginUser != null && "guest".equalsIgnoreCase(loginUser.getRole())) {
            isWish = wishlistService.isWishlisted(loginUser, id);
        }
        model.addAttribute("isWish", isWish);

        boolean canReview = false;
        if (loginUser != null && !"host".equals(loginUser.getRole())) {
            canReview = reservationService.hasCompletedStay(id, loginUser.getUserId());
        }
        model.addAttribute("canReview", canReview);

        if (isOwner) {
            var reservations = reservationService.getReservationsForAccommodation(id);
            model.addAttribute("reservations", reservations);

            var calendarDtos = reservations.stream()
                    .map(r -> new ReservationCalendarDto(
                            r.getId(),
                            r.getCheckInDate(),
                            r.getCheckOutDate(),
                            (r.getUser() != null ? r.getUser().getNickname() : "게스트"),
                            r.getGuestCount()
                    ))
                    .toList();

            model.addAttribute("resvDtos", calendarDtos);

            YearMonth now = YearMonth.now();
            int thisMonthReservationCount = 0;
            int totalReservationCount = reservations.size();

            for (var r : reservations) {
                LocalDate ci = r.getCheckInDate();
                if (ci != null && YearMonth.from(ci).equals(now)) {
                    thisMonthReservationCount++;
                }
            }

            model.addAttribute("thisMonthReservationCount", thisMonthReservationCount);
            model.addAttribute("totalReservationCount", totalReservationCount);

            YearMonth start = now.minusMonths(10);
            List<Map<String, String>> pastMonths = new ArrayList<>();

            for (YearMonth ym = start; !ym.isAfter(now); ym = ym.plusMonths(1)) {
                Map<String, String> map = new HashMap<>();
                map.put("value", ym.toString());
                map.put("label", ym.getYear() + "년 " + ym.getMonthValue() + "월");
                pastMonths.add(map);
            }
            model.addAttribute("pastMonths", pastMonths);

            if (selectedMonth != null && !selectedMonth.isBlank()) {
                try {
                    YearMonth ymSelected = YearMonth.parse(selectedMonth);

                    int selectedMonthReservationCount = 0;
                    Set<LocalDate> reservedDates = new HashSet<>();

                    for (var r : reservations) {
                        LocalDate ci = r.getCheckInDate();
                        LocalDate co = r.getCheckOutDate();
                        if (ci == null || co == null) continue;

                        if (YearMonth.from(ci).equals(ymSelected)) {
                            selectedMonthReservationCount++;
                        }

                        LocalDate d = ci;
                        while (d.isBefore(co)) {
                            if (YearMonth.from(d).equals(ymSelected)) {
                                reservedDates.add(d);
                            }
                            d = d.plusDays(1);
                        }
                    }

                    List<String> reservedDateStrings = reservedDates.stream()
                            .sorted()
                            .map(LocalDate::toString)
                            .toList();

                    model.addAttribute("selectedMonth", selectedMonth);
                    model.addAttribute("selectedMonthLabel",
                            ymSelected.getYear() + "년 " + ymSelected.getMonthValue() + "월");
                    model.addAttribute("selectedMonthReservationCount", selectedMonthReservationCount);
                    model.addAttribute("selectedMonthReservedDates", reservedDateStrings);

                } catch (Exception ignored) { }
            }
        }

        return "accommodation/detail";
    }
}
