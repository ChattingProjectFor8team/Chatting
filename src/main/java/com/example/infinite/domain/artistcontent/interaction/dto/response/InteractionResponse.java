package com.example.infinite.domain.artistcontent.interaction.dto.response;

public record InteractionResponse(
        Long targetId,
        boolean reacted,
        long reactionCount
) {
    public static InteractionResponse of(Long targetId, boolean reacted, long reactionCount) {
        return new InteractionResponse(targetId, reacted, reactionCount);
    }
}
