package br.com.srm.event;

import br.com.srm.utils.UserContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class SimpleSourceBean {

    private CustomChannels customChannels;

    @Autowired
    public SimpleSourceBean(CustomChannels customChannels){
        this.customChannels = customChannels;
    }

    public void publishProductChange(String action, String barCode) {
        ProductChangeModel productChangeModel = new ProductChangeModel(
                ProductChangeModel.class.getTypeName(),
                action,
                barCode,
                UserContextHolder.getContext().getCorrelationId());

        customChannels.changeProduct().
                send(MessageBuilder.withPayload(productChangeModel)
                        .build());
    }

}
