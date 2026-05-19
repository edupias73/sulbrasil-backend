package com.sulbrasil.catalogo.repository;

import com.sulbrasil.catalogo.entity.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    List<Produto> findByTermosBuscaContainingIgnoreCase(String termo);

    List<Produto> findByCategoriaIgnoreCaseOrderByNomePecaAsc(String categoria);

    List<Produto> findByCategoriaIgnoreCaseAndTermosBuscaContainingIgnoreCase(String categoria, String termo);

    Optional<Produto> findByCodigoInterno(String codigoInterno);
}
