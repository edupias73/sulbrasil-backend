package com.sulbrasil.catalogo.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "aplicacoes_veiculo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AplicacaoVeiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    @JsonBackReference
    private Produto produto;

    @Column(nullable = false, length = 80)
    private String montadora;

    @Column(nullable = false, length = 120)
    private String veiculo;

    @Column(name = "ano_inicio")
    private Integer anoInicio;

    @Column(name = "ano_fim")
    private Integer anoFim;
}
