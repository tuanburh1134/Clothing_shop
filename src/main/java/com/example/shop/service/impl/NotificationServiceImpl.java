package com.example.shop.service.impl;

import com.example.shop.entity.AppNotification;
import com.example.shop.entity.NotificationRecipientType;
import com.example.shop.repository.AppNotificationRepository;
import com.example.shop.service.NotificationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final AppNotificationRepository appNotificationRepository;

    public NotificationServiceImpl(AppNotificationRepository appNotificationRepository) {
        this.appNotificationRepository = appNotificationRepository;
    }

    @Override
    public void notifyAdminNewOrder(Long orderId, String username) {
        AppNotification notification = new AppNotification();
        notification.setRecipientType(NotificationRecipientType.ADMIN);
        notification.setTitle("Đơn hàng mới #" + orderId);
        notification.setMessage("Khách hàng " + username + " vừa đặt đơn hàng mới.");
        notification.setLinkUrl("/admin/orders");
        notification.setRead(false);
        appNotificationRepository.save(notification);
    }

    @Override
    public void notifyUserOrderApproved(String username, Long orderId) {
        AppNotification notification = new AppNotification();
        notification.setRecipientType(NotificationRecipientType.USER);
        notification.setRecipientUsername(username);
        notification.setTitle("Đơn hàng #" + orderId + " đã được duyệt");
        notification.setMessage("Đơn hàng của bạn đã được duyệt, bạn sẽ nhận được hàng trong thời gian sớm nhất.");
        notification.setLinkUrl("/account/orders");
        notification.setRead(false);
        appNotificationRepository.save(notification);
    }

    @Override
    public void notifyUserOrderCanceled(String username, Long orderId, String cancelReason) {
        AppNotification notification = new AppNotification();
        notification.setRecipientType(NotificationRecipientType.USER);
        notification.setRecipientUsername(username);
        notification.setTitle("Đơn hàng #" + orderId + " đã bị hủy");
        notification.setMessage("Lí do hủy: " + cancelReason);
        notification.setLinkUrl("/account/orders");
        notification.setRead(false);
        appNotificationRepository.save(notification);
    }

    @Override
    public List<AppNotification> getAdminNotifications() {
        return appNotificationRepository.findAllByRecipientTypeOrderByCreatedAtDesc(NotificationRecipientType.ADMIN);
    }

    @Override
    public List<AppNotification> getUserNotifications(String username) {
        return appNotificationRepository.findAllByRecipientTypeAndRecipientUsernameOrderByCreatedAtDesc(
                NotificationRecipientType.USER,
                username
        );
    }
}
