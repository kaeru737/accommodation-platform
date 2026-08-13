package com.rs.acd.controller;

import com.rs.acd.domain.Reservation;
import com.rs.acd.domain.User;
import com.rs.acd.service.ReservationService;
import com.rs.acd.service.UserService;
import com.rs.acd.service.WishlistService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final ReservationService reservationService;
    private final WishlistService wishlistService;

    public UserController(UserService userService,
                          ReservationService reservationService,
                          WishlistService wishlistService) {
        this.userService = userService;
        this.reservationService = reservationService;
        this.wishlistService = wishlistService;
    }

    @GetMapping("/new")
    public String showForm(Model model) {
        model.addAttribute("user", new User());
        return "user-form";
    }

    @PostMapping("/new")
    public String register(@ModelAttribute @Valid User user,
                           BindingResult bindingResult,
                           Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            return "user-form";
        }

        try {
            userService.save(user);
            return "redirect:/login";
        } catch (IllegalStateException e) {
            model.addAttribute("user", user);
            model.addAttribute("errorMessage", e.getMessage());
            return "user-form";
        }
    }

    @GetMapping("/list")
    public String list(Model model) {
        List<User> users = userService.findAll();
        model.addAttribute("users", users);
        return "user-list";
    }

    @GetMapping("/mypage")
    public String mypage(HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        List<Reservation> reservations = reservationService.findByUserId(loginUser.getUserId());
        model.addAttribute("user", loginUser);
        model.addAttribute("reservations", reservations);
        return "user/user-mypage";
    }

    // ✅ 위시리스트 페이지: 로그인 + 게스트만
    @GetMapping("/wishlist")
    public String wishlistPage(HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        // 호스트는 여기 못 들어오게
        if (!"guest".equalsIgnoreCase(loginUser.getRole())) {
            return "redirect:/host/mypage";
        }

        model.addAttribute("user", loginUser);
        model.addAttribute("wishlist", wishlistService.getWishlist(loginUser));
        return "user/user-wishlist";
    }

    @GetMapping("/messages")
    public String messagesPage(HttpSession session) {
        if (session.getAttribute("loginUser") == null) {
            return "redirect:/login";
        }
        return "user/user-messages";
    }
}
