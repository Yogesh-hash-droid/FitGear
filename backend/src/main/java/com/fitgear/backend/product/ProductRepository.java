package com.fitgear.backend.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

	Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
			String name,
			String description,
			Pageable pageable
	);

	Page<Product> findByCategoryIgnoreCase(String category, Pageable pageable);

	@Query("SELECT p FROM Product p WHERE lower(p.category) = lower(:category) " +
			"AND (lower(p.name) LIKE lower(concat('%', :term, '%')) OR lower(p.description) LIKE lower(concat('%', :term, '%'))) ")
	Page<Product> findByCategoryAndTerm(
			@Param("category") String category,
			@Param("term") String term,
			Pageable pageable
	);
}
