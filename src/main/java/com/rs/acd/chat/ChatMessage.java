package com.rs.acd.chat;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    public enum Type { ENTER, TALK, LEAVE, DELETED }  // ✅ 삭제 이벤트 추가

    private Long id;              // ✅ 메시지 pk (브로드캐스트에 포함)
    private String roomId;
    private String senderId;
    private String senderName;
    private String content;
    private Type type;
    private LocalDateTime sentAt;

    private Boolean deleted;      // ✅ 소프트 삭제 플래그(브로드캐스트 용)
}
