package br.com.srm.service;

import br.com.srm.client.EstoqueClient;
import br.com.srm.exception.BusinessServiceException;
import br.com.srm.model.Order;
import br.com.srm.model.OrderItem;
import br.com.srm.client.dto.Product;
import br.com.srm.repository.OrderRepository;
import br.com.srm.repository.redis.ProductRedisRepository;
import br.com.srm.utils.UserContext;
import br.com.srm.utils.UserContextHolder;
import brave.Span;
import brave.Tracer;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OrderService {

    private static Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EstoqueClient estoqueClient;

    @Autowired
    private ProductRedisRepository productRedisRepository;

    @Autowired
    private Tracer tracer;

    @HystrixCommand(commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "5000")})
    public Order create(Order order) {
        logger.info("m=create, order={}, correlationId={}", order, UserContextHolder.getContext().getCorrelationId());
        validateItensExists(order);
        order.setCreateDate(new Date());
        order.setStatus(Order.Status.CREATED);
        return orderRepository.save(order);
    }

    @HystrixCommand
    public Order findById(String id) {
        return getOrderById(id);
    }

    @HystrixCommand(commandProperties = {
            @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3000")},
            threadPoolProperties = {
                    @HystrixProperty(name = "coreSize", value = "3")
            },
            fallbackMethod = "fallBackFindByClient")
    public List<Order> findByClient(String cpf) {
        return orderRepository.findByClient_CpfOrderByCreateDateAsc(cpf);
    }

    public List<Order> fallBackFindByClient(String cpf) {
        return Collections.EMPTY_LIST;
    }

    public Order finish(String id) {
        logger.info("m=finish, id={}", id);
        Order order = getOrderById(id);
        validateItens(order);
        order.setFinishDate(new Date());
        order.setStatus(Order.Status.FINISHED);
        updateProductAmount(order);
        return orderRepository.save(order);
    }

    public Order cancel(String id) {
        logger.info("m=cancel, id={}", id);
        Order order = getOrderById(id);
        order.setFinishDate(new Date());
        order.setStatus(Order.Status.CANCELED);
        return orderRepository.save(order);
    }

    private void validateItens(Order order) {
        for (OrderItem item : order.getItens()) {
            Product product = findProductByIsbn(item);
            if (product.getAmount() < item.getAmount())
                throw new BusinessServiceException("Quantidade de produto insuficiente no estoque");
        }
    }

    @HystrixCommand(threadPoolKey = "productByIsbnThreadPool",
            threadPoolProperties = {
                @HystrixProperty(name = "coreSize",value="30"),
                @HystrixProperty(name="maxQueueSize", value="10")})
    private Product findProductByIsbn(OrderItem item) {
        Product product = checkRedisCache(item.getProduct());
        if (product != null)
            return product;

        product = estoqueClient.findByIsbn(UserContextHolder.getContext().getAuthToken(),
                UserContextHolder.getContext().getCorrelationId(),1l, item.getProduct());
        cacheProductObject(product);
        return product;
    }

    private void sleep() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void updateProductAmount(Order order) {
        for (OrderItem item : order.getItens()) {
            subtractProductAmount(item);
        }
    }

    @HystrixCommand
    private void subtractProductAmount(OrderItem item) {
        estoqueClient.subtractAmount(UserContextHolder.getContext().getAuthToken(),
                UserContextHolder.getContext().getCorrelationId(),
                1l, item.getProduct(), item.getAmount());
    }

    private void validateItensExists(Order order) {
        for (OrderItem item : order.getItens()) {
            Product product = findProductByIsbn(item);
            if (product == null)
                throw new BusinessServiceException("Produto nao encontrado");
        }
    }

    private Order getOrderById(String id) {
        Optional<Order> optionalOrder = orderRepository.findById(id);
        if (!optionalOrder.isPresent())
            throw new BusinessServiceException("Pedido não encontrado");
        return optionalOrder.get();
    }

    private Product checkRedisCache(String isbn) {
        Span newSpan = tracer.nextSpan().name("readProductDataFromRedis");
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(newSpan.start())) {
            return productRedisRepository.findProduct(isbn);
        } catch (Exception ex){
            logger.error("Falha ao buscar {}.  Exception {}", isbn, ex);
            return null;
        } finally {
            newSpan.finish();
        }
    }

    private void cacheProductObject(Product product) {
        try {
            productRedisRepository.saveProduct(product);
        }catch (Exception ex){
            logger.error("Falha ao salvar {} no Redis. Exception {}", product.getIsbn(), ex);
        }
    }

}
