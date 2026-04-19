package com.example.infinite.domain.artistcontent.hashtag.repository;

import com.example.infinite.domain.artistcontent.hashtag.entity.ContentHashtag;
import com.example.infinite.domain.artistcontent.post.eunms.PostType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ContentHashtagRepository extends JpaRepository<ContentHashtag, Long> {

    @EntityGraph(attributePaths = "hashtag")
    List<ContentHashtag> findByTargetTypeAndTargetIdOrderByIdAsc(PostType targetType, Long targetId);

    @EntityGraph(attributePaths = "hashtag")
    List<ContentHashtag> findByTargetTypeAndTargetIdInOrderByTargetIdAscIdAsc(PostType targetType, Collection<Long> targetIds);
}
