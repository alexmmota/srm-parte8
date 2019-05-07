package br.com.srm.event;

import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

public interface CustomChannels {

    @Output("changeProduct")
    MessageChannel changeProduct();

}
