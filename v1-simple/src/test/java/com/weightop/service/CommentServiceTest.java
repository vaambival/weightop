package com.weightop.service;

import com.weightop.common.CommentSort;
import com.weightop.exception.CommentNotFoundException;
import com.weightop.exception.LikesAlreadyZeroException;
import com.weightop.persistence.model.CommentEntity;
import com.weightop.persistence.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class CommentServiceTest extends BaseServiceTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
    }

    private CommentEntity createComment(Long postId, String text, int likes) {
        CommentEntity entity = new CommentEntity();
        entity.setAuthorId(1L);
        entity.setPostId(postId);
        entity.setText(text);
        entity.setLikes(likes);
        return commentRepository.save(entity);
    }

    // ============ CREATE ============

    @Test
    @DisplayName("Создание комментария — сохраняет все поля")
    void createComment_shouldSaveEntity() {
        // when
        CommentEntity created = commentService.createComment(1L, 100L, "Test comment");

        // then
        assertThat(created.getId()).isNotNull();
        assertThat(created.getAuthorId()).isEqualTo(1L);
        assertThat(created.getPostId()).isEqualTo(100L);
        assertThat(created.getText()).isEqualTo("Test comment");
        assertThat(created.getLikes()).isZero();
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getUpdatedAt()).isNotNull();
    }

    // ============ READ ============

    @Test
    @DisplayName("Получение комментария по ID — успешно")
    void getCommentById_shouldReturnComment() {
        // given
        CommentEntity saved = createComment(100L, "Find me", 0);

        // when
        CommentEntity found = commentService.getCommentById(saved.getId());

        // then
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getText()).isEqualTo("Find me");
        assertThat(found.getLikes()).isZero();
    }

    @Test
    @DisplayName("Получение комментария по ID — не найден")
    void getCommentById_shouldThrowException_whenNotFound() {
        // when & then
        assertThatThrownBy(() -> commentService.getCommentById(999999L))
                .isInstanceOf(CommentNotFoundException.class);
    }

    // ============ UPDATE ============

    @Test
    @DisplayName("Обновление текста — успешно")
    void updateCommentText_shouldUpdateText() {
        // given
        CommentEntity saved = createComment(100L, "Original text", 0);

        // when
        CommentEntity updated = commentService.updateCommentText(saved.getId(), "Updated text");

        // then
        assertThat(updated.getText()).isEqualTo("Updated text");
        assertThat(updated.getUpdatedAt()).isNotNull();
        assertThat(updated.getAuthorId()).isEqualTo(saved.getAuthorId());
        assertThat(updated.getPostId()).isEqualTo(saved.getPostId());
        assertThat(updated.getLikes()).isEqualTo(saved.getLikes());
    }

    @Test
    @DisplayName("Обновление текста — комментарий не найден")
    void updateCommentText_shouldThrowException_whenNotFound() {
        // when & then
        assertThatThrownBy(() -> commentService.updateCommentText(999999L, "New text"))
                .isInstanceOf(CommentNotFoundException.class);
    }

    // ============ DELETE ============

    @Test
    @DisplayName("Удаление комментария — успешно")
    void deleteComment_shouldRemoveEntity() {
        // given
        CommentEntity saved = createComment(100L, "Delete me", 0);

        // when
        commentService.deleteComment(saved.getId());

        // then
        assertThat(commentRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("Удаление комментария — несуществующий (идемпотентно)")
    void deleteComment_shouldNotThrow_whenNotFound() {
        // when & then — не должно быть исключения
        commentService.deleteComment(999999L);
    }

    // ============ INCREMENT LIKES ============

    @Test
    @DisplayName("Инкремент лайков — увеличивает счётчик")
    void incrementLikes_shouldIncreaseCount() {
        // given
        CommentEntity saved = createComment(100L, "Like me", 5);

        // when
        commentService.incrementLikes(saved.getId());

        // then
        CommentEntity found = commentRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getLikes()).isEqualTo(6);
    }

    @Test
    @DisplayName("Инкремент лайков — несуществующий комментарий")
    void incrementLikes_shouldThrowException_whenNotFound() {
        // when & then
        assertThatThrownBy(() -> commentService.incrementLikes(999999L))
                .isInstanceOf(CommentNotFoundException.class);
    }

    // ============ DECREMENT LIKES ============

    @Test
    @DisplayName("Декремент лайков — уменьшает счётчик")
    void decrementLikes_shouldDecreaseCount() {
        // given
        CommentEntity saved = createComment(100L, "Dislike me", 5);

        // when
        commentService.decrementLikes(saved.getId());

        // then
        CommentEntity found = commentRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getLikes()).isEqualTo(4);
    }

    @Test
    @DisplayName("Декремент лайков — не уменьшает ниже нуля")
    void decrementLikes_shouldThrowException_whenLikesZero() {
        // given
        CommentEntity saved = createComment(100L, "Zero likes", 0);

        // when & then
        assertThatThrownBy(() -> commentService.decrementLikes(saved.getId()))
                .isInstanceOf(LikesAlreadyZeroException.class);
    }

    @Test
    @DisplayName("Декремент лайков — несуществующий комментарий")
    void decrementLikes_shouldThrowException_whenNotFound() {
        // when & then
        assertThatThrownBy(() -> commentService.decrementLikes(999999L))
                .isInstanceOf(CommentNotFoundException.class);
    }

    // ============ PAGINATION ============

    @Test
    @DisplayName("Пагинация — возвращает первую страницу с сортировкой по дате")
    void getCommentsByPost_shouldReturnFirstPage() {
        // given
        for (int i = 1; i <= 10; i++) {
            createComment(100L, "Comment " + i, 0);
        }

        // when
        Page<CommentEntity> page = commentService.getCommentsByPost(100L, 0, 5, CommentSort.CREATED_AT);

        // then
        assertThat(page.getContent()).hasSize(5);
        assertThat(page.getTotalElements()).isEqualTo(10);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getNumber()).isZero();
    }

    @Test
    @DisplayName("Пагинация — возвращает вторую страницу")
    void getCommentsByPost_shouldReturnSecondPage() {
        // given
        for (int i = 1; i <= 10; i++) {
            createComment(100L, "Comment " + i, 0);
        }

        // when
        Page<CommentEntity> page = commentService.getCommentsByPost(100L, 5, 5, CommentSort.CREATED_AT);

        // then
        assertThat(page.getContent()).hasSize(5);
        assertThat(page.getTotalElements()).isEqualTo(10);
        assertThat(page.getNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("Пагинация — сортировка по дате (новые сначала)")
    void getCommentsByPost_shouldSortByCreatedAtDesc() {
        // given
        CommentEntity older = createComment(100L, "Older comment", 0);
        CommentEntity newer = createComment(100L, "Newer comment", 0);

        // when
        Page<CommentEntity> page = commentService.getCommentsByPost(100L, 0, 10, CommentSort.CREATED_AT);

        // then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getText()).isEqualTo("Newer comment");
        assertThat(page.getContent().get(1).getText()).isEqualTo("Older comment");
    }

    @Test
    @DisplayName("Пагинация — сортировка по лайкам (популярные сначала)")
    void getCommentsByPost_shouldSortByLikesDesc() {
        // given
        CommentEntity lowLikes = createComment(100L, "Low likes", 2);
        CommentEntity highLikes = createComment(100L, "High likes", 10);

        // when
        Page<CommentEntity> page = commentService.getCommentsByPost(100L, 0, 10, CommentSort.LIKES);

        // then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getText()).isEqualTo("High likes");
        assertThat(page.getContent().get(1).getText()).isEqualTo("Low likes");
    }

    @Test
    @DisplayName("Пагинация — пустая страница для несуществующего поста")
    void getCommentsByPost_shouldReturnEmpty_forNonExistentPost() {
        // when
        Page<CommentEntity> page = commentService.getCommentsByPost(999L, 0, 5, CommentSort.CREATED_AT);

        // then
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getTotalPages()).isZero();
    }

    @Test
    @DisplayName("Пагинация — комментарии разных постов не смешиваются")
    void getCommentsByPost_shouldNotMixDifferentPosts() {
        // given
        createComment(100L, "Post 100 comment", 0);
        createComment(200L, "Post 200 comment", 0);

        // when
        Page<CommentEntity> page = commentService.getCommentsByPost(100L, 0, 10, CommentSort.CREATED_AT);

        // then
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getText()).isEqualTo("Post 100 comment");
    }
}