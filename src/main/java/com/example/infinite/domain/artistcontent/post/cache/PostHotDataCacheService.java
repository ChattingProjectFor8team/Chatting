package com.example.infinite.domain.artistcontent.post.cache;

import com.example.infinite.domain.artistcontent.post.enums.PostType;
import com.example.infinite.global.common.constant.CacheNames;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PostHotDataCacheService {

    private final CacheManager cacheManager;
    private final PostHotDataLoaderService postHotDataLoaderService;

    /**
     * 단건 hot data 조회도 내부적으로는 공통 배치 로직을 재사용한다.
     * 이렇게 하면 detail/list가 같은 캐시 키 규약을 공유하게 된다.
     */
    public PostHotData getPostHotData(PostType postType, Long postId) {
        return getPostHotDataMap(postType, List.of(postId))
                .getOrDefault(postId, PostHotData.empty());
    }

    /**
     * hot data 캐시는 "post 하나당 key 하나"로 운영한다.
     *
     * 이유:
     * - detail과 list가 같은 count 캐시를 재사용할 수 있고
     * - 어떤 slice에서 조회됐는지와 무관하게 동일 post의 count는 한 키로 합쳐진다
     */
    public Map<Long, PostHotData> getPostHotDataMap(PostType postType, Collection<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }

        Cache cache = cacheManager.getCache(CacheNames.POST_HOT_DATA);
        List<Long> distinctPostIds = new ArrayList<>(new LinkedHashSet<>(postIds));
        Map<Long, PostHotData> result = new LinkedHashMap<>();
        List<Long> missedPostIds = new ArrayList<>();

        for (Long postId : distinctPostIds) {
            PostHotData cachedHotData = cache == null ? null : cache.get(buildCacheKey(postType, postId), PostHotData.class);
            if (cachedHotData != null) {
                result.put(postId, cachedHotData);
                continue;
            }
            missedPostIds.add(postId);
        }

        if (missedPostIds.isEmpty()) {
            return result;
        }

        // miss 난 id만 모아 한 번에 원본 테이블을 읽고, 다시 post별 캐시로 채운다.
        Map<Long, PostHotData> loadedHotData = postHotDataLoaderService.load(postType, missedPostIds);
        for (Long postId : missedPostIds) {
            PostHotData hotData = loadedHotData.getOrDefault(postId, PostHotData.empty());
            if (cache != null) {
                cache.put(buildCacheKey(postType, postId), hotData);
            }
            result.put(postId, hotData);
        }

        return result;
    }

    private String buildCacheKey(PostType postType, Long postId) {
        return postType.name() + ":" + postId;
    }
}
