package com.rs.acd.controller;

import com.rs.acd.domain.Review;
import com.rs.acd.domain.User;
import com.rs.acd.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /** ✅ 숙소 상세에서 후기 작성 */
    @PostMapping("/accommodations/{id}/reviews")
    public String write(@PathVariable Long id,
                        @RequestParam("rating") int rating,
                        @RequestParam("content") String content,
                        HttpSession session) {

        User login = (User) session.getAttribute("loginUser");
        if (login == null) {
            return "redirect:/login";
        }

        // 게스트만 후기 작성 가능
        if (!"guest".equalsIgnoreCase(login.getRole())) {
            return "redirect:/accommodations/" + id + "?error=forbidden#reviews";
        }

        int safeRating = Math.max(1, Math.min(5, rating));
        String safeContent = (content == null) ? "" : content.trim();
        if (safeContent.isEmpty()) {
            return "redirect:/accommodations/" + id + "?error=empty#reviews";
        }

        reviewService.write(id, login, safeRating, safeContent);
        return "redirect:/accommodations/" + id + "#reviews";
    }

    /** ✅ 후기 삭제 (작성자만 가능) */
    @PostMapping("/reviews/{reviewId}/delete")
    public String delete(@PathVariable Long reviewId,
                         @RequestParam("accommodationId") Long accommodationId,
                         HttpSession session) {

        User login = (User) session.getAttribute("loginUser");
        if (login == null) {
            return "redirect:/login";
        }

        reviewService.delete(reviewId, login);
        return "redirect:/accommodations/" + accommodationId + "#reviews";
    }

    /** ✅ 내 후기 목록 (게스트 전용) */
    @GetMapping("/users/reviews")
    public String myReviews(HttpSession session, Model model) {

        User login = (User) session.getAttribute("loginUser");
        if (login == null) {
            return "redirect:/login";
        }

        if (!"guest".equalsIgnoreCase(login.getRole())) {
            return "redirect:/host/mypage";
        }

        model.addAttribute("user", login);
        model.addAttribute("reviews", reviewService.myReviews(login));
        return "review/my-reviews";
    }

    /** ✅ 호스트: 내 숙소에 달린 후기 목록 */
    @GetMapping("/host/reviews")
    public String hostReviews(HttpSession session, Model model) {

        User login = (User) session.getAttribute("loginUser");
        if (login == null) {
            return "redirect:/login";
        }

        if (!"host".equalsIgnoreCase(login.getRole())) {
            return "redirect:/users/mypage";
        }

        model.addAttribute("user", login);
        model.addAttribute("reviews", reviewService.hostReviews(login));
        return "review/host-reviews";
    }

    /** 별칭: /my/reviews → /users/reviews */
    @GetMapping("/my/reviews")
    public String myReviewsAlias(HttpSession session, Model model) {
        return myReviews(session, model);
    }

    /* ============================
       ⭐ 호스트: 후기 답변 작성
       ============================ */
    @PostMapping("/reviews/{reviewId}/reply")
    public String addHostReply(
            @PathVariable Long reviewId,
            @RequestParam("replyContent") String replyContent,
            HttpSession session
    ) {
        User login = (User) session.getAttribute("loginUser");
        if (login == null || !"host".equalsIgnoreCase(login.getRole())) {
            return "redirect:/login";
        }

        reviewService.addHostReply(reviewId, replyContent, login);

        Review review = reviewService.getById(reviewId);
        Long accId = review.getAccommodation().getId();

        return "redirect:/accommodations/" + accId + "#reviews";
    }

    /* ============================
       ⭐ 호스트: 후기 답변 삭제
       ============================ */
    @PostMapping("/reviews/{reviewId}/reply/delete")
    public String deleteHostReply(
            @PathVariable Long reviewId,
            HttpSession session
    ) {
        User login = (User) session.getAttribute("loginUser");
        if (login == null || !"host".equalsIgnoreCase(login.getRole())) {
            return "redirect:/login";
        }

        Review review = reviewService.getById(reviewId);
        Long accId = review.getAccommodation().getId();

        reviewService.deleteHostReply(reviewId, login);

        return "redirect:/accommodations/" + accId + "#reviews";
    }
}
