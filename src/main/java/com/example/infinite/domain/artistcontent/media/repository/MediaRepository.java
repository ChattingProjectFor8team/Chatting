package com.example.infinite.domain.artistcontent.media.repository;

import com.example.infinite.domain.artistcontent.media.entity.Media;
import com.example.infinite.domain.artistcontent.post.eunms.PostType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

// Media 엔티티 조회는 대부분 "어느 게시글의 첨부 목록인가" 기준으로 일어난다.
// 그래서 targetType + targetId 조건과 sortOrder 정렬을 기본 조회 패턴으로 둔다.
public interface MediaRepository extends JpaRepository<Media, Long>, MediaRepositoryCustom {
    // 목록 화면처럼 여러 게시글의 미디어를 한 번에 배치 조회할 때 사용한다.
    // targetId ASC -> sortOrder ASC 로 정렬해 서비스에서 게시글별로 그룹핑하기 쉽게 맞춘다.
    List<Media> findByTargetTypeAndTargetIdInOrderByTargetIdAscSortOrderAsc(PostType targetType, Collection<Long> targetIds);

    // 단일 게시글 상세/삭제/교체 시 현재 연결된 첨부를 순서대로 가져올 때 사용한다.
    List<Media> findByTargetTypeAndTargetIdOrderBySortOrderAsc(PostType targetType, Long targetId);
}
