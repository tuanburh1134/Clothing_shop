package com.example.shop.repository;

import com.example.shop.entity.Product;
import com.example.shop.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
	List<Product> findTop4ByOrderByPurchaseCountDescCreatedAtDesc();

	List<Product> findTop4ByOrderByCreatedAtDesc();

	List<Product> findAllByOrderByCreatedAtDesc();

	List<Product> findAllByCategoryOrderByCreatedAtDesc(ProductCategory category);
}
