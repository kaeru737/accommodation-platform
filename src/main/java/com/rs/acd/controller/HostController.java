package com.rs.acd.controller;

import com.rs.acd.domain.Accommodation;
import com.rs.acd.domain.User;
import com.rs.acd.service.AccommodationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class HostController {

    private final AccommodationService accommodationService;

    public HostController(AccommodationService accommodationService) {
        this.accommodationService = accommodationService;
    }

    // ✅ 마이페이지
    @GetMapping("/host/mypage")
    public String myPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null || !"host".equals(user.getRole())) {
            return "redirect:/login";
        }
        List<Accommodation> myAccommodations = accommodationService.findByOwner(user);
        model.addAttribute("myAccommodations", myAccommodations);
        model.addAttribute("user", user); // 🔥 이 줄 추가
        return "host-mypage";
    }
}
