package com.sulbrasil.catalogo.service;

import com.sulbrasil.catalogo.dto.ImportacaoResultado;
import com.sulbrasil.catalogo.entity.Produto;
import com.sulbrasil.catalogo.repository.ProdutoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

    // 1. MOTOR DE BUSCA PAGINADO E OTIMIZADO PARA 1 MILHÃO DE ITENS
    @Transactional(readOnly = true)
    public Page<Produto> buscarPaginado(String termo, String categoria, Pageable pageable) {
        Specification<Produto> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filtro de Categoria
            if (categoria != null && !categoria.isBlank()) {
                String catFormatada = categoria.toLowerCase().trim();
                Predicate exata = cb.equal(cb.lower(root.get("categoria").as(String.class)), catFormatada);

                String termoCat = catFormatada;
                if (termoCat.endsWith("es")) termoCat = termoCat.substring(0, termoCat.length() - 2);
                else if (termoCat.endsWith("s")) termoCat = termoCat.substring(0, termoCat.length() - 1);

                Predicate noNome = cb.like(cb.lower(root.get("nomePeca").as(String.class)), "%" + termoCat + "%");
                predicates.add(cb.or(exata, noNome));
            }

            // Pesquisa Global (Nome, Código Interno, Marca, Códigos OEM e Aplicações)
            if (termo != null && !termo.isBlank()) {
                String buscaLimpa = termo.toLowerCase().trim();
                String[] palavras = buscaLimpa.split("\\s+");

                for (String palavra : palavras) {
                    if (!palavra.isBlank() && palavra.length() > 1) {
                        String pattern = "%" + palavra + "%";
                        Predicate noNome = cb.like(cb.lower(root.get("nomePeca").as(String.class)), pattern);
                        Predicate noCodigo = cb.like(cb.lower(root.get("codigoInterno").as(String.class)), pattern);
                        Predicate naMarca = cb.like(cb.lower(root.get("marcaPrincipal").as(String.class)), pattern);
                        Predicate nosTermos = cb.like(cb.lower(root.get("termosBusca").as(String.class)), pattern);

                        predicates.add(cb.or(noNome, noCodigo, naMarca, nosTermos));
                    }
                }
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        return produtoRepository.findAll(spec, pageable);
    }

    // 2. ATUALIZAR DESCRIÇÃO E GALERIA DIRETO NO MYSQL
    @Transactional
    public void atualizarDetalhes(String codigoInterno, String descripcion, String urlImagen, List<String> galeriaUrls) {
        Produto p = produtoRepository.findByCodigoInterno(codigoInterno)
                .orElseThrow(() -> new IllegalArgumentException("Pieza no encontrada: " + codigoInterno));

        if (descripcion != null) p.setDescripcion(descripcion);
        if (urlImagen != null) p.setUrlImagen(urlImagen);

        if (galeriaUrls != null) {
            if (p.getGaleria() == null) {
                p.setGaleria(new ArrayList<>());
            } else {
                p.getGaleria().clear();
            }
            p.getGaleria().addAll(galeriaUrls);
        }

        p.setTermosBusca(p.montarTermosBusca());
        produtoRepository.save(p);
    }

    // 3. SALVAMENTO MANUAL PELO PAINEL ADMIN
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
        if (produto.getDescripcion() != null) p.setDescripcion(produto.getDescripcion());

        // Sincronizar Códigos OEM
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

        // Sincronizar Aplicações
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

        p.setTermosBusca(p.montarTermosBusca());
        return produtoRepository.save(p);
    }

    // 4. IMPORTAÇÃO EM MASSA VIA EXCEL (CSV)
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

                    // CORREÇÃO AQUI: Instanciação 100% segura sem usar o .builder()
                    Produto produto = produtoRepository.findByCodigoInterno(codigoInterno)
                            .orElseGet(() -> {
                                Produto novo = new Produto();
                                novo.setCodigoInterno(codigoInterno);
                                return novo;
                            });

                    boolean isNovo = produto.getId() == null;

                    produto.setNomePeca(nomePeca);
                    produto.setMarcaPrincipal(colunas[COL_MARCA].trim());
                    produto.setPreco(parsePreco(colunas[COL_PRECO].trim()));
                    produto.setQuantidadeEstoque(Integer.parseInt(colunas[COL_ESTOQUE].trim()));

                    if (colunas.length > COL_CATEGORIA && !colunas[COL_CATEGORIA].isBlank()) {
                        produto.setCategoria(colunas[COL_CATEGORIA].trim());
                    }

                    produto.setTermosBusca(produto.montarTermosBusca());
                    produtoRepository.save(produto);

                    if (isNovo) importados++; else atualizados++;

                    // Limpa a RAM a cada 100 peças processadas (Evita OutOfMemory)
                    if ((importados + atualizados) % BATCH_SIZE == 0) {
                        entityManager.flush();
                        entityManager.clear();
                    }

                } catch (Exception e) {
                    erros.add("Línea " + numeroLinha + ": Error de formato (" + e.getMessage() + ")");
                }
            }
        }

        // Se a sua classe ImportacaoResultado também não tiver @Builder, troque essa linha por um 'new ImportacaoResultado(...)'
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

    // 5. EXCLUSÃO DE PRODUTO
    @Transactional
    public void deletarPorCodigo(String codigoInterno) {
        produtoRepository.findByCodigoInterno(codigoInterno)
                .ifPresent(produtoRepository::delete);
    }
}