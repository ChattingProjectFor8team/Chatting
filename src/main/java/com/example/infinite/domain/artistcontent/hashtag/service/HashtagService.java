package com.example.infinite.domain.artistcontent.hashtag.service;

import com.example.infinite.domain.artistcontent.hashtag.dto.response.HashtagResponse;
import com.example.infinite.domain.artistcontent.hashtag.entity.ContentHashtag;
import com.example.infinite.domain.artistcontent.hashtag.entity.Hashtag;
import com.example.infinite.domain.artistcontent.hashtag.error.HashtagErrorCode;
import com.example.infinite.domain.artistcontent.hashtag.error.HashtagException;
import com.example.infinite.domain.artistcontent.hashtag.repository.ContentHashtagRepository;
import com.example.infinite.domain.artistcontent.hashtag.repository.HashtagRepository;
import com.example.infinite.domain.artistcontent.hashtag.support.HashtagParser;
import com.example.infinite.domain.artistcontent.post.eunms.PostType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 해시태그 master / mapping 테이블을 동기화하는 서비스다.
 *
 * <p>핵심 흐름은 reference 프로젝트와 동일하다.
 * 1. 파서로 본문에서 태그 추출
 * 2. 기존 매핑 제거
 * 3. 새 본문 기준 매핑 재생성
 * 4. usageCount 증감 반영
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HashtagService {

    private static final int DEFAULT_SUGGESTION_LIMIT = 10;
    private static final int MAX_SUGGESTION_LIMIT = 20;

    private final HashtagRepository hashtagRepository;
    private final ContentHashtagRepository contentHashtagRepository;

    @Transactional
    public void syncHashtags(PostType targetType, Long targetId, String content) {
        // 작성/수정 시에는 항상 "기존 연결 제거 -> 새 본문 기준 재생성" 순서를 유지한다.
        List<String> normalizedTags = HashtagParser.extractNormalizedUniqueTags(content);
        // 기존 매핑을 먼저 읽어 와야 usageCount 감소와 orphan 태그 삭제 판단을 정확히 할 수 있다.
        List<ContentHashtag> existingLinks = contentHashtagRepository.findByTargetTypeAndTargetIdOrderByIdAsc(targetType, targetId);
        // 이번 대상 콘텐츠에서 제거되는 기존 태그만 별도로 잡아두면, 나중에 usageCount 0 hard delete 판단이 충분하다.
        List<Hashtag> previousHashtags = existingLinks.stream()
                .map(ContentHashtag::getHashtag)
                .toList();
        for (Hashtag previousHashtag : previousHashtags) {
            previousHashtag.decrementUsageCount();
        }

        if (!existingLinks.isEmpty()) {
            // 컬렉션 orphan 제거에 기대지 않고 매핑 행을 명시적으로 비운다.
            contentHashtagRepository.deleteAllInBatch(existingLinks);
        }

        for (String normalizedTag : normalizedTags) {
            // 정규화된 이름으로 기존 master를 재사용하고, 없으면 새로 만든다.
            Hashtag hashtag = hashtagRepository.findByName(normalizedTag)
                    .orElseGet(() -> hashtagRepository.save(Hashtag.create(normalizedTag)));

            // usageCount와 mapping row는 항상 같은 트랜잭션에서 같이 움직여야 일관성이 유지된다.
            hashtag.incrementUsageCount();
            contentHashtagRepository.save(ContentHashtag.create(targetType, targetId, hashtag));
        }

        // 이번 sync에서 기존에만 있던 태그들 중 usageCount가 0이 되면 보존 가치가 없으므로 hard delete 한다.
        List<Hashtag> removableHashtags = previousHashtags.stream()
                .filter(hashtag -> hashtag.getUsageCount() == 0L)
                .distinct()
                .toList();

        if (!removableHashtags.isEmpty()) {
            hashtagRepository.deleteAllInBatch(removableHashtags);
        }
    }

    public Map<Long, List<String>> findHashtagNamesByTargetIds(PostType targetType, Collection<Long> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) {
            return Map.of();
        }
        // targetId 묶음 IN 조회 한 번으로 여러 게시글의 해시태그 이름을 배치 로딩한다.
        List<ContentHashtag> contentHashtags = contentHashtagRepository.findByTargetTypeAndTargetIdInOrderByTargetIdAscIdAsc(
                targetType,
                targetIds
        );
        // 서비스 응답 조립에서 바로 쓸 수 있도록 targetId -> hashtagNames 구조로 메모리 그룹핑한다.
        Map<Long, List<ContentHashtag>> groupedByTargetId = new LinkedHashMap<>();
        for (ContentHashtag contentHashtag : contentHashtags) {
            groupedByTargetId.computeIfAbsent(contentHashtag.getTargetId(), key -> new java.util.ArrayList<>())
                    .add(contentHashtag);
        }
        Map<Long, List<String>> result = new LinkedHashMap<>();
        for (Long targetId : targetIds) {
            // 입력 순서를 유지해 feed/detail 응답 조립 순서와 어긋나지 않게 한다.
            List<String> hashtagNames = groupedByTargetId.getOrDefault(targetId, List.of()).stream()
                    .map(contentHashtag -> contentHashtag.getHashtag().getName())
                    .toList();
            result.put(targetId, hashtagNames);
        }
        return result;
    }

    public List<HashtagResponse> getSuggestions(String keyword, Integer limit) {
        int safeLimit = normalizeLimit(limit);
        String normalizedKeyword = normalizeSearchToken(keyword);

        List<Hashtag> hashtags = normalizedKeyword == null
                // 입력이 비어 있으면 자동완성 실패 대신 현재 인기 태그 상위 N개를 내려 UX를 자연스럽게 맞춘다.
                ? hashtagRepository.findAllByOrderByUsageCountDescNameAsc(PageRequest.of(0, safeLimit))
                : hashtagRepository.findByNameContainingOrderByUsageCountDescNameAsc(
                        normalizedKeyword,
                        PageRequest.of(0, safeLimit)
                );

        return hashtags.stream()
                .map(hashtag -> new HashtagResponse(hashtag.getName(), hashtag.getUsageCount()))
                .toList();
    }

    public String normalizeSearchToken(String tagName) {
        // 추천 검색어도 저장 키와 동일한 규칙으로 정규화해야 like 검색이 일관된다.
        String normalizedTagName = HashtagParser.normalizeHashtagName(tagName);
        return normalizedTagName.isBlank() ? null : normalizedTagName;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_SUGGESTION_LIMIT;
        }
        if (limit < 1) {
            throw new HashtagException(HashtagErrorCode.HASHTAG_SUGGESTION_LIMIT_INVALID);
        }
        return Math.min(limit, MAX_SUGGESTION_LIMIT);
    }
}
