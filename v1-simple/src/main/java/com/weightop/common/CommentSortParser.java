package com.weightop.common;

import com.weightop.exception.InvalidSortException;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Parses the {@code sort} query parameter: {@code field[:direction]} items, e.g. {@code likes:desc}.
 */
public final class CommentSortParser {

    public static final Sort DEFAULT_SORT = Sort.by(
            Sort.Order.desc(CommentSort.LIKES.getField()),
            Sort.Order.asc(CommentSort.CREATED_AT.getField()));

    private CommentSortParser() {
    }

    public static Sort parse(List<String> values) {
        if (values == null) {
            return DEFAULT_SORT;
        }
        List<Sort.Order> orders = new ArrayList<>();
        Set<CommentSort> usedFields = EnumSet.noneOf(CommentSort.class);
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            String[] parts = value.trim().split(":", -1);
            if (parts.length > 2 || parts[0].isEmpty()) {
                throw new InvalidSortException(value, "expected format 'field' or 'field:direction'");
            }
            CommentSort field = CommentSort.fromField(parts[0])
                    .orElseThrow(() -> new InvalidSortException(value, "unsupported field '" + parts[0]
                            + "'. Supported fields: " + CommentSort.supportedFields()));
            if (!usedFields.add(field)) {
                throw new InvalidSortException(value, "field '" + parts[0] + "' is specified more than once");
            }
            Sort.Direction direction = parts.length == 1 ? Sort.Direction.ASC : parseDirection(value, parts[1]);
            orders.add(new Sort.Order(direction, field.getField()));
        }
        return orders.isEmpty() ? DEFAULT_SORT : Sort.by(orders);
    }

    private static Sort.Direction parseDirection(String value, String direction) {
        return Sort.Direction.fromOptionalString(direction)
                .orElseThrow(() -> new InvalidSortException(value, "unsupported direction '" + direction
                        + "'. Supported directions: asc, desc"));
    }
}
