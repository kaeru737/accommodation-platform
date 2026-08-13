package com.rs.acd.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatConversationHideRepository
        extends JpaRepository<ChatConversationHide, Long> {

    boolean existsByRoomIdAndUserId(String roomId, Long userId);

    Optional<ChatConversationHide> findByRoomIdAndUserId(String roomId, Long userId);

    void deleteByRoomIdAndUserId(String roomId, Long userId);
}
