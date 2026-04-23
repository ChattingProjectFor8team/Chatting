package com.example.infinite.domain.artistcontent.comment.service.cache;

import com.example.infinite.domain.artistcontent.comment.dto.response.CommentResponse;
import com.example.infinite.domain.artistcontent.post.cache.PostHotData;
import com.example.infinite.domain.artistcontent.post.cache.PostHotDataCacheService;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import com.example.infinite.global.common.constant.CacheNames;
import com.example.infinite.global.common.dto.CursorSliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class CommentQueryCacheService {

    private static final long ROOT_COMMENT_COUNT_THRESHOLD = 20L;
    private static final long REPLY_COUNT_THRESHOLD = 20L;
    private static final long HEAT_THRESHOLD = 5L;
    private static final Duration HEAT_WINDOW = Duration.ofSeconds(10);
    private static final Duration INDEX_TTL = Duration.ofMinutes(10);

    private final CacheManager cacheManager;
    private final StringRedisTemplate stringRedisTemplate;
    private final PostHotDataCacheService postHotDataCacheService;

    /**
     * root comment는 조건을 보수적으로 잡는다.
     * post 전체의 commentCount도 충분히 크고, 실제 조회도 반복돼야 캐시 admission을 통과한다.
     */
    public CursorSliceResponse<CommentResponse> getRootSlice(
            PostType targetType,
            Long artistId,
            Long targetId,
            Long cursor,
            Supplier<CursorSliceResponse<CommentResponse>> loader
    ) {
        long heat = increaseHeat(buildRootHeatKey(targetType, targetId));
        PostHotData hotData = postHotDataCacheService.getPostHotData(targetType, targetId);
        boolean shouldCache = hotData.commentCount() >= ROOT_COMMENT_COUNT_THRESHOLD && heat >= HEAT_THRESHOLD;
        if (!shouldCache) {
            return loader.get();
        }

        Cache cache = cacheManager.getCache(CacheNames.COMMENT_ROOT_SLICE);
        String cacheKey = buildRootCacheKey(targetType, artistId, targetId, cursor);
        CommentRootSliceCacheEntry cached = cache == null ? null : cache.get(cacheKey, CommentRootSliceCacheEntry.class);
        if (cached != null) {
            return cached.value();
        }

        CursorSliceResponse<CommentResponse> loaded = loader.get();
        if (cache != null) {
            cache.put(cacheKey, new CommentRootSliceCacheEntry(loaded));
            registerScopeKey(buildRootScopeKey(targetType, targetId), cacheKey);
        }
        return loaded;
    }

    /**
     * replies는 특정 thread만 갑자기 뜨거워질 수 있으므로 admission을 더 공격적으로 잡는다.
     * reply 수가 많거나, 조회 heat가 높으면 캐시한다.
     */
    public List<CommentResponse> getReplies(
            PostType targetType,
            Long artistId,
            Long targetId,
            Long parentCommentId,
            long replyCount,
            Supplier<List<CommentResponse>> loader
    ) {
        long heat = increaseHeat(buildReplyHeatKey(targetType, targetId, parentCommentId));
        boolean shouldCache = replyCount >= REPLY_COUNT_THRESHOLD || heat >= HEAT_THRESHOLD;
        if (!shouldCache) {
            return loader.get();
        }

        Cache cache = cacheManager.getCache(CacheNames.COMMENT_REPLY_LIST);
        String cacheKey = buildReplyCacheKey(targetType, artistId, targetId, parentCommentId);
        CommentReplyListCacheEntry cached = cache == null ? null : cache.get(cacheKey, CommentReplyListCacheEntry.class);
        if (cached != null) {
            return cached.value();
        }

        List<CommentResponse> loaded = loader.get();
        if (cache != null) {
            cache.put(cacheKey, new CommentReplyListCacheEntry(loaded));
            registerScopeKey(buildReplyScopeKey(targetType, targetId, parentCommentId), cacheKey);
        }
        return loaded;
    }

    public void evictRootSliceScope(PostType targetType, Long targetId) {
        evictScope(CacheNames.COMMENT_ROOT_SLICE, buildRootScopeKey(targetType, targetId));
    }

    public void evictReplyScope(PostType targetType, Long targetId, Long rootCommentId) {
        evictScope(CacheNames.COMMENT_REPLY_LIST, buildReplyScopeKey(targetType, targetId, rootCommentId));
    }

    private void evictScope(String cacheName, String scopeKey) {
        Cache cache = cacheManager.getCache(cacheName);
        // 댓글 캐시는 cursor/parentCommentId마다 키가 갈라지므로,
        // scopeKey에 "이번 post/thread에서 생성된 실제 cache key 목록"을 모아두고 한 번에 비운다.
        Set<String> cacheKeys = stringRedisTemplate.opsForSet().members(scopeKey);
        if (cacheKeys != null && cache != null) {
            for (String cacheKey : cacheKeys) {
                cache.evict(cacheKey);
            }
        }
        stringRedisTemplate.delete(scopeKey);
    }

    private void registerScopeKey(String scopeKey, String cacheKey) {
        // scope set이 없으면 댓글 생성/삭제 시 어떤 세부 키를 지워야 할지 몰라 evict 범위를 좁히기 어렵다.
        stringRedisTemplate.opsForSet().add(scopeKey, cacheKey);
        stringRedisTemplate.expire(scopeKey, INDEX_TTL);
    }

    private long increaseHeat(String heatKey) {
        // admission 전용 조회 heat 카운터.
        // 10초 창에서 "실제로 반복 조회되는가"만 보고, 기간이 지나면 자동으로 식는다.
        Long heat = stringRedisTemplate.opsForValue().increment(heatKey);
        stringRedisTemplate.expire(heatKey, HEAT_WINDOW);
        return heat == null ? 1L : heat;
    }

    private String buildRootCacheKey(PostType targetType, Long artistId, Long targetId, Long cursor) {
        return targetType.name() + ":" + artistId + ":" + targetId + ":" + (cursor == null ? "first" : cursor);
    }

    private String buildReplyCacheKey(PostType targetType, Long artistId, Long targetId, Long parentCommentId) {
        return targetType.name() + ":" + artistId + ":" + targetId + ":" + parentCommentId;
    }

    private String buildRootScopeKey(PostType targetType, Long targetId) {
        return "comment:cache:index:root:" + targetType.name() + ":" + targetId;
    }

    private String buildReplyScopeKey(PostType targetType, Long targetId, Long parentCommentId) {
        return "comment:cache:index:reply:" + targetType.name() + ":" + targetId + ":" + parentCommentId;
    }

    private String buildRootHeatKey(PostType targetType, Long targetId) {
        return "comment:cache:heat:root:" + targetType.name() + ":" + targetId;
    }

    private String buildReplyHeatKey(PostType targetType, Long targetId, Long parentCommentId) {
        return "comment:cache:heat:reply:" + targetType.name() + ":" + targetId + ":" + parentCommentId;
    }
}
