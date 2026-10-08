package com.sulbrasil.catalogo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sulbrasil.catalogo.dto.CatalogoContenidoDto;
import com.sulbrasil.catalogo.dto.ContenidoProductoDto;
import com.sulbrasil.catalogo.dto.ContenidoSeccionDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogoContenidoService {

    private static final Set<String> EXTENSIONES = Set.of("jpg", "jpeg", "png", "webp", "mp4");
    private final ObjectMapper objectMapper;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

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
        lock.readLock().lock();
        try {
            if (!Files.exists(contenidoPath)) {
                return CatalogoContenidoDto.builder().build();
            }
            return objectMapper.readValue(contenidoPath.toFile(), CatalogoContenidoDto.class);
        } catch (Exception e) {
            log.error("Erro ao ler JSON de conteúdo", e);
            throw new IOException("Falha ao carregar o conteúdo do catálogo.", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void salvarContenido(CatalogoContenidoDto contenido) throws IOException {
        lock.writeLock().lock();
        try {
            // Salva em um arquivo temporário primeiro (Prevenção contra corrupção por queda de energia)
            Path tempPath = contenidoPath.resolveSibling("catalogo-contenido.tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tempPath.toFile(), contenido);
            Files.move(tempPath, contenidoPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            log.error("Erro ao salvar JSON de conteúdo", e);
            throw new IOException("Falha ao persistir alterações no catálogo.", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void atualizarSeccion(String seccionId, ContenidoSeccionDto dados) throws IOException {
        lock.writeLock().lock();
        try {
            CatalogoContenidoDto contenido = obterContenido();
            ContenidoSeccionDto atual = contenido.getSecciones().getOrDefault(seccionId, new ContenidoSeccionDto());

            if (dados.getDescripcion() != null) atual.setDescripcion(dados.getDescripcion());
            if (dados.getImagen() != null) atual.setImagen(dados.getImagen());

            contenido.getSecciones().put(seccionId, atual);
            salvarContenido(contenido);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void atualizarProducto(String codigoInterno, ContenidoProductoDto dados) throws IOException {
        lock.writeLock().lock();
        try {
            CatalogoContenidoDto contenido = obterContenido();
            String codigo = codigoInterno.trim().toUpperCase(Locale.ROOT);
            ContenidoProductoDto atual = contenido.getProductos().getOrDefault(codigo, new ContenidoProductoDto());

            if (dados.getDescripcion() != null) atual.setDescripcion(dados.getDescripcion());
            if (dados.getImagen() != null) atual.setImagen(dados.getImagen());

            contenido.getProductos().put(codigo, atual);
            salvarContenido(contenido);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String salvarImagem(String tipo, String id, MultipartFile arquivo) throws IOException {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Archivo vacio ou no enviado.");
        }

        String extensao = extrairExtensao(arquivo.getOriginalFilename());
        String nomeArquivo = sanitizarId(id) + "." + extensao;

        Path tipoDir = uploadsPath.resolve(tipo).normalize();
        Path destino = tipoDir.resolve(nomeArquivo).normalize();

        // Prevenção estrita contra Path Traversal (CWE-22)
        if (!destino.startsWith(tipoDir)) {
            throw new SecurityException("Intento de manipulacion de ruta detectado.");
        }

        Files.createDirectories(destino.getParent());
        Files.copy(arquivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/" + tipo + "/" + nomeArquivo;
    }

    private String extrairExtensao(String nomeOriginal) {
        if (nomeOriginal == null || !nomeOriginal.contains(".")) return "jpg";
        String ext = nomeOriginal.substring(nomeOriginal.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!EXTENSIONES.contains(ext)) {
            throw new IllegalArgumentException("Formato no permitido. Use JPG, PNG, WEBP ou MP4.");
        }
        return ext.equals("jpeg") ? "jpg" : ext;
    }

    private String sanitizarId(String id) {
        return id.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\-_]", "-");
    }

    private void copiarContenidoPadrao() throws IOException {
        lock.writeLock().lock();
        try {
            ClassPathResource resource = new ClassPathResource("catalogo-contenido-default.json");
            if (resource.exists()) {
                Files.copy(resource.getInputStream(), contenidoPath, StandardCopyOption.REPLACE_EXISTING);
            } else {
                salvarContenido(CatalogoContenidoDto.builder().build());
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}