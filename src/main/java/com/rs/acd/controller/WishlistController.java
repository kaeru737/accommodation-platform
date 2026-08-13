package com.rs.acd.controller;

import com.rs.acd.domain.User;
import com.rs.acd.service.WishlistService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    /**
     * 숙소 상세 / 위시리스트 페이지에서 공통으로 쓰는 토글 엔드포인트
     * from = "wishlist" 이면 위시리스트 페이지로 리다이렉트
     * 그 외(또는 null)이면 숙소 상세로 리다이렉트
     */
    @PostMapping("/toggle")
    public String toggle(@RequestParam("accommodationId") Long accommodationId,
                         @RequestParam(value = "from", required = false) String from,
                         HttpSession session) {

        User login = (User) session.getAttribute("loginUser");
        if (login == null) {
            return "redirect:/login";
        }

        // 게스트만 위시리스트 사용
        if (!"guest".equalsIgnoreCase(login.getRole())) {
            return "redirect:/host/mypage";
        }

        wishlistService.toggle(login, accommodationId);

        if ("wishlist".equalsIgnoreCase(from)) {
            return "redirect:/users/wishlist";
        }
        return "redirect:/accommodations/" + accommodationId;
    }
}
