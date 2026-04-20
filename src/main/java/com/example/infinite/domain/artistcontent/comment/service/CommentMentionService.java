package com.example.infinite.domain.artistcontent.comment.service;

import com.example.infinite.domain.artistcontent.comment.dto.response.CommentMentionResponse;
import com.example.infinite.domain.artistcontent.comment.entity.Comment;
import com.example.infinite.domain.artistcontent.comment.entity.CommentMention;
import com.example.infinite.domain.artistcontent.comment.repository.CommentMentionRepository;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentMentionService {

    private final CommentMentionRepository commentMentionRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public CommentMentionResponse syncMention(Comment comment, String normalizedNickname) {
        // 댓글 수정까지 고려해 항상 "기존 삭제 -> 새 본문 기준 재생성" 순서를 유지한다.
        commentMentionRepository.deleteAllByComment_Id(comment.getId());

        if (normalizedNickname == null || normalizedNickname.isBlank()) {
            return null;
        }

        // 현재 정책상 대댓글 멘션은 최대 1명만 허용하므로 닉네임 하나만 실제 멤버로 해석한다.
        Member mentionedMember = memberRepository.findByNormalizedNickname(normalizedNickname)
                .orElse(null);
        if (mentionedMember == null) {
            return null;
        }

        CommentMention mention = commentMentionRepository.save(CommentMention.create(comment, mentionedMember));
        return CommentMentionResponse.from(mention);
    }

    public Map<Long, CommentMentionResponse> findMentionResponsesByCommentIds(Collection<Long> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) {
            return Map.of();
        }

        // 댓글 id 묶음으로 멘션을 배치 조회해 목록/상세 응답 조립 시 추가 쿼리를 막는다.
        List<CommentMention> mentions = commentMentionRepository.findByComment_IdInOrderByComment_IdAscIdAsc(commentIds);
        Map<Long, CommentMentionResponse> mentionsByCommentId = new LinkedHashMap<>();

        for (CommentMention mention : mentions) {
            mentionsByCommentId.put(mention.getComment().getId(), CommentMentionResponse.from(mention));
        }
        return mentionsByCommentId;
    }
}
