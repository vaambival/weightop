package com.weightop.exception;

public class LikesAlreadyZeroException extends RuntimeException {

    public LikesAlreadyZeroException(Long commentId) {
        super("Cannot remove a like from comment " + commentId + ": likes count is already zero");
    }
}
