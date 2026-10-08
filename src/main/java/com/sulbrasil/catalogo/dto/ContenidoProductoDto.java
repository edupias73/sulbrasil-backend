package com.sulbrasil.catalogo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContenidoProductoDto {
    private String descripcion;
    private String imagen; // Mantemos para retrocompatibilidade
    private List<String> imagenes; // NOVA PROPRIEDADE PARA A GALERIA
}