package com.example.shop.service.impl;

import com.example.shop.dto.CheckoutItemView;
import com.example.shop.entity.CustomerOrder;
import com.example.shop.entity.OrderItem;
import com.example.shop.entity.OrderStatus;
import com.example.shop.entity.Product;
import com.example.shop.exception.BadRequestException;
import com.example.shop.repository.CustomerOrderRepository;
import com.example.shop.repository.ProductRepository;
import com.example.shop.service.OrderService;
import com.example.shop.service.ProductService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final ProductService productService;
    private final ProductRepository productRepository;
    private final CustomerOrderRepository customerOrderRepository;

    public OrderServiceImpl(ProductService productService,
                            ProductRepository productRepository,
                            CustomerOrderRepository customerOrderRepository) {
        this.productService = productService;
        this.productRepository = productRepository;
        this.customerOrderRepository = customerOrderRepository;
    }

    @Override
    public List<CheckoutItemView> buildCheckoutItems(List<Long> productIds, List<Integer> quantities) {
        List<CheckoutItemView> items = new ArrayList<>();
        for (int i = 0; i < productIds.size(); i++) {
            Long productId = productIds.get(i);
            int quantity = quantities.get(i);
            if (productId == null || quantity <= 0) {
                continue;
            }

            Product product = productService.getProductById(productId);
            CheckoutItemView item = new CheckoutItemView();
            item.setProductId(productId);
            item.setProductName(product.getName());
            item.setImageUrl(product.getImageUrl());
            item.setUnitPrice(product.getDiscountedPrice());
            item.setQuantity(quantity);
            item.setLineTotal(product.getDiscountedPrice().multiply(BigDecimal.valueOf(quantity)));
            items.add(item);
        }
        return items;
    }

    @Override
    public CustomerOrder createOrder(String username, String phoneNumber, String shippingAddress, List<CheckoutItemView> items) {
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("Không có sản phẩm để đặt hàng");
        }

        CustomerOrder order = new CustomerOrder();
        order.setUsername(username);
        order.setCustomerPhone(phoneNumber.trim());
        order.setShippingAddress(shippingAddress.trim());
        order.setStatus(OrderStatus.PENDING);

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CheckoutItemView itemView : items) {
            Product product = productService.getProductById(itemView.getProductId());
            int quantity = itemView.getQuantity();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImageUrl(product.getImageUrl());
            orderItem.setQuantity(quantity);
            orderItem.setUnitPrice(product.getDiscountedPrice());
            BigDecimal lineTotal = product.getDiscountedPrice().multiply(BigDecimal.valueOf(quantity));
            orderItem.setLineTotal(lineTotal);
            total = total.add(lineTotal);
            orderItems.add(orderItem);
        }

        order.setItems(orderItems);
        order.setTotalAmount(total);
        return customerOrderRepository.save(order);
    }

    @Override
    public List<CustomerOrder> getOrdersForUser(String username) {
        return customerOrderRepository.findAllByUsernameOrderByCreatedAtDesc(username);
    }

    @Override
    public List<CustomerOrder> getAllOrdersForAdmin() {
        return customerOrderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public CustomerOrder approveOrder(Long orderId) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn hàng"));

        order.setStatus(OrderStatus.APPROVED);
        order.setCancelReason(null);

        for (OrderItem item : order.getItems()) {
            Product product = productService.getProductById(item.getProductId());
            product.setPurchaseCount(product.getPurchaseCount() + item.getQuantity());
            productRepository.save(product);
        }

        return customerOrderRepository.save(order);
    }

    @Override
    public CustomerOrder cancelOrder(Long orderId, String cancelReason) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn hàng"));

        order.setStatus(OrderStatus.CANCELED);
        order.setCancelReason(cancelReason.trim());
        return customerOrderRepository.save(order);
    }
}
