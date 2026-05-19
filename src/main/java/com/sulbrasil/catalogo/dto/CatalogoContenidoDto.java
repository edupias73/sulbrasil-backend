package com.sulbrasil.catalogo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogoContenidoDto {

    @Builder.Default
    private Map<String, ContenidoSeccionDto> secciones = new HashMap<>();

    @Builder.Default
    private Map<String, ContenidoProductoDto> productos = new HashMap<>();
}
