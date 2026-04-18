package com.example.shop.repository;

import com.example.shop.entity.AppNotification;
import com.example.shop.entity.NotificationRecipientType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {
    List<AppNotification> findAllByRecipientTypeOrderByCreatedAtDesc(NotificationRecipientType recipientType);

    List<AppNotification> findAllByRecipientTypeAndRecipientUsernameOrderByCreatedAtDesc(
            NotificationRecipientType recipientType,
            String recipientUsername
    );
}
