package com.weightop.service;

import com.weightop.common.CommentSort;
import com.weightop.exception.CommentNotFoundException;
import com.weightop.exception.LikesAlreadyZeroException;
import com.weightop.model.Comment;
import com.weightop.model.CommentPage;
import com.weightop.persistence.model.CommentEntity;
import com.weightop.persistence.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;

    /**
     * Создать комментарий
     */
    @Transactional
    public Comment createComment(Long authorId, Long postId, String text) {
        CommentEntity entity = new CommentEntity();
        entity.setAuthorId(authorId);
        entity.setPostId(postId);
        entity.setText(text);
        entity.setLikes(0);
        return commentFromEntity(commentRepository.saveAndFlush(entity));
    }

    private Comment commentFromEntity(CommentEntity entity) {
        return new Comment(entity.getId(), entity.getAuthorId(), entity.getPostId(), entity.getText(),
                entity.getLikes(), entity.getCreatedAt());
    }

    /**
     * Получить комментарий по ID
     */
    @Transactional(readOnly = true)
    public Comment getCommentById(Long commentId) {
        return commentFromEntity(commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId)));
    }

    /**
     * Обновить текст комментария
     */
    @Transactional
    public Comment updateCommentText(Long commentId, String newText) {
        CommentEntity entity = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
        entity.setText(newText);
        return commentFromEntity(commentRepository.save(entity));
    }

    /**
     * Удалить комментарий (идемпотентно)
     */
    @Transactional
    public void deleteComment(Long commentId) {
        commentRepository.deleteById(commentId);
    }

    /**
     * Увеличить количество лайков на 1
     */
    @Transactional
    public void incrementLikes(Long commentId) {
        int updated = commentRepository.incrementLikes(commentId);
        if (updated == 0) {
            throw new CommentNotFoundException(commentId);
        }
    }

    /**
     * Уменьшить количество лайков на 1
     */
    @Transactional
    public void decrementLikes(Long commentId) {
        int updated = commentRepository.decrementLikes(commentId);
        if (updated == 0) {
            if (commentRepository.existsById(commentId)) {
                throw new LikesAlreadyZeroException(commentId);
            }
            throw new CommentNotFoundException(commentId);
        }
    }

    /**
     * Получить комментарии поста с пагинацией
     */
    @Transactional(readOnly = true)
    public CommentPage getCommentsByPost(Long postId, int offset, int limit, CommentSort sort) {
        int pageNumber = offset / limit;
        Pageable pageable = PageRequest.of(pageNumber, limit, Sort.by(sort.getField()).descending());
        return toCommentPage(commentRepository.findAllByPostId(postId, pageable).map(this::commentFromEntity));
    }

    private CommentPage toCommentPage(Page<Comment> page) {
        return new CommentPage(page.getContent(), page.getTotalElements(), page.getTotalPages(),
                page.getNumber(), page.getSize());
    }
}