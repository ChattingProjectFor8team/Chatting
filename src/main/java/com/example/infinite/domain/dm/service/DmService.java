package com.example.infinite.domain.dm.service;

import com.example.infinite.domain.dm.dto.response.DmMessageResponse;
import com.example.infinite.domain.dm.dto.response.DmRoomResponse;
import com.example.infinite.domain.dm.entity.DmMessage;
import com.example.infinite.domain.dm.entity.DmRoom;
import com.example.infinite.domain.dm.enums.SenderType;
import com.example.infinite.domain.dm.error.DmErrorCode;
import com.example.infinite.domain.dm.error.DmException;
import com.example.infinite.domain.dm.repository.DmMessageRepository;
import com.example.infinite.domain.dm.repository.DmRoomRepository;
import com.example.infinite.domain.subscriptionmembership.enums.SubscriptionStatus;
import com.example.infinite.domain.subscriptionmembership.repository.DmSubscriptionRepository;
import com.example.infinite.global.common.dto.CursorSliceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DmService {

    private static final int MAX_USER_REPLIES_BEFORE_ARTIST = 3;

    private final DmRoomRepository dmRoomRepository;
    private final DmMessageRepository dmMessageRepository;
    private final DmSubscriptionRepository dmSubscriptionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public List<DmRoomResponse> getMyRooms(Long userId) {
        return dmRoomRepository.findByUserId(userId).stream()
                .map(DmRoomResponse::from)
                .toList();
    }

    public CursorSliceResponse<DmMessageResponse> getRoomMessages(
            Long roomId, Long userId, Long cursor, int size) {
        DmRoom room = findRoomOrThrow(roomId);
        validateRoomAccess(room, userId);

        int effectiveSize = Math.min(Math.max(size, 1), 100);

        List<DmMessage> rows = dmMessageRepository
                .findByRoomIdWithCursor(roomId, cursor, effectiveSize + 1);

        List<DmMessageResponse> mapped = rows.stream()
                .map(DmMessageResponse::from)
                .toList();

        return CursorSliceResponse.of(mapped, effectiveSize, DmMessageResponse::id);
    }

    public List<DmRoomResponse> getArtistRooms(Long artistId) {
        return dmRoomRepository.findByArtistId(artistId).stream()
                .map(DmRoomResponse::from)
                .toList();
    }

    /**
     * 아티스트 전체 발송 (fan-out).
     * 해당 아티스트의 모든 DM 룸에 메시지 생성 + 각 룸 구독자에게 STOMP 전송.
     */
    @Transactional
    public void broadcast(Long artistId, String content) {
        List<DmRoom> rooms = dmRoomRepository.findByArtistId(artistId);

        for (DmRoom room : rooms) {
            DmMessage message = DmMessage.builder()
                    .dmRoom(room)
                    .senderType(SenderType.ARTIST)
                    .content(content)
                    .build();
            dmMessageRepository.save(message);

            messagingTemplate.convertAndSend(
                    "/sub/dm/" + room.getId(),
                    DmMessageResponse.from(message)
            );
        }

        log.info("아티스트 DM 일괄 발송 완료: artistId={}, rooms={}", artistId, rooms.size());
    }

    /**
     * 개별 DM 메시지 전송 (STOMP 핸들러에서 호출).
     * 유저: 구독 검증 + 답장 제한. 아티스트: 제한 없음.
     */
    @Transactional
    public void sendMessage(Long roomId, Long senderId, String content) {
        DmRoom room = findRoomOrThrow(roomId);

        SenderType senderType;
        if (room.getArtistId().equals(senderId)) {
            senderType = SenderType.ARTIST;
        } else if (room.getUserId().equals(senderId)) {
            senderType = SenderType.USER;

            if (!hasActiveSubscription(room.getUserId(), room.getArtistId())) {
                throw new DmException(DmErrorCode.DM_SUBSCRIPTION_REQUIRED);
            }

            long pendingCount = dmMessageRepository.countUserMessagesSinceLastArtistReply(roomId);
            if (pendingCount >= MAX_USER_REPLIES_BEFORE_ARTIST) {
                throw new DmException(DmErrorCode.DM_REPLY_LIMIT_EXCEEDED);
            }
        } else {
            throw new DmException(DmErrorCode.DM_NOT_ROOM_MEMBER);
        }

        DmMessage message = DmMessage.builder()
                .dmRoom(room)
                .senderType(senderType)
                .content(content)
                .build();
        dmMessageRepository.save(message);

        messagingTemplate.convertAndSend(
                "/sub/dm/" + roomId,
                DmMessageResponse.from(message)
        );
    }

    private boolean hasActiveSubscription(Long userId, Long artistId) {
        return dmSubscriptionRepository
                .findByUserIdAndArtistIdAndStatus(userId, artistId, SubscriptionStatus.ACTIVE)
                .isPresent();
    }

    private DmRoom findRoomOrThrow(Long roomId) {
        return dmRoomRepository.findById(roomId)
                .orElseThrow(() -> new DmException(DmErrorCode.DM_ROOM_NOT_FOUND));
    }

    private void validateRoomAccess(DmRoom room, Long userId) {
        if (!room.getUserId().equals(userId) && !room.getArtistId().equals(userId)) {
            throw new DmException(DmErrorCode.DM_NOT_ROOM_MEMBER);
        }
    }
}
