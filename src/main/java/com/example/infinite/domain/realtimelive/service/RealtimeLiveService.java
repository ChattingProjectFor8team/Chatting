package com.example.infinite.domain.realtimelive.service;

import com.example.infinite.domain.realtimelive.dto.request.LiveCreateRequest;
import com.example.infinite.domain.realtimelive.dto.request.LiveReplayPublishRequest;
import com.example.infinite.domain.realtimelive.dto.response.LiveChatMessageResponse;
import com.example.infinite.domain.realtimelive.dto.response.LiveResponse;
import com.example.infinite.domain.realtimelive.dto.response.LiveVodResponse;
import com.example.infinite.domain.realtimelive.entity.LiveChatMessage;
import com.example.infinite.domain.realtimelive.entity.RealtimeLive;
import com.example.infinite.domain.realtimelive.enums.LiveStatus;
import com.example.infinite.domain.realtimelive.error.LiveErrorCode;
import com.example.infinite.domain.realtimelive.error.LiveException;
import com.example.infinite.domain.realtimelive.repository.LiveChatMessageRepository;
import com.example.infinite.domain.realtimelive.repository.RealtimeLiveRepository;
import com.example.infinite.domain.member.artist.entity.ArtistMember;
import com.example.infinite.domain.member.artist.repository.ArtistMemberRepository;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.enums.MemberRole;
import com.example.infinite.domain.member.member.support.MemberInputSupport;
import com.example.infinite.domain.member.member.support.MemberReader;
import com.example.infinite.global.auth.MemberDetailsImpl;
import com.example.infinite.global.common.dto.CursorSliceResponse;
import org.springframework.data.domain.PageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
// 기존 실시간 채팅 흐름은 건드리지 않고, 라이브 메타데이터와 VOD 공개 경로만 최소 확장한다.
public class RealtimeLiveService {

    private static final String LIVE_STATUS_KEY = "live:%d:status";

    private final RealtimeLiveRepository realtimeLiveRepository;
    private final LiveChatMessageRepository liveChatMessageRepository;
    private final LiveChatBroadcastService liveChatBroadcastService;
    private final LiveChatThrottleService liveChatThrottleService;
    private final StringRedisTemplate stringRedisTemplate;
    private final MemberReader memberReader;
    private final ArtistMemberRepository artistMemberRepository;

    @Transactional
    public LiveResponse createLive(MemberDetailsImpl memberDetails, Long artistId, LiveCreateRequest request) {
        // host 스냅샷을 생성 시점에 저장해 VOD 카드가 Member/ArtistMember를 다시 join하지 않아도 되게 만든다.
        LiveActor actor = resolveAuthorizedActor(memberDetails, artistId);
        RealtimeLive live = RealtimeLive.builder()
                .artistId(artistId)
                .hostMemberId(actor.memberId())
                .hostDisplayName(actor.displayName())
                .hostProfileImageUrl(actor.profileImageUrl())
                .title(request.title())
                .description(request.description())
                .thumbnailUrl(request.thumbnailUrl())
                .build();
        return LiveResponse.from(realtimeLiveRepository.save(live));
    }

    @Transactional
    public LiveResponse startLive(MemberDetailsImpl memberDetails, Long artistId, Long liveId) {
        validateManagePermission(memberDetails, artistId);
        RealtimeLive live = findLiveOrThrow(liveId);
        validateOwnership(live, artistId);

        if (live.getLiveStatus() != LiveStatus.SCHEDULED) {
            throw new LiveException(LiveErrorCode.LIVE_NOT_SCHEDULED);
        }

        live.start();

        stringRedisTemplate.opsForValue()
                .set(String.format(LIVE_STATUS_KEY, liveId), "LIVE");

        return LiveResponse.from(live);
    }

    @Transactional
    public LiveResponse endLive(MemberDetailsImpl memberDetails, Long artistId, Long liveId) {
        validateManagePermission(memberDetails, artistId);
        RealtimeLive live = findLiveOrThrow(liveId);
        validateOwnership(live, artistId);

        if (live.getLiveStatus() != LiveStatus.LIVE) {
            throw new LiveException(LiveErrorCode.LIVE_NOT_LIVE);
        }

        live.end();

        stringRedisTemplate.delete(String.format(LIVE_STATUS_KEY, liveId));

        liveChatBroadcastService.drainAndRemove(liveId);

        return LiveResponse.from(live);
    }

    @Transactional
    public LiveResponse publishReplay(
            MemberDetailsImpl memberDetails,
            Long artistId,
            Long liveId,
            LiveReplayPublishRequest request
    ) {
        validateManagePermission(memberDetails, artistId);
        RealtimeLive live = findLiveOrThrow(liveId);
        validateOwnership(live, artistId);

        if (live.getLiveStatus() != LiveStatus.ENDED && live.getLiveStatus() != LiveStatus.REPLAY_READY) {
            throw new LiveException(LiveErrorCode.LIVE_NOT_LIVE);
        }

        // 녹화본 저장 파이프라인이 아직 없으므로, 현재 단계에서는 replay URL 이 준비된 시점에
        // 운영자/아티스트가 이 API 를 호출해 VOD 공개 상태로 전환한다.
        live.markReplayReady(request.replayUrl());
        return LiveResponse.from(live);
    }

