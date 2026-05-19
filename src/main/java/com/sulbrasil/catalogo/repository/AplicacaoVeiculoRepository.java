package com.sulbrasil.catalogo.repository;

import com.sulbrasil.catalogo.entity.AplicacaoVeiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AplicacaoVeiculoRepository extends JpaRepository<AplicacaoVeiculo, Long> {
}
