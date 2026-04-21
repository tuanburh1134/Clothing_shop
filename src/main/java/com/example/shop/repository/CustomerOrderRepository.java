package com.example.shop.repository;

import com.example.shop.entity.CustomerOrder;
import com.example.shop.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findAllByUsernameOrderByCreatedAtDesc(String username);

    List<CustomerOrder> findAllByOrderByCreatedAtDesc();

    Optional<CustomerOrder> findByIdAndUsername(Long id, String username);

    long countByUsernameAndStatus(String username, OrderStatus status);
}
