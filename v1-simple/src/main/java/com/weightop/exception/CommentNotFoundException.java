package com.weightop.exception;

public class CommentNotFoundException extends RuntimeException {

    public CommentNotFoundException(Long commentId) {
        super("Comment with ID " + commentId + " not found");
    }
}
