package com.example.infinite.domain.artistcontent.post.cache;

import com.example.infinite.domain.artistcontent.post.artistpost.repository.ArtistPostRepository;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import com.example.infinite.domain.artistcontent.post.fanletter.repository.FanLetterRepository;
import com.example.infinite.domain.artistcontent.post.fanpost.repository.FanPostRepository;
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
public class PostHotDataLoaderService {

    private final ArtistPostRepository artistPostRepository;
    private final FanPostRepository fanPostRepository;
    private final FanLetterRepository fanLetterRepository;

    /**
     * post type별 원본 테이블에서 hot 필드를 배치 조회한다.
     *
     * 왜 별도 loader가 필요한가:
     * - cache miss 난 id만 한 번에 모아 DB를 친다
     * - list 조회에서 post 10개를 각각 단건 조회하는 N+1을 피한다
     */
    public Map<Long, PostHotData> load(PostType postType, Collection<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }

        List<PostHotRow> hotRows = switch (postType) {
            case ARTIST_POST -> artistPostRepository.findHotRowsByIds(postIds);
            case FAN_POST -> fanPostRepository.findHotRowsByIds(postIds);
            case FAN_LETTER -> fanLetterRepository.findHotRowsByIds(postIds);
            default -> throw new IllegalArgumentException("지원하지 않는 hot data 대상 타입입니다: " + postType);
        };

        Map<Long, PostHotData> hotDataByPostId = new LinkedHashMap<>();
        for (PostHotRow hotRow : hotRows) {
            hotDataByPostId.put(hotRow.postId(), PostHotData.of(hotRow.likeCount(), hotRow.commentCount()));
        }
        return hotDataByPostId;
    }
}
