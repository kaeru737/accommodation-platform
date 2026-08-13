package com.rs.acd.chat;

import com.rs.acd.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService chatService;

    /** 클라이언트: /app/chat.send
     *  서버 브로드캐스트: /topic/chat/{roomId}
     */
    @MessageMapping("/chat.send")
    public void send(ChatMessage message) {
        if (message.getSentAt() == null) {
            message.setSentAt(LocalDateTime.now());
        }
        if (message.getType() == null) {
            message.setType(ChatMessage.Type.TALK);
        }

        // roomId 예) acc-12-host-3-guest-45
        ParsedRoom parsed = ParsedRoom.parse(message.getRoomId());

        // DB 저장 (id/삭제여부 세팅해서 되돌려 보냄)
        var saved = chatService.save(message, parsed.accommodationId, parsed.hostId, parsed.guestId);
        message.setId(saved.getId());     // ChatMessage DTO에 Long id 필수
        message.setDeleted(false);        // ChatMessage DTO에 Boolean deleted 필수

        // 브로드캐스트
        messagingTemplate.convertAndSend("/topic/chat/" + message.getRoomId(), message);
    }

    /** 메시지 소프트 삭제
     *  클라이언트 → /app/chat.delete 로 {roomId, messageId, requesterId} 전송
     *  성공 시 type=DELETED 이벤트를 같은 room topic으로 브로드캐스트
     */
    @MessageMapping("/chat.delete")
    public void delete(DeleteCommand cmd) {
        Long requester = toLong(cmd.requesterId);
        boolean ok = chatService.softDelete(cmd.messageId, requester); // ChatMessageService에 동일 시그니처 존재

        if (ok) {
            ChatMessage event = ChatMessage.builder()
                    .id(cmd.messageId)                        // 어떤 메시지가 삭제됐는지 알리기
                    .roomId(cmd.roomId)
                    .type(ChatMessage.Type.DELETED)           // enum에 DELETED 포함
                    .sentAt(LocalDateTime.now())
                    .deleted(true)
                    .build();

            messagingTemplate.convertAndSend("/topic/chat/" + cmd.roomId, event);
        }
    }

    // =========================
    // ===== 인박스 숨김 API ====
    // =========================

    /** 인박스에서 '내 목록에서 삭제' (숨김) */
    @PostMapping("/chat/inbox/hide/{roomId}")
    @ResponseBody
    public ResponseEntity<?> hideRoom(@PathVariable String roomId, HttpSession session) {
        User me = (User) session.getAttribute("loginUser");
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        chatService.hideConversation(me.getUserId(), roomId);
        return ResponseEntity.ok().build();
    }

    /** 인박스 '숨김 해제' (선택 기능) */
    @PostMapping("/chat/inbox/unhide/{roomId}")
    @ResponseBody
    public ResponseEntity<?> unhideRoom(@PathVariable String roomId, HttpSession session) {
        User me = (User) session.getAttribute("loginUser");
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        chatService.unhideConversation(me.getUserId(), roomId);
        return ResponseEntity.ok().build();
    }

    // --- helper DTO/유틸 ---

    public record DeleteCommand(String roomId, Long messageId, String requesterId) {}

    private Long toLong(String s) {
        try { return Long.valueOf(s); } catch (Exception e) { return 0L; }
    }

    // 간단 roomId 파서
    static class ParsedRoom {
        Long accommodationId, hostId, guestId;

        static ParsedRoom parse(String roomId) {
            ParsedRoom p = new ParsedRoom();
            try {
                String[] parts = roomId.split("-");
                p.accommodationId = Long.valueOf(parts[1]);
                p.hostId = Long.valueOf(parts[3]);
                p.guestId = Long.valueOf(parts[5]);
            } catch (Exception e) {
                p.accommodationId = 0L;
                p.hostId = 0L;
                p.guestId = 0L;
            }
            return p;
        }
    }
}
