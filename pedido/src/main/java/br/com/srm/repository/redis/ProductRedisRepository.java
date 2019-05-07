package br.com.srm.repository.redis;

import br.com.srm.client.dto.Product;

public interface ProductRedisRepository {

    void saveProduct(Product product);
    void updateProduct(Product product);
    void deleteProduct(String barcCode);
    Product findProduct(String barCode);

}
