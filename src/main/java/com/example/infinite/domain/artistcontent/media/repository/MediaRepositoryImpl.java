package com.example.infinite.domain.artistcontent.media.repository;

import com.example.infinite.domain.artistcontent.media.entity.Media;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.query.NativeQuery;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
public class MediaRepositoryImpl implements MediaRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public List<Media> findPreviewByTargetTypeAndTargetIdInOrderByTargetIdAscSortOrderAsc(
            PostType targetType,
            Collection<Long> targetIds,
            int previewLimitPerTarget
    ) {
        if (targetIds == null || targetIds.isEmpty()) {
            return List.of();
        }

        // 목록에서는 게시글별 앞쪽 미디어 몇 장만 필요하므로
        // target_id 파티션별 row_number를 써서 per-post 상한을 DB 단계에서 자른다.
        String sql = """
                select ranked.id,
                       ranked.target_type,
                       ranked.target_id,
                       ranked.media_type,
                       ranked.storage_key,
                       ranked.file_url,
                       ranked.thumbnail_url,
                       ranked.original_file_name,
                       ranked.content_type,
                       ranked.file_size,
                       ranked.sort_order,
                       ranked.created_at,
                       ranked.updated_at,
                       ranked.deleted_at
                from (
                    select mf.*,
                           row_number() over (
                               partition by mf.target_id
                               order by mf.sort_order asc, mf.id asc
                           ) as rn
                    from media_files mf
                    where mf.target_type = :targetType
                      and mf.target_id in (:targetIds)
                ) ranked
                where ranked.rn <= :previewLimitPerTarget
                order by ranked.target_id asc, ranked.sort_order asc, ranked.id asc
                """;

        NativeQuery<Media> query = entityManager
                .createNativeQuery(sql, Media.class)
                .unwrap(NativeQuery.class);
        query.setParameter("targetType", targetType.name());
        query.setParameterList("targetIds", targetIds);
        query.setParameter("previewLimitPerTarget", previewLimitPerTarget);
        return query.getResultList();
    }
}
