package com.sulbrasil.catalogo.controller;

import com.sulbrasil.catalogo.entity.Produto;
import com.sulbrasil.catalogo.service.ProdutoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProdutoController {

    private final ProdutoService produtoService;

    // A loja agora pede por "páginas" e o backend só envia 20 peças de cada vez
    @GetMapping("/buscar")
    public ResponseEntity<Page<Produto>> buscar(
            @RequestParam(value = "q", required = false, defaultValue = "") String termo,
            @RequestParam(value = "categoria", required = false) String categoria,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("nomePeca").ascending());
        return ResponseEntity.ok(produtoService.buscarPaginado(termo, categoria, pageRequest));
    }

    @PostMapping("/manual")
    public ResponseEntity<Produto> salvarManual(@RequestBody Produto produto) {
        return ResponseEntity.ok(produtoService.salvarManual(produto));
    }
}