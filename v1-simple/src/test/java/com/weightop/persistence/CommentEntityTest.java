package com.weightop.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

class CommentEntityTest extends BaseRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("CreationTimestamp — автоматически устанавливается при создании")
    void createdAt_shouldBeSetAutomatically() {
        // given
        CommentEntity entity = new CommentEntity();
        entity.setAuthorId(1L);
        entity.setPostId(100L);
        entity.setText("Test");
        entity.setLikes(0);

        // when
        CommentEntity saved = entityManager.persistFlushFind(entity);

        // then
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("UpdateTimestamp — автоматически обновляется при изменении")
    void updatedAt_shouldBeSetOnUpdate() {
        // given
        CommentEntity entity = new CommentEntity();
        entity.setAuthorId(1L);
        entity.setPostId(100L);
        entity.setText("Original");
        entity.setLikes(0);

        CommentEntity saved = entityManager.persistFlushFind(entity);
        assertThat(saved.getUpdatedAt()).isNotNull();
        var oldUpdated = saved.getUpdatedAt();
        assertThat(oldUpdated).isEqualTo(saved.getCreatedAt());

        // when
        saved.setText("Updated");
        CommentEntity updated = entityManager.persistFlushFind(saved);

        // then
        assertThat(updated.getUpdatedAt()).isNotNull();
        assertThat(updated.getUpdatedAt()).isNotEqualTo(oldUpdated);
    }
}