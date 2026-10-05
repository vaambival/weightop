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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request body: field 'text' must not be null"));
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
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Comment with id 999999 not found"));
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
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Comment with id 999999 not found"));
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
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Comment with id 999999 not found"));
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Cannot remove a like from comment " + id + ": likes count is already zero"));
    }

    @Test
    @DisplayName("DELETE /comments/{id}/like — 404 для несуществующего")
    void decrementLikes_shouldReturn404() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/comments/{id}/like", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Comment with id 999999 not found"));
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Invalid request: parameter 'limit' must be less than or equal to 100"));
    }

    @Test
    @DisplayName("GET /posts/{postId}/comments — 400 при limit < 1")
    void getCommentsByPost_shouldReturn400_whenLimitTooSmall() throws Exception {
        mockMvc.perform(get(BASE_URL + "/posts/{postId}/comments", 100L)
                        .param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Invalid request: parameter 'limit' must be greater than or equal to 1"));
    }

    @Test
    @DisplayName("GET /posts/{postId}/comments — сортировка по умолчанию: лайки desc, дата asc")
    void getCommentsByPost_shouldUseDefaultSort() throws Exception {
        Long first = createCommentViaApi(1L, 100L, "First, no likes");
        Long popular = createCommentViaApi(1L, 100L, "Two likes");
        Long second = createCommentViaApi(1L, 100L, "Second, no likes");
        mockMvc.perform(post(BASE_URL + "/comments/{commentId}/like", popular)).andExpect(status().isOk());
        mockMvc.perform(post(BASE_URL + "/comments/{commentId}/like", popular)).andExpect(status().isOk());

        mockMvc.perform(get(BASE_URL + "/posts/{postId}/comments", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", contains(
                        popular.intValue(), first.intValue(), second.intValue())));
    }

    @Test
    @DisplayName("GET /posts/{postId}/comments — сортировка по дате desc")
    void getCommentsByPost_shouldSortByCreatedAtDesc() throws Exception {
        Long first = createCommentViaApi(1L, 100L, "First");
        Long second = createCommentViaApi(1L, 100L, "Second");
        Long third = createCommentViaApi(1L, 100L, "Third");

        mockMvc.perform(get(BASE_URL + "/posts/{postId}/comments", 100L)
                        .param("sort", "createdAt:desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", contains(
                        third.intValue(), second.intValue(), first.intValue())));
    }

    @Test
    @DisplayName("GET /posts/{postId}/comments — несколько полей сортировки через запятую и повтор параметра")
    void getCommentsByPost_shouldSortBySeveralFields() throws Exception {
        Long first = createCommentViaApi(1L, 100L, "First");
        Long liked = createCommentViaApi(1L, 100L, "Liked");
        Long third = createCommentViaApi(1L, 100L, "Third");
        mockMvc.perform(post(BASE_URL + "/comments/{commentId}/like", liked)).andExpect(status().isOk());

        mockMvc.perform(get(BASE_URL + "/posts/{postId}/comments", 100L)
                        .param("sort", "likes:asc,createdAt:desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", contains(
                        third.intValue(), first.intValue(), liked.intValue())));

        mockMvc.perform(get(BASE_URL + "/posts/{postId}/comments", 100L)
                        .param("sort", "likes:asc")
                        .param("sort", "createdAt:desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", contains(
                        third.intValue(), first.intValue(), liked.intValue())));
    }

    @Test
    @DisplayName("GET /posts/{postId}/comments — offset, не кратный limit")
    void getCommentsByPost_shouldRespectExactOffset() throws Exception {
        for (int i = 1; i <= 5; i++) {
            createCommentViaApi(1L, 100L, "Comment " + i);
        }

        mockMvc.perform(get(BASE_URL + "/posts/{postId}/comments", 100L)
                        .param("sort", "createdAt")
                        .param("limit", "2")
                        .param("offset", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].text", contains("Comment 2", "Comment 3")));
    }

    @Test
    @DisplayName("GET /posts/{postId}/comments — 400 при неподдерживаемом поле сортировки")
    void getCommentsByPost_shouldReturn400_whenSortFieldUnsupported() throws Exception {
        mockMvc.perform(get(BASE_URL + "/posts/{postId}/comments", 100L)
                        .param("sort", "author:desc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid value 'author:desc' for parameter 'sort': "
                        + "unsupported field 'author'. Supported fields: createdAt, likes"));
    }

    @Test
    @DisplayName("GET /posts/{postId}/comments — 400 при неподдерживаемом направлении сортировки")
    void getCommentsByPost_shouldReturn400_whenSortDirectionUnsupported() throws Exception {
        mockMvc.perform(get(BASE_URL + "/posts/{postId}/comments", 100L)
                        .param("sort", "likes:up"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid value 'likes:up' for parameter 'sort': "
                        + "unsupported direction 'up'. Supported directions: asc, desc"));
    }

    @Test
    @DisplayName("GET /posts/{postId}/comments — 400 при повторе поля сортировки")
    void getCommentsByPost_shouldReturn400_whenSortFieldDuplicated() throws Exception {
        mockMvc.perform(get(BASE_URL + "/posts/{postId}/comments", 100L)
                        .param("sort", "likes:asc,likes:desc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid value 'likes:desc' for parameter 'sort': "
                        + "field 'likes' is specified more than once"));
    }

    // ============ GET /posts/{postId}/comments/count ============

    @Test
    @DisplayName("GET /posts/{postId}/comments/count — возвращает количество комментариев поста")
    void getCommentsCountByPost_shouldReturnCount() throws Exception {
        createCommentViaApi(1L, 100L, "First");
        createCommentViaApi(2L, 100L, "Second");
        Long deleted = createCommentViaApi(3L, 100L, "Deleted");
        createCommentViaApi(1L, 200L, "Other post");
        mockMvc.perform(delete(BASE_URL + "/comments/{commentId}", deleted)).andExpect(status().isNoContent());

        mockMvc.perform(get(BASE_URL + "/posts/{postId}/comments/count", 100L))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.postId").value(100))
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    @DisplayName("GET /posts/{postId}/comments/count — 0 для поста без комментариев")
    void getCommentsCountByPost_shouldReturnZero_forPostWithoutComments() throws Exception {
        mockMvc.perform(get(BASE_URL + "/posts/{postId}/comments/count", 999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(999999))
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    @DisplayName("GET /posts/{postId}/comments/count — 400 при нечисловом postId")
    void getCommentsCountByPost_shouldReturn400_whenPostIdNotNumeric() throws Exception {
        mockMvc.perform(get(BASE_URL + "/posts/{postId}/comments/count", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Invalid value 'abc' for parameter 'postId': expected an integer"));
    }

    // ============ Error responses ============

    @Test
    @DisplayName("Ошибки — сообщения на английском независимо от Accept-Language")
    void errors_shouldBeInEnglish_regardlessOfAcceptLanguage() throws Exception {
        mockMvc.perform(post(BASE_URL + "/comments")
                        .header("Accept-Language", "ru-RU")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request body: "
                        + "field 'author' must not be null; "
                        + "field 'postId' must not be null; "
                        + "field 'text' must not be null"));
    }

    @Test
    @DisplayName("POST /comments — 400 при слишком длинном тексте")
    void createComment_shouldReturn400_whenTextTooLong() throws Exception {
        CommentCreate request = new CommentCreate();
        request.setAuthor(1L);
        request.setPostId(100L);
        request.setText("a".repeat(1025));

        mockMvc.perform(post(BASE_URL + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Invalid request body: field 'text' size must be between 0 and 1024"));
    }

    @Test
    @DisplayName("POST /comments — 400 при некорректном JSON")
    void createComment_shouldReturn400_whenMalformedJson() throws Exception {
        mockMvc.perform(post(BASE_URL + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"author\": 1,"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request body: malformed JSON"));
    }

    @Test
    @DisplayName("POST /comments — 400 при неверном типе поля")
    void createComment_shouldReturn400_whenFieldHasWrongType() throws Exception {
        mockMvc.perform(post(BASE_URL + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"author": "abc", "postId": 100, "text": "Hi"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request body: field 'author' has an invalid value"));
    }

    @Test
    @DisplayName("POST /comments — 400 без тела запроса")
    void createComment_shouldReturn400_whenBodyMissing() throws Exception {
        mockMvc.perform(post(BASE_URL + "/comments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Request body is missing"));
    }

    @Test
    @DisplayName("POST /comments — 415 при неподдерживаемом Content-Type")
    void createComment_shouldReturn415_whenContentTypeUnsupported() throws Exception {
        mockMvc.perform(post(BASE_URL + "/comments")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error")
                        .value("Content type 'text/plain' is not supported. Use 'application/json'"));
    }

    @Test
    @DisplayName("GET /comments/{id} — 400 при нечисловом id")
    void getCommentById_shouldReturn400_whenIdNotNumber() throws Exception {
        mockMvc.perform(get(BASE_URL + "/comments/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Invalid value 'abc' for parameter 'commentId': expected an integer"));
    }

    @Test
    @DisplayName("PATCH /comments/{id} — 405 для неподдерживаемого метода")
    void unsupportedMethod_shouldReturn405() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/comments/{id}", 1L))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error", startsWith("HTTP method PATCH is not supported for this endpoint")));
    }

    @Test
    @DisplayName("Неизвестный endpoint — 404 с понятным сообщением")
    void unknownEndpoint_shouldReturn404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Endpoint GET /api/v1/unknown does not exist"));
    }
}
