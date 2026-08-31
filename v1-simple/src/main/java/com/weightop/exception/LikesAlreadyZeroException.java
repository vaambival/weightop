package com.weightop.exception;

public class LikesAlreadyZeroException extends RuntimeException {

    public LikesAlreadyZeroException(Long commentId) {
        super("Likes count for comment " + commentId + " is already zero");
    }
}
