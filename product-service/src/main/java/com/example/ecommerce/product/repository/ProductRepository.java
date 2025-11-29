package com.example.ecommerce.product.repository;

import com.example.ecommerce.product.Entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
