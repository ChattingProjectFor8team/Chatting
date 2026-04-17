package com.example.infinite.global.common.querydsl;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
// Offset 기반 Page 조회에서 정렬과 조건식 조합을 도와주는 공통 유틸이다.
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

    public static BooleanExpression like(StringPath field, String val) {
        return StringUtils.hasText(val) ? field.containsIgnoreCase(val) : null;
    }

    public static BooleanExpression likeAnyOf(String val, StringPath... fields) {
        if (!StringUtils.hasText(val) || fields == null || fields.length == 0) {
            return null;
        }

        BooleanExpression predicate = null;
        for (StringPath field : fields) {
            if (field == null) {
                continue;
            }

            BooleanExpression expression = field.containsIgnoreCase(val);
            predicate = predicate == null ? expression : predicate.or(expression);
        }

        return predicate;
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
