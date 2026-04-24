package com.example.infinite.domain.dm.service;

import com.example.infinite.domain.dm.entity.DmMessage;
import com.example.infinite.domain.dm.entity.DmRoom;
import com.example.infinite.domain.dm.enums.SenderType;
import com.example.infinite.domain.dm.error.DmErrorCode;
import com.example.infinite.domain.dm.error.DmException;
import com.example.infinite.domain.dm.repository.DmMessageRepository;
import com.example.infinite.domain.dm.repository.DmRoomRepository;
import com.example.infinite.domain.member.artist.repository.ArtistMemberRepository;
import com.example.infinite.domain.subscriptionmembership.repository.DmSubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DmService 단위 테스트")
class DmServiceTest {

    @Mock DmRoomRepository dmRoomRepository;
    @Mock DmMessageRepository dmMessageRepository;
    @Mock DmSubscriptionRepository dmSubscriptionRepository;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock ArtistMemberRepository artistMemberRepository;

    @InjectMocks DmService dmService;

    // DmRoom은 @Builder 로 필드를 주입하지만 id는 @GeneratedValue 라서 리플렉션으로 세팅
    private DmRoom createTestRoom(Long roomId, Long userId, Long artistId) {
        DmRoom room = DmRoom.builder()
                .userId(userId)
                .artistId(artistId)
                .build();
        ReflectionTestUtils.setField(room, "id", roomId);
        return room;
    }

    @Nested
    @DisplayName("broadcast()")
    class BroadcastTest {

        @Test
        @DisplayName("아티스트 소속 멤버가 아닌 사용자의 broadcast는 DM_BROADCAST_UNAUTHORIZED 예외가 발생한다")
        void broadcast_byNonArtistMember_throwsUnauthorized() {
            Long artistId = 10L;
            Long nonMemberId = 999L;

            when(artistMemberRepository.existsByArtistIdAndMemberId(artistId, nonMemberId))
                    .thenReturn(false);

            DmException ex = catchThrowableOfType(
                    () -> dmService.broadcast(artistId, nonMemberId, "전체 공지"),
                    DmException.class);
            assertThat(ex.getErrorCode()).isEqualTo(DmErrorCode.DM_BROADCAST_UNAUTHORIZED);

            verify(dmMessageRepository, never()).save(any());
            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }
    }

    @Nested
    @DisplayName("sendMessage()")
    class SendMessageTest {

        @Test
        @DisplayName("아티스트 소속 멤버가 DM을 보내면 SenderType.ARTIST로 저장된다")
        void sendMessage_byArtistMember_setSenderTypeArtist() {
            Long roomId = 1L;
            Long artistId = 10L;
            Long artistMemberId = 100L;

            DmRoom room = createTestRoom(roomId, 200L, artistId);

            when(dmRoomRepository.findById(roomId)).thenReturn(Optional.of(room));
            when(artistMemberRepository.existsByArtistIdAndMemberId(artistId, artistMemberId))
                    .thenReturn(true);
            when(dmMessageRepository.save(any(DmMessage.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            dmService.sendMessage(roomId, artistMemberId, "안녕하세요 팬 여러분");

            ArgumentCaptor<DmMessage> captor = ArgumentCaptor.forClass(DmMessage.class);
            verify(dmMessageRepository).save(captor.capture());

            DmMessage saved = captor.getValue();
            assertThat(saved.getSenderType()).isEqualTo(SenderType.ARTIST);
        }
    }
}
