package com.sulbrasil.catalogo.service;

import com.sulbrasil.catalogo.dto.ImportacaoResultado;
import com.sulbrasil.catalogo.entity.Produto;
import com.sulbrasil.catalogo.repository.ProdutoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ProdutoService {

    private static final int COL_CODIGO = 0;
    private static final int COL_NOME = 1;
    private static final int COL_MARCA = 2;
    private static final int COL_PRECO = 3;
    private static final int COL_ESTOQUE = 4;
    private static final int COL_CATEGORIA = 5;

    private static final int BATCH_SIZE = 100;

    private final ProdutoRepository produtoRepository;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<Produto> buscar(String termo, String categoria) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Produto> cq = cb.createQuery(Produto.class);
        Root<Produto> produto = cq.from(Produto.class);
        List<Predicate> predicates = new ArrayList<>();

        // 1. FILTRO DE CATEGORIA
        if (categoria != null && !categoria.isBlank()) {
            String catFormatada = categoria.toLowerCase().trim();
            Predicate exata = cb.equal(cb.lower(produto.get("categoria")), catFormatada);

            String termoCat = catFormatada;
            if (termoCat.endsWith("es")) termoCat = termoCat.substring(0, termoCat.length() - 2);
            else if (termoCat.endsWith("s")) termoCat = termoCat.substring(0, termoCat.length() - 1);

            Predicate noNome = cb.like(cb.lower(produto.get("nomePeca")), "%" + termoCat + "%");
            predicates.add(cb.or(exata, noNome));
        }

        // 2. PESQUISA GLOBAL (Nome, Código Interno, Marca, Códigos OEM e Aplicações)
        if (termo != null && !termo.isBlank()) {
            String buscaLimpa = termo.toLowerCase().trim();
            String[] palavras = buscaLimpa.split("\\s+");

            for (String palavra : palavras) {
                if (!palavra.isBlank() && palavra.length() > 1) {
                    String pattern = "%" + palavra + "%";
                    Predicate noNome = cb.like(cb.lower(produto.get("nomePeca")), pattern);
                    Predicate noCodigo = cb.like(cb.lower(produto.get("codigoInterno")), pattern);
                    Predicate naMarca = cb.like(cb.lower(produto.get("marcaPrincipal")), pattern);

                    // AQUI ESTÁ A CORREÇÃO: Pesquisa na coluna que guarda os Códigos OEM e Aplicações
                    Predicate nosTermos = cb.like(cb.lower(produto.get("termosBusca")), pattern);

                    predicates.add(cb.or(noNome, noCodigo, naMarca, nosTermos));
                }
            }
        }

        if (predicates.isEmpty()) return List.of();

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.asc(produto.get("nomePeca")));

        List<Produto> produtos = entityManager.createQuery(cq)
                .setMaxResults(200) // Proteção contra travamento
                .getResultList();
        inicializarColecoes(produtos);
        return produtos;
    }

    private void inicializarColecoes(List<Produto> produtos) {
        produtos.forEach(p -> {
            if (p.getCodigosCruzados() != null) p.getCodigosCruzados().size();
            if (p.getAplicacoesVeiculo() != null) p.getAplicacoesVeiculo().size();
        });
    }

    @Transactional
    public Produto salvarManual(Produto produto) {
        if (produto.getCodigoInterno() == null || produto.getCodigoInterno().isBlank()) {
            throw new IllegalArgumentException("El código interno es obligatorio.");
        }

        Produto p = produtoRepository.findByCodigoInterno(produto.getCodigoInterno())
                .orElseGet(Produto::new);

        p.setCodigoInterno(produto.getCodigoInterno().toUpperCase(java.util.Locale.ROOT));
        p.setNomePeca(produto.getNomePeca());
        p.setMarcaPrincipal(produto.getMarcaPrincipal());
        p.setPreco(produto.getPreco() != null ? produto.getPreco() : BigDecimal.ZERO);
        p.setQuantidadeEstoque(produto.getQuantidadeEstoque() != null ? produto.getQuantidadeEstoque() : 0);
        if (produto.getCategoria() != null) p.setCategoria(produto.getCategoria());

        // SINCRONIZAR CÓDIGOS OEM
        if (p.getCodigosCruzados() != null) {
            p.getCodigosCruzados().clear();
        } else {
            p.setCodigosCruzados(new ArrayList<>());
        }
        if (produto.getCodigosCruzados() != null) {
            produto.getCodigosCruzados().forEach(c -> {
                c.setProduto(p);
                p.getCodigosCruzados().add(c);
            });
        }

        // SINCRONIZAR APLICACIONES
        if (p.getAplicacoesVeiculo() != null) {
            p.getAplicacoesVeiculo().clear();
        } else {
            p.setAplicacoesVeiculo(new ArrayList<>());
        }
        if (produto.getAplicacoesVeiculo() != null) {
            produto.getAplicacoesVeiculo().forEach(a -> {
                a.setProduto(p);
                p.getAplicacoesVeiculo().add(a);
            });
        }

        // --- A SOLUÇÃO ESTÁ AQUI ---
        // Obriga o sistema a varrer as tabelas e atualizar os termos de busca com os novos códigos!
        p.setTermosBusca(p.montarTermosBusca());

        return produtoRepository.save(p);
    }

    @Transactional
    public ImportacaoResultado importarCsv(MultipartFile arquivo) throws IOException {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Archivo CSV no enviado o vacio.");
        }

        int importados = 0;
        int atualizados = 0;
        List<String> erros = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(arquivo.getInputStream(), StandardCharsets.UTF_8))) {
            String linha;
            int numeroLinha = 0;

            while ((linha = reader.readLine()) != null) {
                numeroLinha++;
                linha = linha.trim();

                if (linha.isEmpty() || (numeroLinha == 1 && isCabecalho(linha))) continue;

                try {
                    String[] colunas = linha.split(",", -1);
                    if (colunas.length < 5) {
                        erros.add("Línea " + numeroLinha + ": faltan columnas.");
                        continue;
                    }

                    String codigoInterno = colunas[COL_CODIGO].trim().toUpperCase();
                    String nomePeca = colunas[COL_NOME].trim();

                    if (codigoInterno.isBlank() || nomePeca.isBlank()) {
                        erros.add("Línea " + numeroLinha + ": codigo_interno y nome_peca son obligatorios.");
                        continue;
                    }

                    Produto produto = produtoRepository.findByCodigoInterno(codigoInterno)
                            .orElseGet(() -> Produto.builder().codigoInterno(codigoInterno).build());

                    boolean isNovo = produto.getId() == null;

                    produto.setNomePeca(nomePeca);
                    produto.setMarcaPrincipal(colunas[COL_MARCA].trim());
                    produto.setPreco(parsePreco(colunas[COL_PRECO].trim()));
                    produto.setQuantidadeEstoque(Integer.parseInt(colunas[COL_ESTOQUE].trim()));

                    if (colunas.length > COL_CATEGORIA && !colunas[COL_CATEGORIA].isBlank()) {
                        produto.setCategoria(colunas[COL_CATEGORIA].trim());
                    }

                    produtoRepository.save(produto);

                    if (isNovo) importados++; else atualizados++;

                    // Prevenção de OutOfMemory em arquivos gigantes
                    if ((importados + atualizados) % BATCH_SIZE == 0) {
                        entityManager.flush();
                        entityManager.clear();
                    }

                } catch (Exception e) {
                    erros.add("Línea " + numeroLinha + ": Error de formato (" + e.getMessage() + ")");
                }
            }
        }
        return ImportacaoResultado.builder().importados(importados).atualizados(atualizados).erros(erros).build();
    }

    private boolean isCabecalho(String linha) {
        return linha.toLowerCase().startsWith("codigo_interno");
    }

    private BigDecimal parsePreco(String valor) {
        try {
            return new BigDecimal(valor.replace(",", "."));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    @Transactional
    public void deletarPorCodigo(String codigoInterno) {
        produtoRepository.findByCodigoInterno(codigoInterno)
                .ifPresent(produtoRepository::delete);
    }
}