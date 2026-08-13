package com.rs.acd.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository repo;
    private final ChatConversationHideRepository hideRepo;

    // ===== 저장 =====
    @Transactional
    public ChatMessageEntity save(ChatMessage msg, Long accommodationId, Long hostId, Long guestId) {
        ChatMessageEntity e = ChatMessageEntity.builder()
                .roomId(msg.getRoomId())
                .accommodationId(accommodationId)
                .hostId(hostId)
                .guestId(guestId)
                .senderId(parseLong(msg.getSenderId()))
                // 이름/본문 정규화(양끝 따옴표·기본 이스케이프 제거)
                .senderName(normalize(msg.getSenderName()))
                .content(normalize(msg.getContent()))
                .sentAt(msg.getSentAt() != null ? msg.getSentAt() : LocalDateTime.now())
                .deleted(false)
                .build();
        return repo.save(e);
    }

    // ===== 소프트 삭제 =====
    @Transactional
    public boolean softDelete(Long messageId, Long requesterId) {
        if (messageId == null || requesterId == null) return false;

        return repo.findById(messageId).map(m -> {
            boolean allowed = requesterId.equals(m.getSenderId())
                    || requesterId.equals(m.getHostId())
                    || requesterId.equals(m.getGuestId());
            if (!allowed) return false;

            if (m.isDeleted()) return true;
            m.setDeleted(true);
            repo.save(m);
            return true;
        }).orElse(false);
    }

    // ===== 히스토리 (일부) =====
    @Transactional(readOnly = true)
    public List<ChatMessage> loadHistory(String roomId, int limit) {
        int cap = (limit > 0 ? limit : 50);
        return repo.findTop50ByRoomIdOrderBySentAtDesc(roomId).stream()
                .filter(e -> !e.isDeleted())
                .limit(cap)
                .sorted(Comparator.comparing(ChatMessageEntity::getSentAt)) // 오래된→최신
                .map(this::toDto)
                .toList();
    }

    // ===== 전체 히스토리 (오래된 → 최신) =====
    @Transactional(readOnly = true)
    public List<ChatMessage> loadAllHistory(String roomId) {
        return repo.findByRoomIdOrderBySentAtAsc(roomId).stream()
                .filter(e -> !e.isDeleted())
                .map(this::toDto)
                .toList();
    }

    // ===== 목록(인박스) =====
    @Transactional(readOnly = true)
    public List<ConversationPreview> findConversations(Long userId) {
        return repo.findConversationViews(userId).stream()
                .map(v -> ConversationPreview.builder()
                        .roomId(v.getRoomId())
                        .accommodationName(normalize(v.getAccommodationName()))
                        .hostName(normalize(v.getHostName()))
                        .lastSenderName(normalize(v.getLastSenderName()))
                        .lastContent(normalize(v.getLastContent()))
                        .lastTime(v.getLastTime())
                        .build())
                .toList();
    }

    // ===== 방 헤더 =====
    @Transactional(readOnly = true)
    public RoomHeaderDto findRoomHeader(String roomId) {
        ChatMessageRepository.RoomHeader h = repo.findRoomHeader(roomId);
        if (h == null) return new RoomHeaderDto(roomId, "알 수 없는 숙소", "알 수 없는 호스트");
        return new RoomHeaderDto(
                h.getRoomId(),
                normalize(h.getAccommodationName()),
                normalize(h.getHostName())
        );
    }

    // ===== 변환 =====
    private ChatMessage toDto(ChatMessageEntity e) {
        return ChatMessage.builder()
                .id(e.getId())
                .roomId(e.getRoomId())
                .senderId(String.valueOf(e.getSenderId()))
                .senderName(normalize(e.getSenderName()))
                .content(normalize(e.getContent()))
                .type(ChatMessage.Type.TALK)
                .sentAt(e.getSentAt())
                .deleted(e.isDeleted())
                .build();
    }

    private Long parseLong(String s) {
        try {
            return Long.valueOf(s);
        } catch (Exception e) {
            return 0L;
        }
    }

    // ===== 숨김/해제 기능 =====
    @Transactional
    public void hideConversation(Long userId, String roomId) {
        if (!hideRepo.existsByRoomIdAndUserId(roomId, userId)) {
            hideRepo.save(ChatConversationHide.builder()
                    .roomId(roomId)
                    .userId(userId)
                    .hiddenAt(LocalDateTime.now())
                    .build());
        }
    }

    @Transactional
    public void unhideConversation(Long userId, String roomId) {
        hideRepo.deleteByRoomIdAndUserId(roomId, userId);
    }

    @Transactional(readOnly = true)
    public boolean isHidden(Long userId, String roomId) {
        return hideRepo.existsByRoomIdAndUserId(roomId, userId);
    }

    // ===== 텍스트 정규화 유틸 =====
    private String normalize(String s) {
        if (s == null) return null;
        s = s.trim();
        // 양끝 따옴표 제거
        if ((s.length() >= 2) &&
            ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")))) {
            s = s.substring(1, s.length() - 1);
        }
        // 기본 이스케이프 해제: \n, \r, \t, \" , \\ , 그리고 역슬래시-유-헥사 형태 처리
        return unescapeBasic(s);
    }

    private String unescapeBasic(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                switch (n) {
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case '"' -> out.append('\"');
                    case '\\' -> out.append('\\');
                    case 'u' -> {
                        // 역슬래시-유 다음에 4자리 16진수면 유니코드로 해석
                        if (i + 4 < s.length()) {
                            String hex = s.substring(i + 1, i + 5);
                            if (isHex4(hex)) {
                                out.append((char) Integer.parseInt(hex, 16));
                                i += 4;
                            } else {
                                // 잘못된 형태면 문자열 그대로 보존: "\\" + "u" + hex
                                out.append('\\').append('u').append(hex);
                                i += 4;
                            }
                        } else {
                            // 길이가 부족하면 그대로 보존: "\" + "u"
                            out.append('\\').append('u');
                        }
                    }
                    default -> out.append('\\').append(n);
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private boolean isHex4(String s) {
        if (s == null || s.length() != 4) return false;
        for (int i = 0; i < 4; i++) {
            char ch = s.charAt(i);
            boolean hex = (ch >= '0' && ch <= '9') ||
                          (ch >= 'a' && ch <= 'f') ||
                          (ch >= 'A' && ch <= 'F');
            if (!hex) return false;
        }
        return true;
    }

    // ===== DTOs =====
    @lombok.Getter
    @lombok.Setter
    @lombok.Builder
    public static class ConversationPreview {
        private String roomId;
        private String accommodationName;
        private String hostName;
        private String lastSenderName;
        private String lastContent;
        private LocalDateTime lastTime;
    }

    public record RoomHeaderDto(String roomId, String accommodationName, String hostName) {}
}
