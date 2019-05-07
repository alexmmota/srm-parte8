package br.com.srm.event;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.SubscribableChannel;

public interface CustomChannels {

    @Input("changeProduct")
    SubscribableChannel changeProduct();

}
