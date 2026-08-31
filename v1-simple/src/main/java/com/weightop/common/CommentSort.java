package com.weightop.common;

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
}
