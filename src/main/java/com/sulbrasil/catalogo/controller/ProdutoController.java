package com.sulbrasil.catalogo.controller;

import com.sulbrasil.catalogo.dto.ImportacaoResultado;
import com.sulbrasil.catalogo.entity.Produto;
import com.sulbrasil.catalogo.service.ProdutoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
public class ProdutoController {

    private final ProdutoService produtoService;

    @GetMapping("/buscar")
    public ResponseEntity<List<Produto>> buscar(
            @RequestParam(value = "q", required = false, defaultValue = "") String termo,
            @RequestParam(value = "categoria", required = false) String categoria) {
        return ResponseEntity.ok(produtoService.buscar(termo, categoria));
    }

    @PostMapping(value = "/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importar(@RequestParam("arquivo") MultipartFile arquivo) {
        try {
            ImportacaoResultado resultado = produtoService.importarCsv(arquivo);
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Falha ao ler o arquivo CSV."));
        }
    }
}
