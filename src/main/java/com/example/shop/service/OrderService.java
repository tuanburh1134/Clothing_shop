package com.example.shop.service;

import com.example.shop.dto.CheckoutItemView;
import com.example.shop.entity.CustomerOrder;

import java.util.List;

public interface OrderService {
    List<CheckoutItemView> buildCheckoutItems(List<Long> productIds, List<Integer> quantities);

    CustomerOrder createOrder(String username, String phoneNumber, String shippingAddress, List<CheckoutItemView> items);

    List<CustomerOrder> getOrdersForUser(String username);

    List<CustomerOrder> getAllOrdersForAdmin();

    CustomerOrder approveOrder(Long orderId);

    CustomerOrder cancelOrder(Long orderId, String cancelReason);
}
