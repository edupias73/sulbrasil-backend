package com.sulbrasil.catalogo.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "produtos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_interno", nullable = false, unique = true, length = 50)
    private String codigoInterno;

    @Column(name = "nome_peca", nullable = false, length = 255)
    private String nomePeca;

    @Column(name = "marca_principal", length = 100)
    private String marcaPrincipal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

    @Column(name = "quantidade_estoque", nullable = false)
    private Integer quantidadeEstoque;

    @Column(name = "termos_busca", columnDefinition = "TEXT")
    private String termosBusca;

    @Column(length = 50)
    private String categoria;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "url_imagen", length = 500)
    private String urlImagen;

    @OneToMany(mappedBy = "produto", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @Builder.Default
    private List<CodigoCruzado> codigosCruzados = new ArrayList<>();

    @OneToMany(mappedBy = "produto", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @Builder.Default
    private List<AplicacaoVeiculo> aplicacoesVeiculo = new ArrayList<>();

    @PrePersist
    @PreUpdate
    private void atualizarTermosBuscaAutomatico() {
        this.termosBusca = montarTermosBusca();
    }

    public String montarTermosBusca() {
        StringBuilder sb = new StringBuilder();
        appendTermo(sb, nomePeca);
        appendTermo(sb, marcaPrincipal);
        appendTermo(sb, categoria);
        appendTermo(sb, descripcion);

        if (codigosCruzados != null) {
            for (CodigoCruzado codigo : codigosCruzados) {
                appendTermo(sb, codigo.getCodigo());
                appendTermo(sb, codigo.getMarcaFabricante());
            }
        }

        if (aplicacoesVeiculo != null) {
            for (AplicacaoVeiculo aplicacao : aplicacoesVeiculo) {
                appendTermo(sb, aplicacao.getMontadora());
                appendTermo(sb, aplicacao.getVeiculo());
            }
        }

        return sb.toString().trim();
    }

    private void appendTermo(StringBuilder sb, String valor) {
        if (valor == null || valor.isBlank()) {
            return;
        }
        String limpo = valor.toLowerCase().trim();
        if (!sb.isEmpty()) {
            sb.append(' ');
        }
        sb.append(limpo);

        // Mágica: remove caracteres especiais para achar SB123 quando a peça for SB-123
        String semEspecial = limpo.replaceAll("[^a-z0-9]", "");
        if (!semEspecial.equals(limpo) && !semEspecial.isBlank()) {
            sb.append(' ').append(semEspecial);
        }
    }
}
