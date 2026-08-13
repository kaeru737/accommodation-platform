package com.rs.acd.controller;

import com.rs.acd.domain.BlockedDate;
import com.rs.acd.domain.User;
import com.rs.acd.service.BlockedDateService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/host/accommodations/{accommodationId}/blocked-dates")
public class BlockedDateController {

    private final BlockedDateService blockedDateService;

    public BlockedDateController(BlockedDateService blockedDateService) {
        this.blockedDateService = blockedDateService;
    }

    @GetMapping
    public String showBlockedDates(@PathVariable Long accommodationId,
                                   HttpSession session,
                                   Model model) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        List<BlockedDate> blockedDates =
                blockedDateService.getBlockedDates(accommodationId, loginUser);

        model.addAttribute("blockedDates", blockedDates);
        model.addAttribute("accommodationId", accommodationId);
        return "reservation/blocked-dates";
    }

    @PostMapping
    public String addBlockedDate(@PathVariable Long accommodationId,
                                 @RequestParam("date") String dateStr,
                                 HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        LocalDate date = LocalDate.parse(dateStr); // yyyy-MM-dd 형식 가정
        blockedDateService.addBlockedDate(accommodationId, date, loginUser);

        return "redirect:/host/accommodations/" + accommodationId + "/blocked-dates";
    }

    @PostMapping("/{blockedDateId}/delete")
    public String deleteBlockedDate(@PathVariable Long accommodationId,
                                    @PathVariable Long blockedDateId,
                                    HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return "redirect:/login";
        }

        blockedDateService.deleteBlockedDate(blockedDateId, loginUser);
        return "redirect:/host/accommodations/" + accommodationId + "/blocked-dates";
    }
}
