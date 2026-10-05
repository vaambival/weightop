package com.weightop.common;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public enum CommentSort {
    CREATED_AT("createdAt"),
    LIKES("likes");

    private final String field;

    CommentSort(String field) {
        this.field = field;
    }

    public String getField() {
        return field;
    }

    public static Optional<CommentSort> fromField(String field) {
        return Arrays.stream(values())
                .filter(sort -> sort.field.equals(field))
                .findFirst();
    }

    public static String supportedFields() {
        return Arrays.stream(values())
                .map(CommentSort::getField)
                .collect(Collectors.joining(", "));
    }
}
