package com.rs.acd.controller;

import com.rs.acd.domain.User;
import com.rs.acd.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class RegisterController {

    private final UserService userService;

    public RegisterController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String name,
                           @RequestParam String nickname,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam String role,
                           Model model) {

        if (userService.findByEmail(email) != null) {
            model.addAttribute("error", "이미 사용 중인 이메일입니다.");
            return "register";
        }

        User user = new User();
        user.setName(name);
        user.setNickname(nickname);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole(role);

        userService.save(user);
        model.addAttribute("successMessage", "회원가입이 완료되었습니다!");
        return "login";
    }
}
