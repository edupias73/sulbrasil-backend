package com.sulbrasil.catalogo.controller;

import com.sulbrasil.catalogo.dto.CatalogoContenidoDto;
import com.sulbrasil.catalogo.service.CatalogoContenidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/catalogo")
@RequiredArgsConstructor
public class CatalogoContenidoController {

    private final CatalogoContenidoService catalogoContenidoService;

    @GetMapping("/contenido")
    public ResponseEntity<CatalogoContenidoDto> obterContenido() {
        try {
            return ResponseEntity.ok(catalogoContenidoService.obterContenido());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
