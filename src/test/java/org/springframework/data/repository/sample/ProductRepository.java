package org.springframework.data.repository.sample;

import org.springframework.data.repository.Repository;

public interface ProductRepository extends Repository<Product, Long> {

	Product findOne(Long id);

	Product save(Product product);
}
