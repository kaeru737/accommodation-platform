package com.rs.acd.chat;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_conversation_hide",
       indexes = {
         @Index(name = "idx_hide_user", columnList = "userId"),
         @Index(name = "idx_hide_room", columnList = "roomId")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatConversationHide {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String roomId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDateTime hiddenAt;
}
