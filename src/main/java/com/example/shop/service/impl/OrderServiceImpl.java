package com.example.shop.service.impl;

import com.example.shop.dto.CheckoutItemInput;
import com.example.shop.dto.CheckoutItemView;
import com.example.shop.dto.ProductVariant;
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
import java.util.Locale;
import java.util.StringJoiner;

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
    public List<CheckoutItemView> buildCheckoutItems(List<CheckoutItemInput> inputs) {
        List<CheckoutItemView> items = new ArrayList<>();
        for (CheckoutItemInput input : inputs) {
            Long productId = input.getProductId();
            int quantity = input.getQuantity();
            if (productId == null || quantity <= 0) {
                continue;
            }

            Product product = productService.getProductById(productId);
            String selectedColor = clean(input.getColor());
            String selectedSize = normalizeSize(input.getSize());

            int available = findAvailableStock(product, selectedColor, selectedSize);
            if (available <= 0) {
                throw new BadRequestException("Màu hoặc size đã chọn không còn hàng");
            }

            CheckoutItemView item = new CheckoutItemView();
            item.setProductId(productId);
            item.setProductName(product.getName());
            item.setImageUrl(product.getImageUrl());
            item.setColor(selectedColor);
            item.setSize(selectedSize);
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
            orderItem.setSelectedColor(itemView.getColor());
            orderItem.setSelectedSize(itemView.getSize());
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

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể duyệt đơn đang chờ duyệt");
        }

        order.setStatus(OrderStatus.APPROVED);
        order.setCancelReason(null);

        for (OrderItem item : order.getItems()) {
            Product product = productService.getProductById(item.getProductId());
            deductVariantStock(product, item.getSelectedColor(), item.getSelectedSize(), item.getQuantity());
            product.setPurchaseCount(product.getPurchaseCount() + item.getQuantity());
            productRepository.save(product);
        }

        return customerOrderRepository.save(order);
    }

    @Override
    public CustomerOrder cancelOrder(Long orderId, String cancelReason) {
        CustomerOrder order = customerOrderRepository.findById(orderId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn hàng"));

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("Không thể hủy đơn đã giao");
        }

        order.setStatus(OrderStatus.CANCELED);
        order.setCancelReason(cancelReason.trim());
        return customerOrderRepository.save(order);
    }

    @Override
    public CustomerOrder cancelOrderByUser(String username, Long orderId, String cancelReason) {
        if (cancelReason == null || cancelReason.trim().isEmpty()) {
            throw new BadRequestException("Vui lòng nhập lí do hủy đơn");
        }

        CustomerOrder order = customerOrderRepository.findByIdAndUsername(orderId, username)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn hàng"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Bạn chỉ có thể hủy đơn đang chờ xác nhận");
        }

        order.setStatus(OrderStatus.CANCELED);
        order.setCancelReason(cancelReason.trim());
        return customerOrderRepository.save(order);
    }

    @Override
    public CustomerOrder markOrderDeliveredByUser(String username, Long orderId) {
        CustomerOrder order = customerOrderRepository.findByIdAndUsername(orderId, username)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn hàng"));

        if (order.getStatus() != OrderStatus.APPROVED) {
            throw new BadRequestException("Chỉ có thể xác nhận nhận hàng cho đơn chờ lấy hàng");
        }

        order.setStatus(OrderStatus.DELIVERED);
        order.setCancelReason(null);
        return customerOrderRepository.save(order);
    }

    private int findAvailableStock(Product product, String color, String size) {
        if (color == null || size == null) {
            return 0;
        }

        for (ProductVariant variant : product.getVariants()) {
            if (variant.getColor() == null || !variant.getColor().trim().equalsIgnoreCase(color)) {
                continue;
            }

            return switch (size) {
                case "S" -> value(variant.getS());
                case "M" -> value(variant.getM());
                case "L" -> value(variant.getL());
                case "XL" -> value(variant.getXl());
                case "XXL" -> value(variant.getXxl());
                default -> 0;
            };
        }

        return 0;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeSize(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return null;
        }
        return cleaned.toUpperCase(Locale.ROOT);
    }

    private int value(Integer number) {
        return number == null ? 0 : number;
    }

    private void deductVariantStock(Product product, String color, String size, int requestedQuantity) {
        if (requestedQuantity <= 0) {
            throw new BadRequestException("Số lượng mua không hợp lệ");
        }

        String normalizedColor = clean(color);
        String normalizedSize = normalizeSize(size);
        if (normalizedColor == null || normalizedSize == null) {
            throw new BadRequestException("Đơn hàng thiếu thông tin màu hoặc size");
        }

        List<ProductVariant> variants = product.getVariants();
        ProductVariant target = null;

        for (ProductVariant variant : variants) {
            if (variant.getColor() != null && variant.getColor().trim().equalsIgnoreCase(normalizedColor)) {
                target = variant;
                break;
            }
        }

        if (target == null) {
            throw new BadRequestException("Sản phẩm " + product.getName() + " không còn màu đã chọn");
        }

        int current = getSizeQuantity(target, normalizedSize);
        if (current < requestedQuantity) {
            throw new BadRequestException("Sản phẩm " + product.getName() + " không đủ tồn kho cho size " + normalizedSize + "");
        }

        setSizeQuantity(target, normalizedSize, current - requestedQuantity);
        product.setVariantData(serializeVariants(variants));
        product.setQuantity(calculateTotalQuantity(variants));
    }

    private int getSizeQuantity(ProductVariant variant, String size) {
        return switch (size) {
            case "S" -> value(variant.getS());
            case "M" -> value(variant.getM());
            case "L" -> value(variant.getL());
            case "XL" -> value(variant.getXl());
            case "XXL" -> value(variant.getXxl());
            default -> throw new BadRequestException("Size không hợp lệ");
        };
    }

    private void setSizeQuantity(ProductVariant variant, String size, int quantity) {
        switch (size) {
            case "S" -> variant.setS(quantity);
            case "M" -> variant.setM(quantity);
            case "L" -> variant.setL(quantity);
            case "XL" -> variant.setXl(quantity);
            case "XXL" -> variant.setXxl(quantity);
            default -> throw new BadRequestException("Size không hợp lệ");
        }
    }

    private String serializeVariants(List<ProductVariant> variants) {
        StringJoiner joiner = new StringJoiner(";;");

        for (ProductVariant variant : variants) {
            String color = clean(variant.getColor());
            if (color == null) {
                continue;
            }

            String sizeBlock = "S=" + value(variant.getS())
                    + ",M=" + value(variant.getM())
                    + ",L=" + value(variant.getL())
                    + ",XL=" + value(variant.getXl())
                    + ",XXL=" + value(variant.getXxl());

            joiner.add(color + "::" + sizeBlock);
        }

        String serialized = joiner.toString();
        return serialized.isBlank() ? null : serialized;
    }

    private int calculateTotalQuantity(List<ProductVariant> variants) {
        int total = 0;
        for (ProductVariant variant : variants) {
            total += value(variant.getS());
            total += value(variant.getM());
            total += value(variant.getL());
            total += value(variant.getXl());
            total += value(variant.getXxl());
        }
        return total;
    }
}
