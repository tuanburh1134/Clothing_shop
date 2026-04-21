package com.example.shop.repository;

import com.example.shop.entity.SupportChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportChatMessageRepository extends JpaRepository<SupportChatMessage, Long> {
    List<SupportChatMessage> findByUsernameOrderByCreatedAtAscIdAsc(String username);

    List<SupportChatMessage> findAllByOrderByCreatedAtDescIdDesc();
}
