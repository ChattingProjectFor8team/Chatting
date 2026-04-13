package com.example.infinite.global.common.querydsl;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class QuerydslUtils {
    private QuerydslUtils() {
    }

    // Offset 기반 페이지 정렬에서 사용하는 유틸이다.
    public static <T extends Comparable> OrderSpecifier<?>[] getSort(
            Sort sort,
            Map<String, Expression<?>> sortMap,
            OrderSpecifier<?> defaultSort
    ) {
        List<OrderSpecifier<T>> orders = sort.stream()
                .map(order -> {
                    Expression<?> expr = sortMap.get(order.getProperty());
                    if (expr == null) {
                        return null;
                    }

                    return new OrderSpecifier<>(
                            order.isAscending() ? Order.ASC : Order.DESC,
                            (Expression<T>) expr
                    );
                })
                .filter(Objects::nonNull)
                .toList();

        return orders.isEmpty()
                ? new OrderSpecifier[]{defaultSort}
                : orders.toArray(new OrderSpecifier[0]);
    }

    // Cursor 기반 조회에서 사용할 limit 계산용 유틸이다.
    public static int resolveLimit(Integer size, int defaultSize, int maxSize) {
        if (size == null || size < 1) {
            return defaultSize;
        }
        return Math.min(size, maxSize);
    }

    // id 기반 내림차순 커서 페이징에서 다음 구간을 잘라낼 때 사용한다.
    public static BooleanExpression ltCursor(NumberPath<Long> idPath, Long cursor) {
        return cursor != null ? idPath.lt(cursor) : null;
    }

    // id 기반 오름차순 커서 페이징에서 다음 구간을 잘라낼 때 사용한다.
    public static BooleanExpression gtCursor(NumberPath<Long> idPath, Long cursor) {
        return cursor != null ? idPath.gt(cursor) : null;
    }

    // createdAt 같은 시간 컬럼 기준 커서 페이징이 필요할 때 사용한다.
    public static BooleanExpression ltCursor(DateTimePath<LocalDateTime> dateTimePath, LocalDateTime cursor) {
        return cursor != null ? dateTimePath.lt(cursor) : null;
    }

    public static OrderSpecifier<Long> orderByIdDesc(NumberPath<Long> idPath) {
        return new OrderSpecifier<>(Order.DESC, idPath);
    }

    public static OrderSpecifier<Long> orderByIdAsc(NumberPath<Long> idPath) {
        return new OrderSpecifier<>(Order.ASC, idPath);
    }

    public static BooleanExpression like(StringPath field, String val) {
        return val != null ? field.contains(val) : null;
    }

    public static BooleanExpression eq(NumberPath<Long> field, Long val) {
        return val != null ? field.eq(val) : null;
    }

    public static BooleanExpression eq(StringPath field, String val) {
        return val != null ? field.eq(val) : null;
    }

    public static BooleanExpression eq(EnumPath field, Enum val) {
        return val != null ? field.eq(val) : null;
    }
}
