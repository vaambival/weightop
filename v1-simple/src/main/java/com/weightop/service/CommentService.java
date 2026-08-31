package com.weightop.service;

import com.weightop.common.CommentSort;
import com.weightop.exception.CommentNotFoundException;
import com.weightop.exception.LikesAlreadyZeroException;
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
    public CommentEntity createComment(Long authorId, Long postId, String text) {
        CommentEntity entity = new CommentEntity();
        entity.setAuthorId(authorId);
        entity.setPostId(postId);
        entity.setText(text);
        entity.setLikes(0);
        return commentRepository.save(entity);
    }

    /**
     * Получить комментарий по ID
     */
    @Transactional(readOnly = true)
    public CommentEntity getCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
    }

    /**
     * Обновить текст комментария
     */
    @Transactional
    public CommentEntity updateCommentText(Long commentId, String newText) {
        CommentEntity entity = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
        entity.setText(newText);
        return commentRepository.save(entity);
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
    public Page<CommentEntity> getCommentsByPost(Long postId, int offset, int limit, CommentSort sort) {
        int pageNumber = offset / limit;
        Pageable pageable = PageRequest.of(pageNumber, limit, Sort.by(sort.getField()).descending());
        return commentRepository.findAllByPostId(postId, pageable);
    }
}