package com.weightop.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<CommentEntity, Long>,
        PagingAndSortingRepository<CommentEntity, Long> {

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE comments SET likes = likes + 1 WHERE id = :commentId
            """, nativeQuery = true)
    int incrementLikes(@Param("commentId") Long commentId);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE comments SET likes = likes - 1 WHERE id = :commentId AND likes > 0
            """, nativeQuery = true)
    int decrementLikes(@Param("commentId") Long commentId);

    Page<CommentEntity> findAllByPostId(Long postId, Pageable pageRequest);
}
