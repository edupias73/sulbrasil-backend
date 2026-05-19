package com.sulbrasil.catalogo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sulbrasil.catalogo.dto.CatalogoContenidoDto;
import com.sulbrasil.catalogo.dto.ContenidoProductoDto;
import com.sulbrasil.catalogo.dto.ContenidoSeccionDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CatalogoContenidoService {

    private static final Set<String> EXTENSIONES = Set.of("jpg", "jpeg", "png", "webp");

    private final ObjectMapper objectMapper;

    @Value("${catalogo.data.dir:./data}")
    private String dataDir;

    private Path contenidoPath;
    private Path uploadsPath;

    @PostConstruct
    void init() throws IOException {
        Path base = Path.of(dataDir).toAbsolutePath().normalize();
        Files.createDirectories(base);
        contenidoPath = base.resolve("catalogo-contenido.json");
        uploadsPath = base.resolve("uploads");
        Files.createDirectories(uploadsPath.resolve("secciones"));
        Files.createDirectories(uploadsPath.resolve("productos"));

        if (!Files.exists(contenidoPath)) {
            copiarContenidoPadrao();
        }
    }

    public Path getUploadsPath() {
        return uploadsPath;
    }

    public CatalogoContenidoDto obterContenido() throws IOException {
        return objectMapper.readValue(contenidoPath.toFile(), CatalogoContenidoDto.class);
    }

    public void salvarContenido(CatalogoContenidoDto contenido) throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(contenidoPath.toFile(), contenido);
    }

    public void atualizarSeccion(String seccionId, ContenidoSeccionDto dados) throws IOException {
        CatalogoContenidoDto contenido = obterContenido();
        ContenidoSeccionDto atual = contenido.getSecciones().getOrDefault(seccionId, new ContenidoSeccionDto());
        if (dados.getDescripcion() != null) {
            atual.setDescripcion(dados.getDescripcion());
        }
        if (dados.getImagen() != null) {
            atual.setImagen(dados.getImagen());
        }
        contenido.getSecciones().put(seccionId, atual);
        salvarContenido(contenido);
    }

    public void atualizarProducto(String codigoInterno, ContenidoProductoDto dados) throws IOException {
        CatalogoContenidoDto contenido = obterContenido();
        String codigo = codigoInterno.trim().toUpperCase(Locale.ROOT);
        ContenidoProductoDto atual = contenido.getProductos().getOrDefault(codigo, new ContenidoProductoDto());
        if (dados.getDescripcion() != null) {
            atual.setDescripcion(dados.getDescripcion());
        }
        if (dados.getImagen() != null) {
            atual.setImagen(dados.getImagen());
        }
        contenido.getProductos().put(codigo, atual);
        salvarContenido(contenido);
    }

    public String salvarImagem(String tipo, String id, MultipartFile arquivo) throws IOException {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Archivo de imagen vacío.");
        }

        String extensao = extrairExtensao(arquivo.getOriginalFilename());
        String nomeArquivo = sanitizarId(id) + "." + extensao;
        Path destino = uploadsPath.resolve(tipo).resolve(nomeArquivo);
        Files.createDirectories(destino.getParent());
        Files.copy(arquivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/" + tipo + "/" + nomeArquivo;
    }

    private String extrairExtensao(String nomeOriginal) {
        if (nomeOriginal == null || !nomeOriginal.contains(".")) {
            return "jpg";
        }
        String ext = nomeOriginal.substring(nomeOriginal.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!EXTENSIONES.contains(ext)) {
            throw new IllegalArgumentException("Formato no permitido. Use JPG, PNG o WEBP.");
        }
        return ext.equals("jpeg") ? "jpg" : ext;
    }

    private String sanitizarId(String id) {
        return id.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\-_]", "-");
    }

    private void copiarContenidoPadrao() throws IOException {
        ClassPathResource resource = new ClassPathResource("catalogo-contenido-default.json");
        if (resource.exists()) {
            Files.copy(resource.getInputStream(), contenidoPath, StandardCopyOption.REPLACE_EXISTING);
        } else {
            salvarContenido(CatalogoContenidoDto.builder().build());
        }
    }
}
