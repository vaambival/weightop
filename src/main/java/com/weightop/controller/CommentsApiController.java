package com.weightop.controller;

import com.weightop.api.CommentsApi;
import com.weightop.model.Comment;
import com.weightop.model.CommentCreate;
import com.weightop.model.CommentTextUpdate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class CommentsApiController implements CommentsApi {

    @Override
    public ResponseEntity<Comment> createComment(CommentCreate commentCreate) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    public ResponseEntity<Void> deleteComment(UUID commentId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    public ResponseEntity<Comment> getCommentById(UUID commentId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    public ResponseEntity<List<Comment>> getCommentsByPost(UUID postId, Integer limit, Integer offset) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    public ResponseEntity<Comment> incrementLikes(UUID commentId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    public ResponseEntity<Comment> decrementLikes(UUID commentId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    public ResponseEntity<Comment> updateCommentText(UUID commentId, CommentTextUpdate commentUpdate) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
