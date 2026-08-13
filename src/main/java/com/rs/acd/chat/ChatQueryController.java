package com.rs.acd.chat;

import com.rs.acd.domain.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatQueryController {

    private final ChatMessageService service;

    @GetMapping("/history")
    public List<ChatMessage> history(@RequestParam String roomId,
                                     @RequestParam(defaultValue = "50") int limit) {
        return service.loadHistory(roomId, limit);
    }

    /** ✅ 대화 숨김(나만 보기 삭제) */
    @PostMapping("/rooms/{roomId}/hide")
    public void hideRoom(@PathVariable String roomId, HttpSession session) {
        User me = (User) session.getAttribute("loginUser");
        if (me == null) throw new RuntimeException("로그인 필요");
        service.hideConversation(me.getUserId(), roomId);
    }

    /** ✅ 대화 숨김 해제(필요 시) */
    @DeleteMapping("/rooms/{roomId}/hide")
    public void unhideRoom(@PathVariable String roomId, HttpSession session) {
        User me = (User) session.getAttribute("loginUser");
        if (me == null) throw new RuntimeException("로그인 필요");
        service.unhideConversation(me.getUserId(), roomId);
    }
}

/* ===== 화면 컨트롤러(변경 없음, 참고용) ===== */

@Controller
@RequiredArgsConstructor
class ChatViewController {

    private final ChatMessageService service;

    @GetMapping("/chat/messages")
    public String inbox(HttpSession session, Model model) {
        User me = (User) session.getAttribute("loginUser");
        if (me == null) return "redirect:/login";

        Long meId = me.getUserId();
        model.addAttribute("conversations", service.findConversations(meId));
        model.addAttribute("meId", meId);
        return "chat/inbox";
    }

    @GetMapping("/chat")
    public String chatPage(@RequestParam String roomId,
                           HttpSession session,
                           Model model) {
        User me = (User) session.getAttribute("loginUser");
        if (me == null) return "redirect:/login";

        ChatMessageService.RoomHeaderDto header = service.findRoomHeader(roomId);

        model.addAttribute("roomId", roomId);
        model.addAttribute("meId", me.getUserId());
        model.addAttribute("meName", me.getNickname());
        model.addAttribute("header", header);
        model.addAttribute("history", service.loadAllHistory(roomId));
        model.addAttribute("hidden", service.isHidden(me.getUserId(), roomId)); // 필요 시 사용
        return "chat/chat";
    }
}
