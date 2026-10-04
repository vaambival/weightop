package com.weightop.controller;

import com.weightop.api.CommentsApi;
import com.weightop.common.CommentSort;
import com.weightop.model.Comment;
import com.weightop.model.CommentCreate;
import com.weightop.model.CommentTextUpdate;
import com.weightop.persistence.model.CommentEntity;
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
        CommentEntity created = commentService.createComment(
                commentCreate.getAuthor(), commentCreate.getPostId(), commentCreate.getText());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(created));
    }

    @Override
    public ResponseEntity<Void> deleteComment(Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Comment> getCommentById(Long commentId) {
        return ResponseEntity.ok(toDto(commentService.getCommentById(commentId)));
    }

    @Override
    public ResponseEntity<List<Comment>> getCommentsByPost(Long postId, Integer limit, Integer offset) {
        List<Comment> comments = commentService.getCommentsByPost(postId, offset, limit, CommentSort.CREATED_AT)
                .map(CommentsApiController::toDto)
                .getContent();
        return ResponseEntity.ok(comments);
    }

    @Override
    public ResponseEntity<Comment> incrementLikes(Long commentId) {
        commentService.incrementLikes(commentId);
        return ResponseEntity.ok(toDto(commentService.getCommentById(commentId)));
    }

    @Override
    public ResponseEntity<Comment> decrementLikes(Long commentId) {
        commentService.decrementLikes(commentId);
        return ResponseEntity.ok(toDto(commentService.getCommentById(commentId)));
    }

    @Override
    public ResponseEntity<Comment> updateCommentText(Long commentId, CommentTextUpdate commentUpdate) {
        return ResponseEntity.ok(toDto(commentService.updateCommentText(commentId, commentUpdate.getText())));
    }

    private static Comment toDto(CommentEntity entity) {
        return new Comment()
                .id(entity.getId())
                .author(entity.getAuthorId())
                .postId(entity.getPostId())
                .text(entity.getText())
                .likes(entity.getLikes())
                .createdAt(entity.getCreatedAt());
    }
}
