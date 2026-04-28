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
            PostHotData cachedHotData = cache == null ? null : readCachedHotData(cache, buildCacheKey(postType, postId));
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

    private PostHotData readCachedHotData(Cache cache, String cacheKey) {
        // mixed read/write 테스트에서 실제로 터졌던 문제:
        // Redis cache에서 읽은 값이 PostHotData record가 아니라 LinkedHashMap으로 돌아왔다.
        //
        // 원인:
        // - JSON 직렬화 캐시는 serializer 설정에 따라 타입 메타데이터가 빠질 수 있다
        // - 그러면 Spring Cache가 "원래 DTO" 대신 Map 형태로 역직렬화해 버린다
        //
        // 그래서 여기서는 "정상 타입이면 그대로 사용, Map이면 수동 복원"의 두 경로를 모두 허용한다.
        // 이렇게 해 두면 serializer 변경 전후나 기존 캐시 잔존 데이터에도 읽기 코드가 덜 깨진다.
        Cache.ValueWrapper valueWrapper = cache.get(cacheKey);
        if (valueWrapper == null) {
            return null;
        }
        Object cachedValue = valueWrapper.get();
        if (cachedValue == null) {
            return null;
        }
        if (cachedValue instanceof PostHotData hotData) {
            return hotData;
        }
        if (cachedValue instanceof Map<?, ?> valueMap) {
            return PostHotData.of(
                    toLong(valueMap.get("likeCount")),
                    toLong(valueMap.get("commentCount"))
            );
        }
        // 여기까지 왔다는 것은 "우리가 예상한 캐시 shape가 아니다"라는 뜻이므로
        // 조용히 삼키지 말고 즉시 실패시켜 원인 파악이 가능하게 한다.
        throw new IllegalStateException(
                "Cached value is not of required type [PostHotData]: " + cachedValue
        );
    }

    private Long toLong(Object value) {
        // Redis JSON 숫자는 Long, Integer, String 등 여러 모습으로 들어올 수 있어
        // 복원 코드는 느슨하게 받아 주는 편이 안전하다.
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }
}
