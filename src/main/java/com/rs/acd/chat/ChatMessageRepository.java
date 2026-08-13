package com.rs.acd.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    List<ChatMessageEntity> findTop50ByRoomIdOrderBySentAtDesc(String roomId);

    @Query(value = """
        SELECT cm.room_id
        FROM chat_message cm
        WHERE (cm.host_id = :userId OR cm.guest_id = :userId)
        GROUP BY cm.room_id
        ORDER BY MAX(cm.sent_at) DESC
        """, nativeQuery = true)
    List<String> findRoomsForUserOrderByLatest(@Param("userId") Long userId);

    /* ✅ 인박스 목록: 내가 숨긴 방 제외 + 삭제/시스템 메시지 제외하여 정렬/미리보기 */
    @Query(value = """
        WITH rooms AS (
            SELECT DISTINCT cm.room_id
            FROM chat_message cm
            WHERE cm.host_id = :userId OR cm.guest_id = :userId
        ),
        /* 정렬 기준: 삭제/시스템 메시지 제외한 가장 최근 시간 */
        any_last AS (
            SELECT room_id, MAX(sent_at) AS any_last_time
            FROM chat_message
            WHERE deleted = 0
              AND content NOT LIKE '%입장%'
              AND content NOT LIKE '%퇴장%'
            GROUP BY room_id
        ),
        /* 미리보기용 최신 대화 메시지(삭제/시스템 제외) */
        talk_last AS (
            SELECT cm.room_id, cm.sender_name, cm.content, cm.sent_at
            FROM chat_message cm
            JOIN (
                SELECT room_id, MAX(sent_at) AS last_time
                FROM chat_message
                WHERE deleted = 0
                  AND content NOT LIKE '%입장%'
                  AND content NOT LIKE '%퇴장%'
                GROUP BY room_id
            ) t
              ON t.room_id = cm.room_id
             AND t.last_time = cm.sent_at
        )
        SELECT
            r.room_id            AS roomId,
            cmx.accommodation_id AS accommodationId,
            a.name               AS accommodationName,
            cmx.host_id          AS hostId,
            u.nickname           AS hostName,
            cmx.guest_id         AS guestId,
            tl.sender_name       AS lastSenderName,
            tl.content           AS lastContent,
            tl.sent_at           AS lastTime
        FROM rooms r
        LEFT JOIN any_last al
               ON al.room_id = r.room_id
        LEFT JOIN chat_message cmx
               ON cmx.room_id = r.room_id
              AND cmx.sent_at = al.any_last_time
        LEFT JOIN talk_last tl
               ON tl.room_id = r.room_id
        LEFT JOIN accommodation a
               ON a.id = cmx.accommodation_id
        LEFT JOIN user u
               ON u.user_id = cmx.host_id
        WHERE NOT EXISTS (
            SELECT 1
            FROM chat_conversation_hide h
            WHERE h.room_id = r.room_id
              AND h.user_id = :userId
        )
        ORDER BY COALESCE(tl.sent_at, al.any_last_time) DESC
        """, nativeQuery = true)
    List<ConversationView> findConversationViews(@Param("userId") Long userId);

    interface ConversationView {
        String getRoomId();
        Long getAccommodationId();
        String getAccommodationName();
        Long getHostId();
        String getHostName();
        Long getGuestId();
        String getLastSenderName();
        String getLastContent();
        LocalDateTime getLastTime();
    }

    @Query(value = """
        SELECT
            cm.room_id            AS roomId,
            a.name                AS accommodationName,
            u.nickname            AS hostName
        FROM chat_message cm
        LEFT JOIN accommodation a ON a.id = cm.accommodation_id
        LEFT JOIN user          u ON u.user_id = cm.host_id
        WHERE cm.room_id = :roomId
        ORDER BY cm.sent_at DESC
        LIMIT 1
        """, nativeQuery = true)
    RoomHeader findRoomHeader(@Param("roomId") String roomId);

    interface RoomHeader {
        String getRoomId();
        String getAccommodationName();
        String getHostName();
    }

    List<ChatMessageEntity> findByRoomIdOrderBySentAtAsc(String roomId);
}
