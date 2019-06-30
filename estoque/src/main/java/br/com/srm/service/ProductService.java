package br.com.srm.service;

import br.com.srm.event.SimpleSourceBean;
import br.com.srm.exception.BusinessServiceException;
import br.com.srm.model.ProductEntity;
import br.com.srm.repository.ProductRepository;
import br.com.srm.utils.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RefreshScope
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Value("${srm.message.fail}")
    private String messageFail;

    @Autowired
    private SimpleSourceBean simpleSourceBean;

    @Transactional
    public ProductEntity save(ProductEntity product) {
        log.info("m=save, product={}", product);
        if (productRepository.findByIsbn(product.getIsbn()) != null)
            throw new BusinessServiceException("Já existe um produto com esse codigo ISBN");
        ProductEntity result = productRepository.save(product);
        simpleSourceBean.publishProductChange("ADD", result.getIsbn());
        return result;
    }

    public void delete(String isbn) {
        log.info("m=delete, isbn={}", isbn);
        productRepository.deleteById(isbn);
        simpleSourceBean.publishProductChange("DELETE", isbn);
    }

    public ProductEntity addAmount(String isbn, Integer amount) {
        log.info("m=addAmount, isbn={}, amount={}", isbn, amount);
        ProductEntity product = findByISBN(isbn);
        product.setAmount(product.getAmount() + amount);
        ProductEntity result = productRepository.save(product);
        simpleSourceBean.publishProductChange("UPDATE", result.getIsbn());
        return result;
    }

    public ProductEntity subtractAmount(String isbn, Integer amount) {
        log.info("m=subtractAmount, isbn={}, amount={}", isbn, amount);
        ProductEntity product = findByISBN(isbn);
        if (product.getAmount() < amount)
            throw new BusinessServiceException(messageFail);
        product.setAmount(product.getAmount() - amount);
        ProductEntity result = productRepository.save(product);
        simpleSourceBean.publishProductChange("UPDATE", result.getIsbn());
        return result;
    }

    public ProductEntity findByISBN(String isbn) {
        log.info("m=findByISBN, idbn={}, correlationId={}", isbn, UserContextHolder.getContext().getCorrelationId());
        Optional<ProductEntity> product = productRepository.findById(isbn);
        if (product.isPresent())
            return product.get();
        return null;
    }

}
