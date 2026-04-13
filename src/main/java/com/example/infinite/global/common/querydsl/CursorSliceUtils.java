package com.example.infinite.global.common.querydsl;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
// Cursor 기반 Slice 조회에서 커서 조건과 정렬 기준을 공통 처리한다.
public class CursorSliceUtils {
    private CursorSliceUtils() {
    }

    // 잘못된 size 입력을 방지하고 실제 조회 limit을 계산한다.
    public static int resolveLimit(Integer size, int defaultSize, int maxSize) {
        if (size == null || size < 1) {
            return defaultSize;
        }
        return Math.min(size, maxSize);
    }

    // id 내림차순 무한 스크롤에서 마지막 id 다음 구간만 조회할 때 사용한다.
    public static BooleanExpression ltCursor(NumberPath<Long> idPath, Long cursor) {
        return cursor != null ? idPath.lt(cursor) : null;
    }

    // id 오름차순 탐색이 필요할 때 다음 구간만 조회하도록 커서를 건다.
    public static BooleanExpression gtCursor(NumberPath<Long> idPath, Long cursor) {
        return cursor != null ? idPath.gt(cursor) : null;
    }

    // createdAt 기반 최신순 피드에서 시간 커서를 쓸 때 사용한다.
    public static BooleanExpression ltCursor(DateTimePath<LocalDateTime> dateTimePath, LocalDateTime cursor) {
        return cursor != null ? dateTimePath.lt(cursor) : null;
    }

    // 기본 무한 스크롤 정렬을 id 내림차순으로 맞춘다.
    public static OrderSpecifier<Long> orderByIdDesc(NumberPath<Long> idPath) {
        return new OrderSpecifier<>(Order.DESC, idPath);
    }

    // 오름차순 커서 탐색이 필요할 때 사용할 정렬 유틸이다.
    public static OrderSpecifier<Long> orderByIdAsc(NumberPath<Long> idPath) {
        return new OrderSpecifier<>(Order.ASC, idPath);
    }
}
