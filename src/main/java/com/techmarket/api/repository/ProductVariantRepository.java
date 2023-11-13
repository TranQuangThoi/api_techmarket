package com.techmarket.api.repository;

import com.techmarket.api.model.ProductVariant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long>, JpaSpecificationExecutor<ProductVariant> {

    @Query("SELECT p FROM ProductVariant p WHERE LOWER(p.color) = LOWER(:color) AND p.product.id = :id")
    ProductVariant findByColorAndProductId(String color, Long id);

    Page<ProductVariant> findAllByProductId(Long id, Pageable pageable);
}
