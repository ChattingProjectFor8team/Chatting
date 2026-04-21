package com.example.infinite.domain.artistcontent.comment.repository;

import com.example.infinite.domain.artistcontent.comment.entity.Comment;
import com.example.infinite.domain.artistcontent.comment.entity.QComment;
import com.example.infinite.domain.artistcontent.post.eunms.PostType;
import com.example.infinite.domain.member.member.entity.QMember;
import com.example.infinite.global.common.util.querydsl.CursorSliceUtils;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.querydsl.core.group.GroupBy.groupBy;

@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentRepositoryCustom {

    protected final JPAQueryFactory queryFactory;

    @Override
    public List<Comment> findRootSliceByTargetTypeAndTargetId(PostType targetType, Long targetId, Long cursor, int limit) {
        QComment comment = QComment.comment;
        QMember member = QMember.member;

        var query = queryFactory
                .selectFrom(comment)
                .join(comment.writer, member).fetchJoin()
                .where(
                        comment.targetType.eq(targetType),
                        comment.targetId.eq(targetId),
                        comment.parent.isNull(),
                        CursorSliceUtils.ltCursor(comment.id, cursor)
                );

        return query
                // 댓글 무한 스크롤도 최신순 정책을 맞추기 위해 id DESC 커서를 사용한다.
                .orderBy(CursorSliceUtils.orderByIdDesc(comment.id))
                .limit(limit)
                .fetch();
    }

    @Override
    public List<Comment> findRepliesByParentId(Long parentId, int limit) {
        if (parentId == null) {
            return List.of();
        }

        QComment comment = QComment.comment;
        QMember member = QMember.member;

        return queryFactory
                .selectFrom(comment)
                .join(comment.writer, member).fetchJoin()
                .where(comment.parent.id.eq(parentId))
                // 같은 스레드의 대댓글은 오래된 순으로 읽히게 둔다.
                .orderBy(comment.id.asc())
                .limit(limit)
                .fetch();
    }

    @Override
    public Map<Long, Integer> findReplyCountsByParentIds(Collection<Long> parentIds) {
        if (parentIds == null || parentIds.isEmpty()) {
            return Map.of();
        }

        QComment comment = QComment.comment;
        QComment parent = new QComment("parent");

        // 부모 댓글 여러 개의 대댓글 개수를 한 번에 계산해 목록 응답의 replyCount 조립에 재사용한다.
        return queryFactory
                .from(comment)
                .join(comment.parent, parent)
                .where(parent.id.in(parentIds))
                .transform(groupBy(parent.id).as(comment.id.count().intValue()));
    }

    @Override
    public List<Comment> findThreadCommentsByRootCommentId(PostType targetType, Long targetId, Long rootCommentId) {
        QComment comment = QComment.comment;
        QComment parent = new QComment("parent");
        QMember member = QMember.member;

        return queryFactory
                .selectFrom(comment)
                .leftJoin(comment.parent, parent).fetchJoin()
                .join(comment.writer, member).fetchJoin()
                .where(
                        comment.targetType.eq(targetType),
                        comment.targetId.eq(targetId),
                        comment.id.eq(rootCommentId).or(comment.parent.id.eq(rootCommentId))
                )
                .orderBy(comment.id.asc())
                .fetch();
    }
}
