package com.example.infinite.domain.dm.controller;

import com.example.infinite.domain.dm.service.DmService;
import com.example.infinite.global.auth.MemberDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DmMessageHandler {

    private final DmService dmService;

    @MessageMapping("/dm/{roomId}")
    public void handleDmMessage(
            @DestinationVariable Long roomId,
            @Payload String content,
            Principal principal) {

        Long senderId = extractMemberId(principal);
        if (senderId == null) {
            log.warn("인증 정보 없는 DM 메시지 무시: roomId={}", roomId);
            return;
        }

        try {
            dmService.sendMessage(roomId, senderId, content);
        } catch (Exception e) {
            log.warn("DM 메시지 전송 실패: roomId={}, senderId={}, error={}",
                    roomId, senderId, e.getMessage());
        }
    }

    @MessageMapping("/dm/broadcast/{artistId}")
    public void handleBroadcast(
            @DestinationVariable Long artistId,
            @Payload String content,
            Principal principal) {

        Long senderId = extractMemberId(principal);
        if (senderId == null) {
            log.warn("인증 정보 없는 broadcast 무시: artistId={}", artistId);
            return;
        }

        try {
            dmService.broadcast(artistId, content);
        } catch (Exception e) {
            log.warn("DM broadcast 실패: artistId={}, senderId={}, error={}",
                    artistId, senderId, e.getMessage());
        }
    }

    private Long extractMemberId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth
                && auth.getPrincipal() instanceof MemberDetailsImpl memberDetails) {
            return memberDetails.getMemberId();
        }
        return null;
    }
}
