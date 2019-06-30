package br.com.srm.event;

import br.com.srm.repository.redis.ProductRedisRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;

@Slf4j
@EnableBinding(CustomChannels.class)
public class ProductChangeHandler {

    @Autowired
    private ProductRedisRepository productRedisRepository;

    @StreamListener("changeProduct")
    public void receiveChangeProduct(ProductChangeModel productChange) {
        log.info("m=receiveChangeProduct, productChange={}", productChange);

        switch(productChange.getAction()){
            case "GET":
                log.debug("Evento GET para produto {}", productChange.getIsbn());
                break;
            case "SAVE":
                log.debug("Evento SAVE para produto {}", productChange.getIsbn());
                break;
            case "UPDATE":
                log.debug("Evento UPDATE para produto {}", productChange.getIsbn());
                productRedisRepository.deleteProduct(productChange.getIsbn());
                break;
            case "DELETE":
                log.debug("Evento DELETE para produto {}", productChange.getIsbn());
                productRedisRepository.deleteProduct(productChange.getIsbn());
                break;
            default:
                log.error("Evento desconhecido para o produto {}", productChange.getType());
                break;
        }
    }
}
