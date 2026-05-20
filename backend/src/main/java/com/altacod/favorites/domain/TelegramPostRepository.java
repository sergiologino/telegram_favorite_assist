package com.altacod.favorites.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TelegramPostRepository extends JpaRepository<TelegramPost, Long> {
    Optional<TelegramPost> findByTelegramMessageId(Long telegramMessageId);

    List<TelegramPost> findByStatusOrderByPostedAtDesc(PostStatus status, Pageable pageable);

    long countByStatus(PostStatus status);
}