    @Transactional
    public void deleteChatMessage(MemberDetailsImpl memberDetails, Long artistId, Long liveId, Long messageId) {
        validateManagePermission(memberDetails, artistId);
        RealtimeLive live = findLiveOrThrow(liveId);
        validateOwnership(live, artistId);

        LiveChatMessage message = liveChatMessageRepository.findById(messageId)
                .orElseThrow(() -> new LiveException(LiveErrorCode.CHAT_MESSAGE_NOT_FOUND));

        if (message.getDeletedAt() != null) {
            throw new LiveException(LiveErrorCode.MESSAGE_ALREADY_DELETED);
        }

        message.softDelete();
    }

    public void muteUser(MemberDetailsImpl memberDetails, Long artistId, Long liveId, Long userId) {
        validateManagePermission(memberDetails, artistId);
        RealtimeLive live = findLiveOrThrow(liveId);
        validateOwnership(live, artistId);
        liveChatThrottleService.mute(liveId, userId);
    }

    public void unmuteUser(MemberDetailsImpl memberDetails, Long artistId, Long liveId, Long userId) {
        validateManagePermission(memberDetails, artistId);
        RealtimeLive live = findLiveOrThrow(liveId);
        validateOwnership(live, artistId);
        liveChatThrottleService.unmute(liveId, userId);
    }

    public List<LiveResponse> getLiveList(Long artistId, LiveStatus status) {
        List<RealtimeLive> lives = (status != null)
                ? realtimeLiveRepository.findByArtistIdAndLiveStatusOrderByCreatedAtDesc(artistId, status)
                : realtimeLiveRepository.findByArtistIdOrderByCreatedAtDesc(artistId);
        return lives.stream().map(LiveResponse::from).toList();
    }

    public LiveResponse getLiveDetail(Long artistId, Long liveId) {
        RealtimeLive live = findLiveOrThrow(liveId);
        if (!live.getArtistId().equals(artistId)) {
            throw new LiveException(LiveErrorCode.LIVE_NOT_FOUND);
        }
        return LiveResponse.from(live);
    }

    public CursorSliceResponse<LiveVodResponse> getVodList(Long artistId, Long cursor, Integer size) {
        int effectiveSize = Math.min(Math.max(size == null ? 12 : size, 1), 50);
        PageRequest pageRequest = PageRequest.of(0, effectiveSize + 1);

        // 종료 방송 중에서도 replayUrl 이 준비된 REPLAY_READY 상태만 라이브 탭 VOD로 노출한다.
        List<LiveVodResponse> rows = (cursor == null
                ? realtimeLiveRepository.findByArtistIdAndLiveStatusOrderByIdDesc(artistId, LiveStatus.REPLAY_READY, pageRequest)
                : realtimeLiveRepository.findByArtistIdAndLiveStatusAndIdLessThanOrderByIdDesc(
                        artistId,
                        LiveStatus.REPLAY_READY,
                        cursor,
                        pageRequest
                ))
                .stream()
                .map(LiveVodResponse::from)
                .toList();

        return CursorSliceResponse.of(rows, effectiveSize, LiveVodResponse::liveId);
    }

    public CursorSliceResponse<LiveChatMessageResponse> getChatMessages(
            Long liveId, Long cursor, int size) {
        findLiveOrThrow(liveId);

        int effectiveSize = Math.min(Math.max(size, 1), 100);

        List<LiveChatMessage> rows = liveChatMessageRepository
                .findByCursor(liveId, cursor, effectiveSize + 1);

        List<LiveChatMessageResponse> mapped = rows.stream()
                .map(LiveChatMessageResponse::from)
                .toList();

        return CursorSliceResponse.of(mapped, effectiveSize, LiveChatMessageResponse::id);
    }

    public boolean isLiveActive(Long liveId) {
        return Boolean.TRUE.equals(
                stringRedisTemplate.hasKey(String.format(LIVE_STATUS_KEY, liveId))
        );
    }

    private RealtimeLive findLiveOrThrow(Long liveId) {
        return realtimeLiveRepository.findById(liveId)
                .orElseThrow(() -> new LiveException(LiveErrorCode.LIVE_NOT_FOUND));
    }

    private void validateOwnership(RealtimeLive live, Long artistId) {
        if (!live.getArtistId().equals(artistId)) {
            throw new LiveException(LiveErrorCode.NOT_LIVE_OWNER);
        }
    }

    private void validateManagePermission(MemberDetailsImpl memberDetails, Long artistId) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        if (member.getRole() == MemberRole.SUPER_ADMIN) {
            return;
        }
        artistMemberRepository.findByArtistIdAndMemberId(artistId, member.getId())
                .orElseThrow(() -> new LiveException(LiveErrorCode.NOT_LIVE_OWNER));
    }

    private LiveActor resolveAuthorizedActor(MemberDetailsImpl memberDetails, Long artistId) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        // SecurityConfig는 SUPER_ADMIN도 live admin 경로를 열어 두고 있으므로,
        // 서비스에서도 SUPER_ADMIN은 artist member 연결 없이 통과시켜 회귀를 막는다.
        if (member.getRole() == MemberRole.SUPER_ADMIN) {
            return new LiveActor(
                    member.getId(),
                    member.getNickname(),
                    member.getProfileImageUrl()
            );
        }
        ArtistMember artistMember = artistMemberRepository.findByArtistIdAndMemberId(artistId, member.getId())
                .orElseThrow(() -> new LiveException(LiveErrorCode.NOT_LIVE_OWNER));
        return new LiveActor(
                artistMember.getMember().getId(),
                artistMember.getStageName(),
                artistMember.getProfileImageUrl()
        );
    }

    private record LiveActor(
            Long memberId,
            String displayName,
            String profileImageUrl
    ) {
    }
}
