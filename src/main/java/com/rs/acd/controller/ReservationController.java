package com.rs.acd.controller;

import com.rs.acd.domain.Reservation;
import com.rs.acd.domain.User;
import com.rs.acd.service.ReservationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    // ✅ 예약 생성
    @PostMapping("/book")
    public String book(@RequestParam Long accommodationId,
                       @RequestParam Long userId,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
                       @RequestParam int guestCount,
                       RedirectAttributes redirectAttributes) {

        Reservation reservation = reservationService.createReservation(accommodationId, userId, checkIn, checkOut, guestCount);
        redirectAttributes.addFlashAttribute("msg", "예약 성공!");
        return "redirect:/reservations/" + reservation.getId();
    }

    // ✅ 예약 상세 보기
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("reservation", reservationService.findById(id));
        return "reservation/detail";
    }

    // ✅ 사용자 예약 목록 (마이페이지)
    @GetMapping("/my")
    public String myReservations(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");

        // user가 null이면 guest로 대체
        if (user == null) {
            user = new User();
            user.setNickname("비회원");
            user.setRole("guest");
        }

        model.addAttribute("user", user);
        model.addAttribute("reservations", reservationService.findByUserId(user.getUserId()));

        return "user/user-mypage";
    }

    // ✅ 숙소 예약된 날짜 범위 JSON 응답
    @GetMapping("/accommodation/{id}/reserved-dates")
    @ResponseBody
    public List<Map<String, LocalDate>> getReservedDates(@PathVariable Long id) {
        List<Reservation> reservations = reservationService.findOverlappingReservations(id);
        return reservations.stream()
            .map(res -> Map.of(
                "checkIn", res.getCheckInDate(),
                "checkOut", res.getCheckOutDate()
            ))
            .collect(Collectors.toList());
    }

    // ✅ 예약 취소
    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        reservationService.cancelReservation(id);
        redirectAttributes.addFlashAttribute("msg", "예약이 취소되었습니다.");
        return "redirect:/reservations/my"; // 경로 유지
    }
}
