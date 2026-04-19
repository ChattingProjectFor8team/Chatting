package com.example.infinite.domain.artistcontent.media.repository;

import com.example.infinite.domain.artistcontent.media.entity.Media;
import com.example.infinite.domain.artistcontent.post.eunms.PostType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MediaRepository extends JpaRepository<Media, Long> {
    List<Media> findByTargetTypeAndTargetIdInOrderByTargetIdAscSortOrderAsc(PostType targetType, Collection<Long> targetIds);
}
