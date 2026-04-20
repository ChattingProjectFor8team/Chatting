package com.example.infinite.domain.artistcontent.comment.support;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MentionParser {

    private static final int MAX_MENTION_LENGTH = 50;
    private static final int MAX_MENTIONS_PER_CONTENT = 20;
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([0-9A-Za-z가-힣_]+)");

    private MentionParser() {
    }

    public static List<String> extractMentionedNicknames(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        Matcher matcher = MENTION_PATTERN.matcher(content);
        Set<String> orderedUniqueNicknames = new LinkedHashSet<>();

        while (matcher.find()) {
            String normalizedNickname = matcher.group(1).strip().toLowerCase(Locale.ROOT);
            if (normalizedNickname.isEmpty() || normalizedNickname.length() > MAX_MENTION_LENGTH) {
                continue;
            }
            orderedUniqueNicknames.add(normalizedNickname);
        }

        List<String> result = new ArrayList<>(orderedUniqueNicknames);
        if (result.size() > MAX_MENTIONS_PER_CONTENT) {
            return result.subList(0, MAX_MENTIONS_PER_CONTENT);
        }
        return result;
    }
}
