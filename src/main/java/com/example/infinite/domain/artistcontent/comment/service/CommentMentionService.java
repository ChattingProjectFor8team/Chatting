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

    public Map<Long, CommentMentionResponse> findMentionResponseByCommentIds(Collection<Long> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) {
            return Map.of();
        }

        // 여러 댓글 id를 한 번에 읽되, 각 댓글은 최대 1건의 멘션만 가진다는 현재 정책을 전제로 조립한다.
        List<CommentMention> mentionRows = commentMentionRepository.findByComment_IdInOrderByComment_IdAscIdAsc(commentIds);
        Map<Long, CommentMentionResponse> mentionByCommentId = new LinkedHashMap<>();

        for (CommentMention mentionRow : mentionRows) {
            mentionByCommentId.put(mentionRow.getComment().getId(), CommentMentionResponse.from(mentionRow));
        }
        return mentionByCommentId;
    }

    @Transactional
    public void deleteMention(Long commentId) {
        commentMentionRepository.deleteAllByComment_Id(commentId);
    }
}
