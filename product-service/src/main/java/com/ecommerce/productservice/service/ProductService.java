package com.ecommerce.productservice.service;

import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.entity.ProductStatus;
import com.ecommerce.productservice.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // ADMIN
    public Product createProduct(Product product) {
        product.setStatus(ProductStatus.ACTIVE);
        return productRepository.save(product);
    }

    // ADMIN
    public Product updateProduct(Long id, Product updated) {
        Product existing = getProductById(id);
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setPrice(updated.getPrice());
        existing.setQuantity(updated.getQuantity());
        existing.setCategory(updated.getCategory());
        return productRepository.save(existing);
    }

    // ADMIN (soft delete)
    public void deactivateProduct(Long id) {
        Product product = getProductById(id);
        product.setStatus(ProductStatus.INACTIVE);
        productRepository.save(product);
    }

    // USER / ADMIN
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    // USER / ADMIN
    public Page<Product> getProducts(String category, Pageable pageable) {
        if (category != null) {
            return productRepository.findByCategoryAndStatus(category, ProductStatus.ACTIVE, pageable);
        }
        return productRepository.findByStatus(ProductStatus.ACTIVE, pageable);
    }

    // INTERNAL (Order Service)
    public void reduceStock(Long productId, int quantity) {
        Product product = getProductById(productId);

        if (product.getQuantity() < quantity) {
            throw new RuntimeException("Insufficient stock");
        }

        product.setQuantity(product.getQuantity() - quantity);
        productRepository.save(product);
    }
}
