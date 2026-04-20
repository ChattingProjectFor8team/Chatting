package com.example.infinite.domain.artistcontent.hashtag.support;

import com.example.infinite.domain.artistcontent.hashtag.error.HashtagErrorCode;
import com.example.infinite.domain.artistcontent.hashtag.error.HashtagException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 본문에서 해시태그 토큰을 추출하고 저장용 키로 정규화한다.
 *
 * <p>reference 프로젝트처럼 파싱 책임을 support 계층으로 분리해
 * 서비스는 "매핑 동기화"만 담당하도록 유지한다.
 */
public final class HashtagParser {

    // Hashtag.name 컬럼 길이와 동일하게 유지한다.
    public static final int MAX_TAG_LENGTH = 100;
    public static final int MAX_TAGS_PER_CONTENT = 30;

    // # 뒤에는 영문, 숫자, 한글, 밑줄만 허용한다.
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#([0-9A-Za-z가-힣_]+)");

    private HashtagParser() {
    }

    public static List<String> extractNormalizedUniqueTags(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        // 정규식 매처로 문자열 전체를 훑으면서 "#태그" 패턴을 전역 검색한다.
        Matcher matcher = HASHTAG_PATTERN.matcher(content);
        // LinkedHashSet은 "중복 제거 + 첫 등장 순서 유지"를 동시에 충족한다.
        Set<String> orderedUnique = new LinkedHashSet<>();

        while (matcher.find()) {
            // group(1)은 '#' 제외 태그 본문만 반환한다.
            String rawToken = matcher.group(1);
            String normalizedToken = normalizeHashtagName(rawToken);

            if (normalizedToken.isEmpty()) {
                continue;
            }
            if (normalizedToken.length() > MAX_TAG_LENGTH) {
                throw new HashtagException(HashtagErrorCode.HASHTAG_NAME_TOO_LONG);
            }

            orderedUnique.add(normalizedToken);
        }

        if (orderedUnique.size() > MAX_TAGS_PER_CONTENT) {
            throw new HashtagException(HashtagErrorCode.TOO_MANY_HASHTAGS);
        }

        return new ArrayList<>(orderedUnique);
    }

    public static String normalizeHashtagName(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return "";
        }

        // 외부 입력 방어 차원에서 선행 '#' 제거와 앞뒤 공백 제거를 함께 처리한다.
        String token = rawToken.strip();
        if (token.startsWith("#")) {
            token = token.substring(1);
        }
        // 저장 키와 검색 키를 통일하기 위해 소문자로 정규화한다.
        return token.toLowerCase();
    }
}
