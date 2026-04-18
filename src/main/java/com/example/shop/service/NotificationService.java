package com.example.shop.service;

import com.example.shop.entity.AppNotification;

import java.util.List;

public interface NotificationService {
    void notifyAdminNewOrder(Long orderId, String username);

    void notifyUserOrderApproved(String username, Long orderId);

    void notifyUserOrderCanceled(String username, Long orderId, String cancelReason);

    List<AppNotification> getAdminNotifications();

    List<AppNotification> getUserNotifications(String username);
}
