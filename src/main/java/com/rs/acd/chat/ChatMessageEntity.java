package com.rs.acd.chat;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message", indexes = {
        @Index(name = "idx_room_sent", columnList = "roomId,sentAt"),
        @Index(name = "idx_host", columnList = "hostId"),
        @Index(name = "idx_guest", columnList = "guestId")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessageEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 방 식별자: acc-{accommodationId}-host-{hostId}-guest-{guestId}
    @Column(nullable = false, length = 120)
    private String roomId;

    private Long accommodationId;
    private Long hostId;
    private Long guestId;

    @Column(nullable = false)
    private Long senderId;

    @Column(nullable = false, length = 60)
    private String senderName;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    // ✅ 소프트 삭제
    @Column(nullable = false, columnDefinition = "bit(1) default 0")
    private boolean deleted;
}
