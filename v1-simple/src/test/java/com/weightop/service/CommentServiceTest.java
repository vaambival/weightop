package com.weightop.service;

import com.weightop.exception.CommentNotFoundException;
import com.weightop.exception.InvalidSortException;
import com.weightop.exception.LikesAlreadyZeroException;
import com.weightop.model.Comment;
import com.weightop.model.CommentCount;
import com.weightop.model.CommentPage;
import com.weightop.persistence.model.CommentEntity;
import com.weightop.persistence.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        Comment created = commentService.createComment(1L, 100L, "Test comment");

        // then
        assertThat(created.getId()).isNotNull();
        assertThat(created.getAuthor()).isEqualTo(1L);
        assertThat(created.getPostId()).isEqualTo(100L);
        assertThat(created.getText()).isEqualTo("Test comment");
        assertThat(created.getLikes()).isZero();
        assertThat(created.getCreatedAt()).isNotNull();
    }

    // ============ READ ============

    @Test
    @DisplayName("Получение комментария по ID — успешно")
    void getCommentById_shouldReturnComment() {
        // given
        CommentEntity saved = createComment(100L, "Find me", 0);

        // when
        var found = commentService.getCommentById(saved.getId());

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
        var updated = commentService.updateCommentText(saved.getId(), "Updated text");

        // then
        assertThat(updated.getText()).isEqualTo("Updated text");
        assertThat(updated.getAuthor()).isEqualTo(saved.getAuthorId());
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

    // ============ COUNT ============

    @Test
    @DisplayName("Подсчёт — возвращает количество комментариев поста")
    void getCommentsCountByPost_shouldReturnCount() {
        // given
        createComment(100L, "First", 0);
        createComment(100L, "Second", 3);
        createComment(200L, "Other post", 0);

        // when
        CommentCount count = commentService.getCommentsCountByPost(100L);

        // then
        assertThat(count.getPostId()).isEqualTo(100L);
        assertThat(count.getCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Подсчёт — учитывает удаление комментария")
    void getCommentsCountByPost_shouldDecrease_afterDelete() {
        // given
        createComment(100L, "Keep", 0);
        CommentEntity toDelete = createComment(100L, "Delete", 0);

        // when
        commentService.deleteComment(toDelete.getId());

        // then
        assertThat(commentService.getCommentsCountByPost(100L).getCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Подсчёт — 0 для поста без комментариев")
    void getCommentsCountByPost_shouldReturnZero_forNonExistentPost() {
        CommentCount count = commentService.getCommentsCountByPost(999L);

        assertThat(count.getPostId()).isEqualTo(999L);
        assertThat(count.getCount()).isZero();
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
        CommentPage page = commentService.getCommentsByPost(100L, 0, 5, List.of("createdAt:desc"));

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
        CommentPage page = commentService.getCommentsByPost(100L, 5, 5, List.of("createdAt:desc"));

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
        CommentPage page = commentService.getCommentsByPost(100L, 0, 10, List.of("createdAt:desc"));

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
        CommentPage page = commentService.getCommentsByPost(100L, 0, 10, List.of("likes:desc"));

        // then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getText()).isEqualTo("High likes");
        assertThat(page.getContent().get(1).getText()).isEqualTo("Low likes");
    }

    @Test
    @DisplayName("Пагинация — пустая страница для несуществующего поста")
    void getCommentsByPost_shouldReturnEmpty_forNonExistentPost() {
        // when
        CommentPage page = commentService.getCommentsByPost(999L, 0, 5, List.of("createdAt:desc"));

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
        CommentPage page = commentService.getCommentsByPost(100L, 0, 10, List.of("createdAt:desc"));

        // then
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getText()).isEqualTo("Post 100 comment");
    }

    @Test
    @DisplayName("Пагинация — сортировка по умолчанию: лайки по убыванию, затем дата по возрастанию")
    void getCommentsByPost_shouldUseDefaultSort() {
        // given
        createComment(100L, "Old, 5 likes", 5);
        createComment(100L, "10 likes", 10);
        createComment(100L, "New, 5 likes", 5);

        // when
        CommentPage page = commentService.getCommentsByPost(100L, 0, 10, null);

        // then
        assertThat(page.getContent()).extracting(Comment::getText)
                .containsExactly("10 likes", "Old, 5 likes", "New, 5 likes");
    }

    @Test
    @DisplayName("Пагинация — offset, не кратный limit, возвращает точное окно")
    void getCommentsByPost_shouldRespectExactOffset() {
        // given
        for (int i = 1; i <= 10; i++) {
            createComment(100L, "Comment " + i, 0);
        }

        // when
        CommentPage page = commentService.getCommentsByPost(100L, 2, 3, List.of("createdAt:asc"));

        // then
        assertThat(page.getContent()).extracting(Comment::getText)
                .containsExactly("Comment 3", "Comment 4", "Comment 5");
    }

    @Test
    @DisplayName("Пагинация — обход всех страниц без дублей и пропусков при равных лайках")
    void getCommentsByPost_shouldPageWithoutDuplicates_whenLikesAreEqual() {
        // given
        for (int i = 1; i <= 23; i++) {
            createComment(100L, "Comment " + i, 0);
        }

        // when
        Set<Long> seen = new HashSet<>();
        int total = 0;
        for (int offset = 0; offset < 23; offset += 5) {
            List<Comment> content = commentService.getCommentsByPost(100L, offset, 5, List.of("likes:desc"))
                    .getContent();
            total += content.size();
            content.forEach(c -> seen.add(c.getId()));
        }

        // then
        assertThat(total).isEqualTo(23);
        assertThat(seen).hasSize(23);
    }

    @Test
    @DisplayName("Пагинация — неподдерживаемое поле сортировки")
    void getCommentsByPost_shouldThrow_whenSortFieldUnsupported() {
        assertThatThrownBy(() -> commentService.getCommentsByPost(100L, 0, 10, List.of("text:asc")))
                .isInstanceOf(InvalidSortException.class)
                .hasMessage("Invalid value 'text:asc' for parameter 'sort': "
                        + "unsupported field 'text'. Supported fields: createdAt, likes");
    }
}
