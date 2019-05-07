package br.com.srm.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductChangeModel {

    private String type;
    private String action;
    private String barCode;
    private String correlationId;

}
