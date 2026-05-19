package com.sulbrasil.catalogo.dto;

import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;

@Value
@Builder
public class ImportacaoResultado {

    int importados;
    int atualizados;
    @Builder.Default
    List<String> erros = new ArrayList<>();

    public int getTotalProcessados() {
        return importados + atualizados;
    }
}
