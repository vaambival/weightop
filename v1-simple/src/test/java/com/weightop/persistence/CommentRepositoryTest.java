package com.weightop.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CommentRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    private CommentEntity createComment(Long postId, String text, int likes) {
        CommentEntity entity = new CommentEntity();
        entity.setAuthorId(1L);
        entity.setPostId(postId);
        entity.setText(text);
        entity.setLikes(likes);
        return commentRepository.saveAndFlush(entity);
    }

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
    }

    @Test
    @DisplayName("Создание комментария — поля сохраняются корректно")
    void createComment_shouldSaveEntity() {
        // when
        CommentEntity saved = createComment(100L, "Test comment", 0);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAuthorId()).isEqualTo(1L);
        assertThat(saved.getPostId()).isEqualTo(100L);
        assertThat(saved.getText()).isEqualTo("Test comment");
        assertThat(saved.getLikes()).isZero();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Поиск по ID — существующий комментарий")
    void findById_shouldReturnComment() {
        // given
        CommentEntity saved = createComment(100L, "Find me", 0);

        // when
        Optional<CommentEntity> found = commentRepository.findById(saved.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getText()).isEqualTo("Find me");
    }

    @Test
    @DisplayName("Поиск по ID — несуществующий комментарий")
    void findById_shouldReturnEmpty() {
        // when
        Optional<CommentEntity> found = commentRepository.findById(999999L);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Инкремент лайков — увеличивает счётчик")
    void incrementLikes_shouldIncreaseCount() {
        // given
        CommentEntity saved = createComment(100L, "Like me", 5);

        // when
        int updated = commentRepository.incrementLikes(saved.getId());

        // then
        assertThat(updated).isEqualTo(1);

        CommentEntity found = commentRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getLikes()).isEqualTo(6);
    }

    @Test
    @DisplayName("Инкремент лайков — несуществующий комментарий")
    void incrementLikes_shouldReturnZero_forNonExistentComment() {
        // when
        int updated = commentRepository.incrementLikes(999999L);

        // then
        assertThat(updated).isZero();
    }

    @Test
    @DisplayName("Декремент лайков — уменьшает счётчик")
    @Transactional
    void decrementLikes_shouldDecreaseCount() {
        // given
        CommentEntity saved = createComment(100L, "Dislike me", 5);

        // when
        int updated = commentRepository.decrementLikes(saved.getId());

        // then
        assertThat(updated).isEqualTo(1);

        CommentEntity found = commentRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getLikes()).isEqualTo(4);
    }

    @Test
    @DisplayName("Декремент лайков — не уменьшает ниже нуля")
    void decrementLikes_shouldNotGoBelowZero() {
        // given
        CommentEntity saved = createComment(100L, "Zero likes", 0);

        // when
        int updated = commentRepository.decrementLikes(saved.getId());

        // then
        assertThat(updated).isZero();

        CommentEntity found = commentRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getLikes()).isZero();
    }

    @Test
    @DisplayName("Пагинация — возвращает страницу с сортировкой по created_at DESC")
    void findAllByPostId_shouldReturnPaginatedResults() {
        // given
        for (int i = 1; i <= 10; i++) {
            createComment(100L, "Comment " + i, 0);
        }

        Pageable pageable = PageRequest.of(0, 5, Sort.by("createdAt").descending());

        // when
        Page<CommentEntity> page = commentRepository.findAllByPostId(100L, pageable);

        // then
        assertThat(page.getContent()).hasSize(5);
        assertThat(page.getTotalElements()).isEqualTo(10);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getNumber()).isZero();
    }

    @Test
    @DisplayName("Пагинация — возвращает пустую страницу для несуществующего поста")
    void findAllByPostId_shouldReturnEmpty_forNonExistentPost() {
        // given
        Pageable pageable = PageRequest.of(0, 5, Sort.by("createdAt").descending());

        // when
        Page<CommentEntity> page = commentRepository.findAllByPostId(999L, pageable);

        // then
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Пагинация — вторая страница содержит оставшиеся записи")
    void findAllByPostId_shouldReturnSecondPage() {
        // given
        for (int i = 1; i <= 10; i++) {
            createComment(100L, "Comment " + i, 0);
        }

        Pageable pageable = PageRequest.of(1, 5, Sort.by("createdAt").descending());

        // when
        Page<CommentEntity> page = commentRepository.findAllByPostId(100L, pageable);

        // then
        assertThat(page.getContent()).hasSize(5);
        assertThat(page.getTotalElements()).isEqualTo(10);
        assertThat(page.getNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("Пагинация — сортировка по created_at DESC (новые сначала)")
    void findAllByPostId_shouldSortByCreatedAtDesc() {
        // given
        CommentEntity old = createComment(100L, "Old comment", 0);
        CommentEntity newer = createComment(100L, "New comment", 0);

        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

        // when
        Page<CommentEntity> page = commentRepository.findAllByPostId(100L, pageable);

        // then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getText()).isEqualTo("New comment");
        assertThat(page.getContent().get(1).getText()).isEqualTo("Old comment");
    }

    @Test
    @DisplayName("Удаление — физически удаляет запись")
    void delete_shouldRemoveEntity() {
        // given
        CommentEntity saved = createComment(100L, "Delete me", 0);

        // when
        commentRepository.delete(saved);

        // then
        Optional<CommentEntity> found = commentRepository.findById(saved.getId());
        assertThat(found).isEmpty();
    }
}