package br.com.srm.event;

import br.com.srm.repository.redis.ProductRedisRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;

@EnableBinding(CustomChannels.class)
public class ProductChangeHandler {

    private static Logger logger = LoggerFactory.getLogger(ProductChangeHandler.class);

    @Autowired
    private ProductRedisRepository productRedisRepository;

    @StreamListener("changeProduct")
    public void receiveChangeProduct(ProductChangeModel productChange) {
        logger.info("m=receiveChangeProduct, productChange={}", productChange);

        switch(productChange.getAction()){
            case "GET":
                logger.debug("Evento GET para produto {}", productChange.getBarCode());
                break;
            case "SAVE":
                logger.debug("Evento SAVE para produto {}", productChange.getBarCode());
                break;
            case "UPDATE":
                logger.debug("Evento UPDATE para produto {}", productChange.getBarCode());
                productRedisRepository.deleteProduct(productChange.getBarCode());
                break;
            case "DELETE":
                logger.debug("Evento DELETE para produto {}", productChange.getBarCode());
                productRedisRepository.deleteProduct(productChange.getBarCode());
                break;
            default:
                logger.error("Evento desconhecido para o produto {}", productChange.getType());
                break;
        }
    }
}
