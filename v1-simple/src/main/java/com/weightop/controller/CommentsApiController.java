package com.weightop.controller;

import com.weightop.api.CommentsApi;
import com.weightop.model.Comment;
import com.weightop.model.CommentCreate;
import com.weightop.model.CommentPage;
import com.weightop.model.CommentTextUpdate;
import com.weightop.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommentsApiController implements CommentsApi {

    private final CommentService commentService;

    @Override
    public ResponseEntity<Comment> createComment(CommentCreate commentCreate) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.createComment(commentCreate.getAuthor(), commentCreate.getPostId(),
                        commentCreate.getText()));
    }

    @Override
    public ResponseEntity<Void> deleteComment(Long commentId) {
        commentService.deleteComment(commentId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<Comment> getCommentById(Long commentId) {
        return ResponseEntity.ok(commentService.getCommentById(commentId));
    }

    @Override
    public ResponseEntity<CommentPage> getCommentsByPost(Long postId, Integer limit, Integer offset,
                                                         List<String> sort) {
        return ResponseEntity.ok(commentService.getCommentsByPost(postId, offset, limit, sort));
    }

    @Override
    public ResponseEntity<Void> incrementLikes(Long commentId) {
        commentService.incrementLikes(commentId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> decrementLikes(Long commentId) {
        commentService.decrementLikes(commentId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Comment> updateCommentText(Long commentId, CommentTextUpdate commentUpdate) {
        return ResponseEntity.ok(commentService.updateCommentText(commentId, commentUpdate.getText()));
    }
}
