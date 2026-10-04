package com.weightop.web;

import com.weightop.persistence.model.CommentEntity;
import com.weightop.persistence.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommentsApiControllerTest extends BaseWebTest {

    private static final String BASE = "/api/v1";
    private static final long MISSING_ID = 999_999L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CommentRepository commentRepository;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
    }

    private CommentEntity saveComment(Long postId, String text, int likes) {
        CommentEntity entity = new CommentEntity();
        entity.setAuthorId(1L);
        entity.setPostId(postId);
        entity.setText(text);
        entity.setLikes(likes);
        return commentRepository.save(entity);
    }

    // ============ CREATE ============

    @Test
    @DisplayName("POST /comments — создаёт комментарий и возвращает 201")
    void createComment_shouldReturnCreated() throws Exception {
        mockMvc.perform(post(BASE + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"author": 1, "postId": 100, "text": "Hello"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.author").value(1))
                .andExpect(jsonPath("$.postId").value(100))
                .andExpect(jsonPath("$.text").value("Hello"))
                .andExpect(jsonPath("$.likes").value(0))
                .andExpect(jsonPath("$.createdAt", notNullValue()));

        assertThat(commentRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /comments — 400 при отсутствии обязательных полей")
    void createComment_shouldReturnBadRequest_whenTextMissing() throws Exception {
        mockMvc.perform(post(BASE + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"author": 1, "postId": 100}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", notNullValue()));

        assertThat(commentRepository.count()).isZero();
    }

    @Test
    @DisplayName("POST /comments — 400 при слишком длинном тексте")
    void createComment_shouldReturnBadRequest_whenTextTooLong() throws Exception {
        String longText = "a".repeat(1025);
        mockMvc.perform(post(BASE + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"author\": 1, \"postId\": 100, \"text\": \"" + longText + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /comments — 400 при некорректном JSON")
    void createComment_shouldReturnBadRequest_whenMalformedJson() throws Exception {
        mockMvc.perform(post(BASE + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest());
    }

    // ============ READ ============

    @Test
    @DisplayName("GET /comments/{id} — возвращает комментарий")
    void getCommentById_shouldReturnComment() throws Exception {
        CommentEntity saved = saveComment(100L, "Find me", 3);

        mockMvc.perform(get(BASE + "/comments/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.text").value("Find me"))
                .andExpect(jsonPath("$.likes").value(3));
    }

    @Test
    @DisplayName("GET /comments/{id} — 404, если комментарий не найден")
    void getCommentById_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get(BASE + "/comments/{id}", MISSING_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", notNullValue()));
    }

    @Test
    @DisplayName("GET /comments/{id} — 400 при нечисловом ID")
    void getCommentById_shouldReturnBadRequest_whenIdInvalid() throws Exception {
        mockMvc.perform(get(BASE + "/comments/{id}", "abc"))
                .andExpect(status().isBadRequest());
    }

    // ============ UPDATE ============

    @Test
    @DisplayName("PUT /comments/{id} — обновляет текст")
    void updateCommentText_shouldUpdate() throws Exception {
        CommentEntity saved = saveComment(100L, "Original", 0);

        mockMvc.perform(put(BASE + "/comments/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text": "Updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.text").value("Updated"));

        assertThat(commentRepository.findById(saved.getId()))
                .get()
                .extracting(CommentEntity::getText)
                .isEqualTo("Updated");
    }

    @Test
    @DisplayName("PUT /comments/{id} — 404, если комментарий не найден")
    void updateCommentText_shouldReturnNotFound() throws Exception {
        mockMvc.perform(put(BASE + "/comments/{id}", MISSING_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text": "Updated"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /comments/{id} — 400 при пустом тексте")
    void updateCommentText_shouldReturnBadRequest_whenTextEmpty() throws Exception {
        CommentEntity saved = saveComment(100L, "Original", 0);

        mockMvc.perform(put(BASE + "/comments/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ============ DELETE ============

    @Test
    @DisplayName("DELETE /comments/{id} — удаляет комментарий")
    void deleteComment_shouldRemove() throws Exception {
        CommentEntity saved = saveComment(100L, "Delete me", 0);

        mockMvc.perform(delete(BASE + "/comments/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        assertThat(commentRepository.existsById(saved.getId())).isFalse();
    }

    @Test
    @DisplayName("DELETE /comments/{id} — идемпотентно для несуществующего комментария")
    void deleteComment_shouldBeIdempotent() throws Exception {
        mockMvc.perform(delete(BASE + "/comments/{id}", MISSING_ID))
                .andExpect(status().isNoContent());
    }

    // ============ LIKES ============

    @Test
    @DisplayName("POST /comments/{id}/like — увеличивает лайки")
    void incrementLikes_shouldIncrease() throws Exception {
        CommentEntity saved = saveComment(100L, "Like me", 5);

        mockMvc.perform(post(BASE + "/comments/{id}/like", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likes").value(6));
    }

    @Test
    @DisplayName("POST /comments/{id}/like — 404, если комментарий не найден")
    void incrementLikes_shouldReturnNotFound() throws Exception {
        mockMvc.perform(post(BASE + "/comments/{id}/like", MISSING_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /comments/{id}/like — уменьшает лайки")
    void decrementLikes_shouldDecrease() throws Exception {
        CommentEntity saved = saveComment(100L, "Unlike me", 5);

        mockMvc.perform(delete(BASE + "/comments/{id}/like", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likes").value(4));
    }

    @Test
    @DisplayName("DELETE /comments/{id}/like — 400, если лайков уже 0")
    void decrementLikes_shouldReturnBadRequest_whenZero() throws Exception {
        CommentEntity saved = saveComment(100L, "No likes", 0);

        mockMvc.perform(delete(BASE + "/comments/{id}/like", saved.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", notNullValue()));
    }

    @Test
    @DisplayName("DELETE /comments/{id}/like — 404, если комментарий не найден")
    void decrementLikes_shouldReturnNotFound() throws Exception {
        mockMvc.perform(delete(BASE + "/comments/{id}/like", MISSING_ID))
                .andExpect(status().isNotFound());
    }

    // ============ LIST BY POST ============

    @Test
    @DisplayName("GET /posts/{postId}/comments — возвращает только комментарии поста")
    void getCommentsByPost_shouldReturnPostComments() throws Exception {
        saveComment(100L, "First", 0);
        saveComment(100L, "Second", 0);
        saveComment(200L, "Other post", 0);

        mockMvc.perform(get(BASE + "/posts/{postId}/comments", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].postId").value(everyItem(
                        is(100))));
    }

    @Test
    @DisplayName("GET /posts/{postId}/comments — учитывает limit и offset")
    void getCommentsByPost_shouldPaginate() throws Exception {
        for (int i = 0; i < 5; i++) {
            saveComment(100L, "Comment " + i, 0);
        }

        mockMvc.perform(get(BASE + "/posts/{postId}/comments", 100L)
                        .param("limit", "2")
                        .param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get(BASE + "/posts/{postId}/comments", 100L)
                        .param("limit", "2")
                        .param("offset", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /posts/{postId}/comments — пустой список для поста без комментариев")
    void getCommentsByPost_shouldReturnEmptyList() throws Exception {
        mockMvc.perform(get(BASE + "/posts/{postId}/comments", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /posts/{postId}/comments — 400 при limit вне диапазона")
    void getCommentsByPost_shouldReturnBadRequest_whenLimitOutOfRange() throws Exception {
        mockMvc.perform(get(BASE + "/posts/{postId}/comments", 100L).param("limit", "0"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get(BASE + "/posts/{postId}/comments", 100L).param("limit", "101"))
                .andExpect(status().isBadRequest());
    }
}
