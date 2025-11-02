package com.example.ecommerce.product.service;

import com.example.ecommerce.product.dto.ProductDTO;
import com.example.ecommerce.product.model.Category;
import com.example.ecommerce.product.model.Product;
import com.example.ecommerce.product.repository.CategoryRepository;
import com.example.ecommerce.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Autowired
    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public ProductDTO createProduct(@Valid ProductDTO productDTO) {
        Product product = new Product();
        updateProductFromDTO(product, productDTO);
        return toDTO(productRepository.save(product));
    }

    public ProductDTO getProductById(Long id) {
        return toDTO(productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id)));
    }

    public Page<ProductDTO> getAllProducts(String categoryName, int page, int size, String sortBy, String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<Product> productPage;
        if (categoryName != null && !categoryName.isEmpty()) {
            productPage = productRepository.findByCategoryId(
                    getCategoryByName(categoryName).getId(),
                    pageRequest
            );
        } else {
            productPage = productRepository.findAll(pageRequest);
        }

        return productPage.map(this::toDTO);
    }

    public Page<ProductDTO> searchProducts(String query, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return productRepository.searchProducts(query, pageRequest).map(this::toDTO);
    }

    public Page<ProductDTO> getAvailableProducts(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return productRepository.findAvailableProducts(pageRequest).map(this::toDTO);
    }

    public ProductDTO updateProduct(Long id, @Valid ProductDTO productDTO) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));
        updateProductFromDTO(existingProduct, productDTO);
        return toDTO(productRepository.save(existingProduct));
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new EntityNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    @Transactional
    public ProductDTO updateStock(Long id, int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));
        product.setStockQuantity(quantity);
        return toDTO(productRepository.save(product));
    }

    private void updateProductFromDTO(Product product, ProductDTO dto) {
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStockQuantity(dto.getStockQuantity());
        product.setImageUrl(dto.getImageUrl());
        product.setActive(dto.getActive() != null ? dto.getActive() : true);

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + dto.getCategoryId()));
            product.setCategory(category);
        }
    }

    private ProductDTO toDTO(Product product) {
        return ProductDTO.builder()
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .imageUrl(product.getImageUrl())
                .active(product.getActive())
                .build();
    }

    private Category getCategoryByName(String categoryName) {
        return categoryRepository.findByName(categoryName)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with name: " + categoryName));
    }
}
