package com.example.shop.service.impl;

import com.example.shop.entity.CustomerOrder;
import com.example.shop.entity.OrderItem;
import com.example.shop.entity.OrderStatus;
import com.example.shop.entity.ProductReview;
import com.example.shop.exception.BadRequestException;
import com.example.shop.repository.CustomerOrderRepository;
import com.example.shop.repository.ProductReviewRepository;
import com.example.shop.service.ReviewService;
import com.example.shop.util.ImageStorageUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ProductReviewRepository productReviewRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final ImageStorageUtil imageStorageUtil;

    public ReviewServiceImpl(ProductReviewRepository productReviewRepository,
                             CustomerOrderRepository customerOrderRepository,
                             ImageStorageUtil imageStorageUtil) {
        this.productReviewRepository = productReviewRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.imageStorageUtil = imageStorageUtil;
    }

    @Override
    public void createReview(String username,
                             Long orderId,
                             Long orderItemId,
                             Long productId,
                             Integer rating,
                             String comment,
                             MultipartFile[] imageFiles) {
        if (orderId == null || orderItemId == null || productId == null) {
            throw new BadRequestException("Thông tin đánh giá không hợp lệ");
        }

        int normalizedRating = rating == null ? 0 : rating;
        if (normalizedRating < 1 || normalizedRating > 5) {
            throw new BadRequestException("Vui lòng chọn số sao từ 1 đến 5");
        }

        String normalizedComment = comment == null ? "" : comment.trim();
        if (normalizedComment.isBlank()) {
            throw new BadRequestException("Vui lòng nhập bình luận đánh giá");
        }

        if (productReviewRepository.existsByOrderItemIdAndUsername(orderItemId, username)) {
            throw new BadRequestException("Bạn đã đánh giá sản phẩm này trong đơn hàng này");
        }

        CustomerOrder order = customerOrderRepository.findByIdAndUsername(orderId, username)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn hàng để đánh giá"));

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BadRequestException("Chỉ có thể đánh giá đơn đã giao");
        }

        OrderItem targetItem = null;
        for (OrderItem item : order.getItems()) {
            if (orderItemId.equals(item.getId()) && productId.equals(item.getProductId())) {
                targetItem = item;
                break;
            }
        }

        if (targetItem == null) {
            throw new BadRequestException("Không tìm thấy sản phẩm trong đơn để đánh giá");
        }

        ProductReview review = new ProductReview();
        review.setOrderId(orderId);
        review.setOrderItemId(orderItemId);
        review.setProductId(productId);
        review.setUsername(username);
        review.setRating(normalizedRating);
        review.setComment(normalizedComment);
        review.setImageUrls(imageStorageUtil.storeReviewImages(imageFiles));

        productReviewRepository.save(review);
    }

    @Override
    public List<ProductReview> getReviewsForProduct(Long productId) {
        return productReviewRepository.findAllByProductIdOrderByCreatedAtDesc(productId);
    }

    @Override
    public Set<Long> getReviewedOrderItemIds(String username) {
        List<ProductReview> reviews = productReviewRepository.findAllByUsernameOrderByCreatedAtDesc(username);
        Set<Long> ids = new LinkedHashSet<>();
        for (ProductReview review : reviews) {
            if (review.getOrderItemId() != null) {
                ids.add(review.getOrderItemId());
            }
        }
        return ids;
    }
}
