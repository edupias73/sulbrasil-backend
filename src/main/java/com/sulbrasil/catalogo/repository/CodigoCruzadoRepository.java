package com.sulbrasil.catalogo.repository;

import com.sulbrasil.catalogo.entity.CodigoCruzado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CodigoCruzadoRepository extends JpaRepository<CodigoCruzado, Long> {
}
