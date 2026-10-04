package com.weightop.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weightop.model.CommentCreate;
import com.weightop.model.CommentTextUpdate;
import com.weightop.persistence.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CommentsApiControllerTest extends BaseWebTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CommentRepository commentRepository;

    private static final String BASE_URL = "/api/v1";

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
    }

    private Long createCommentViaApi(Long authorId, Long postId, String text) throws Exception {
        CommentCreate request = new CommentCreate();
        request.setAuthor(authorId);
        request.setPostId(postId);
        request.setText(text);

        String response = mockMvc.perform(post(BASE_URL + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    // ============ POST /comments ============

    @Test
    @DisplayName("POST /comments — создаёт комментарий, возвращает 201")
    void createComment_shouldReturn201() throws Exception {
        CommentCreate request = new CommentCreate();
        request.setAuthor(1L);
        request.setPostId(100L);
        request.setText("Hello, world!");

        mockMvc.perform(post(BASE_URL + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.author").value(1))
                .andExpect(jsonPath("$.postId").value(100))
                .andExpect(jsonPath("$.text").value("Hello, world!"))
                .andExpect(jsonPath("$.likes").value(0))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("POST /comments — 400 при отсутствии text")
    void createComment_shouldReturn400_whenTextMissing() throws Exception {
        String invalidJson = """
                {
                  "author": 1,
                  "postId": 100
                }
                """;

        mockMvc.perform(post(BASE_URL + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    // ============ GET /comments/{id} ============

    @Test
    @DisplayName("GET /comments/{id} — возвращает комментарий")
    void getCommentById_shouldReturn200() throws Exception {
        Long id = createCommentViaApi(1L, 100L, "Find me");

        mockMvc.perform(get(BASE_URL + "/comments/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.text").value("Find me"));
    }

    @Test
    @DisplayName("GET /comments/{id} — 404 если не найден")
    void getCommentById_shouldReturn404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/comments/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    // ============ PUT /comments/{id} ============

    @Test
    @DisplayName("PUT /comments/{id} — обновляет текст")
    void updateCommentText_shouldReturn200() throws Exception {
        Long id = createCommentViaApi(1L, 100L, "Original");

        CommentTextUpdate update = new CommentTextUpdate();
        update.setText("Updated text");

        mockMvc.perform(put(BASE_URL + "/comments/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.text").value("Updated text"));
    }

    @Test
    @DisplayName("PUT /comments/{id} — 404 если не найден")
    void updateCommentText_shouldReturn404() throws Exception {
        CommentTextUpdate update = new CommentTextUpdate();
        update.setText("Updated text");

        mockMvc.perform(put(BASE_URL + "/comments/{id}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());
    }

    // ============ DELETE /comments/{id} ============

    @Test
    @DisplayName("DELETE /comments/{id} — 204 при успехе")
    void deleteComment_shouldReturn204() throws Exception {
        Long id = createCommentViaApi(1L, 100L, "Delete me");

        mockMvc.perform(delete(BASE_URL + "/comments/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE_URL + "/comments/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /comments/{id} — идемпотентно (204 для несуществующего)")
    void deleteComment_shouldBeIdempotent() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/comments/{id}", 999999L))
                .andExpect(status().isNoContent());
    }

    // ============ POST /comments/{id}/like ============

    @Test
    @DisplayName("POST /comments/{id}/like — увеличивает лайки")
    void incrementLikes_shouldReturn200() throws Exception {
        Long id = createCommentViaApi(1L, 100L, "Like me");

        mockMvc.perform(post(BASE_URL + "/comments/{id}/like", id))
                .andExpect(status().isOk());

        mockMvc.perform(get(BASE_URL + "/comments/{id}", id))
                .andExpect(jsonPath("$.likes").value(1));
    }

    @Test
    @DisplayName("POST /comments/{id}/like — 404 для несуществующего")
    void incrementLikes_shouldReturn404() throws Exception {
        mockMvc.perform(post(BASE_URL + "/comments/{id}/like", 999999L))
                .andExpect(status().isNotFound());
    }

    // ============ DELETE /comments/{id}/like ============

    @Test
    @DisplayName("DELETE /comments/{id}/like — уменьшает лайки")
    void decrementLikes_shouldReturn200() throws Exception {
        Long id = createCommentViaApi(1L, 100L, "Dislike me");

        mockMvc.perform(post(BASE_URL + "/comments/{id}/like", id))
                .andExpect(status().isOk());

        mockMvc.perform(delete(BASE_URL + "/comments/{id}/like", id))
                .andExpect(status().isOk());

        mockMvc.perform(get(BASE_URL + "/comments/{id}", id))
                .andExpect(jsonPath("$.likes").value(0));
    }

    @Test
    @DisplayName("DELETE /comments/{id}/like — 400 если likes = 0")
    void decrementLikes_shouldReturn400_whenLikesZero() throws Exception {
        Long id = createCommentViaApi(1L, 100L, "Zero likes");

        mockMvc.perform(delete(BASE_URL + "/comments/{id}/like", id))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /comments/{id}/like — 404 для несуществующего")
    void decrementLikes_shouldReturn404() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/comments/{id}/like", 999999L))
                .andExpect(status().isNotFound());
    }

    // ============ GET /posts/{postId}/comments ============

    @Test
    @DisplayName("GET /posts/{postId}/comments — возвращает страницу")
    void getCommentsByPost_shouldReturnPage() throws Exception {
        for (int i = 1; i <= 5; i++) {
            createCommentViaApi(1L, 100L, "Comment " + i);
        }

        mockMvc.perform(get(BASE_URL + "/posts/{postId}/comments", 100L)
                        .param("limit", "3")
                        .param("offset", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(3));
    }

    @Test
    @DisplayName("GET /posts/{postId}/comments — вторая страница")
    void getCommentsByPost_shouldReturnSecondPage() throws Exception {
        for (int i = 1; i <= 5; i++) {
            createCommentViaApi(1L, 100L, "Comment " + i);
        }

        mockMvc.perform(get(BASE_URL + "/posts/{postId}/comments", 100L)
                        .param("limit", "3")
                        .param("offset", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.number").value(1));
    }

    @Test
    @DisplayName("GET /posts/{postId}/comments — пустая страница")
    void getCommentsByPost_shouldReturnEmpty() throws Exception {
        mockMvc.perform(get(BASE_URL + "/posts/{postId}/comments", 999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("GET /posts/{postId}/comments — 400 при limit > 100")
    void getCommentsByPost_shouldReturn400_whenLimitTooLarge() throws Exception {
        mockMvc.perform(get(BASE_URL + "/posts/{postId}/comments", 100L)
                        .param("limit", "101"))
                .andExpect(status().isBadRequest());
    }
}
