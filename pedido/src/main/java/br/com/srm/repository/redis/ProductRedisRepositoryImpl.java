package br.com.srm.repository.redis;

import br.com.srm.client.dto.Product;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;

@Repository
public class ProductRedisRepositoryImpl implements ProductRedisRepository {

    private static final String HASH_NAME = "product";
    private RedisTemplate<String, Product> redisTemplate;
    private HashOperations hashOperations;

    public ProductRedisRepositoryImpl(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    private void init() {
        hashOperations = redisTemplate.opsForHash();
    }

    @Override
    public void saveProduct(Product product) {
        hashOperations.put(HASH_NAME, product.getId(), product);
    }

    @Override
    public void updateProduct(Product product) {
        hashOperations.put(HASH_NAME, product.getId(), product);
    }

    @Override
    public void deleteProduct(String barcCode) {
        hashOperations.delete(HASH_NAME, barcCode);
    }

    @Override
    public Product findProduct(String barCode) {
        return (Product) hashOperations.get(HASH_NAME, barCode);
    }
}
