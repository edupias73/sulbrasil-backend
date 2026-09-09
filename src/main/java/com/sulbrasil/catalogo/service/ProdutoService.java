package com.sulbrasil.catalogo.service;

import com.sulbrasil.catalogo.dto.ImportacaoResultado;
import com.sulbrasil.catalogo.entity.Produto;
import com.sulbrasil.catalogo.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProdutoService {

    private static final int COL_CODIGO = 0;
    private static final int COL_NOME = 1;
    private static final int COL_MARCA = 2;
    private static final int COL_PRECO = 3;
    private static final int COL_ESTOQUE = 4;
    private static final int COL_CATEGORIA = 5;

    private final ProdutoRepository produtoRepository;

    @Transactional(readOnly = true)
    public List<Produto> buscar(String termo, String categoria) {
        List<Produto> produtos;
        boolean temTermo = termo != null && !termo.isBlank();
        boolean temCategoria = categoria != null && !categoria.isBlank();

        if (temCategoria && temTermo) {
            produtos = produtoRepository.findByCategoriaIgnoreCaseAndTermosBuscaContainingIgnoreCase(
                    categoria.trim(), termo.trim());
        } else if (temCategoria) {
            produtos = produtoRepository.findByCategoriaIgnoreCaseOrderByNomePecaAsc(categoria.trim());
        } else if (temTermo) {
            produtos = produtoRepository.findByTermosBuscaContainingIgnoreCase(termo.trim());
        } else {
            produtos = List.of();
        }

        inicializarColecoes(produtos);
        return produtos;
    }

    private void inicializarColecoes(List<Produto> produtos) {
        produtos.forEach(p -> {
            p.getCodigosCruzados().size();
            p.getAplicacoesVeiculo().size();
        });
    }


    @Transactional
    public Produto salvarManual(Produto produto) {
        Optional<Produto> existente = produtoRepository.findByCodigoInterno(produto.getCodigoInterno());

        if (existente.isPresent()) {
            Produto p = existente.get();
            p.setNomePeca(produto.getNomePeca());
            p.setMarcaPrincipal(produto.getMarcaPrincipal());
            p.setPreco(produto.getPreco());
            p.setQuantidadeEstoque(produto.getQuantidadeEstoque());
            if (produto.getCategoria() != null) {
                p.setCategoria(produto.getCategoria());
            }
            return produtoRepository.save(p);
        }

        return produtoRepository.save(produto);
    }

    @Transactional
    public ImportacaoResultado importarCsv(MultipartFile arquivo) throws IOException {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Arquivo CSV não enviado ou vazio.");
        }

        int importados = 0;
        int atualizados = 0;
        List<String> erros = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(arquivo.getInputStream(), StandardCharsets.UTF_8))) {

            String linha;
            int numeroLinha = 0;

            while ((linha = reader.readLine()) != null) {
                numeroLinha++;
                linha = linha.trim();
                if (linha.isEmpty()) {
                    continue;
                }

                if (numeroLinha == 1 && isCabecalho(linha)) {
                    continue;
                }

                try {
                    String[] colunas = linha.split(",", -1);
                    if (colunas.length < 5) {
                        erros.add("Linha " + numeroLinha + ": esperadas ao menos 5 colunas (codigo_interno, nome_peca, marca_principal, preco, estoque, [categoria]).");
                        continue;
                    }

                    String codigoInterno = colunas[COL_CODIGO].trim();
                    String nomePeca = colunas[COL_NOME].trim();
                    String marcaPrincipal = colunas[COL_MARCA].trim();
                    BigDecimal preco = parsePreco(colunas[COL_PRECO].trim());
                    Integer estoque = Integer.parseInt(colunas[COL_ESTOQUE].trim());
                    String categoria = colunas.length > COL_CATEGORIA ? colunas[COL_CATEGORIA].trim() : null;
                    if (categoria != null && categoria.isBlank()) {
                        categoria = null;
                    }

                    if (codigoInterno.isBlank() || nomePeca.isBlank()) {
                        erros.add("Linha " + numeroLinha + ": codigo_interno e nome_peca são obrigatórios.");
                        continue;
                    }

                    Optional<Produto> existente = produtoRepository.findByCodigoInterno(codigoInterno);
                    Produto produto;

                    if (existente.isPresent()) {
                        produto = existente.get();
                        produto.setNomePeca(nomePeca);
                        produto.setMarcaPrincipal(marcaPrincipal);
                        produto.setPreco(preco);
                        produto.setQuantidadeEstoque(estoque);
                        if (categoria != null) {
                            produto.setCategoria(categoria);
                        }
                        atualizados++;
                    } else {
                        produto = Produto.builder()
                                .codigoInterno(codigoInterno)
                                .nomePeca(nomePeca)
                                .marcaPrincipal(marcaPrincipal)
                                .preco(preco)
                                .quantidadeEstoque(estoque)
                                .categoria(categoria)
                                .build();
                        importados++;
                    }

                    produtoRepository.save(produto);
                } catch (NumberFormatException e) {
                    erros.add("Linha " + numeroLinha + ": preço ou estoque inválido.");
                } catch (Exception e) {
                    erros.add("Linha " + numeroLinha + ": " + e.getMessage());
                }
            }
        }

        return ImportacaoResultado.builder()
                .importados(importados)
                .atualizados(atualizados)
                .erros(erros)
                .build();
    }

    private boolean isCabecalho(String linha) {
        return linha.toLowerCase().startsWith("codigo_interno");
    }

    private BigDecimal parsePreco(String valor) {
        return new BigDecimal(valor.replace(",", "."));
    }
}
