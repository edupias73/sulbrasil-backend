package com.sulbrasil.catalogo.controller;

import com.sulbrasil.catalogo.dto.ContenidoProductoDto;
import com.sulbrasil.catalogo.dto.ContenidoSeccionDto;
import com.sulbrasil.catalogo.service.CatalogoContenidoService;
import com.sulbrasil.catalogo.service.ProdutoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminCatalogoController {

    private final CatalogoContenidoService catalogoContenidoService;
    private final ProdutoService produtoService;

    @Value("${admin.pin:1234}")
    private String adminPin;

    private boolean pinValido(String pin) {
        return pin != null && pin.equals(adminPin);
    }

    private ResponseEntity<Map<String, String>> naoAutorizado() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "PIN incorrecto o ausente."));
    }

    @PostMapping("/verificar-pin")
    public ResponseEntity<?> verificarPin(@RequestBody Map<String, String> body) {
        if (pinValido(body.get("pin"))) {
            return ResponseEntity.ok(Map.of("valido", true));
        }
        return naoAutorizado();
    }

    @GetMapping("/contenido")
    public ResponseEntity<?> obterContenido() {
        // O conteúdo JSON ainda é usado para Banners da página inicial
        try {
            return ResponseEntity.ok(catalogoContenidoService.obterContenido());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "Error al leer el contenido."));
        }
    }

    @PatchMapping("/secciones/{seccionId}")
    public ResponseEntity<?> atualizarSeccion(
            @RequestHeader(value = "X-Admin-Pin", required = false) String pin,
            @PathVariable String seccionId,
            @RequestBody ContenidoSeccionDto dados) {
        if (!pinValido(pin)) return naoAutorizado();
        try {
            catalogoContenidoService.atualizarSeccion(seccionId, dados);
            return ResponseEntity.ok(Map.of("mensaje", "Sección actualizada."));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "Error al guardar sección."));
        }
    }

    @PostMapping("/upload/{tipo}/{id}")
    public ResponseEntity<?> uploadImagem(
            @RequestHeader(value = "X-Admin-Pin", required = false) String pin,
            @PathVariable String tipo,
            @PathVariable String id,
            @RequestParam("archivo") MultipartFile arquivo) {
        if (!pinValido(pin)) return naoAutorizado();
        try {
            String url = catalogoContenidoService.salvarImagem(tipo, id, arquivo);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "Error al subir la imagen."));
        }
    }

    // --- NOVAS ROTAS (DIRETO NO MYSQL) ---

    @PatchMapping("/productos/{codigoInterno}")
    public ResponseEntity<?> atualizarProducto(
            @RequestHeader(value = "X-Admin-Pin", required = false) String pin,
            @PathVariable String codigoInterno,
            @RequestBody ContenidoProductoDto dados) {
        if (!pinValido(pin)) return naoAutorizado();
        try {
            // Salva Descrição e a Galeria de Fotos no MySQL!
            produtoService.atualizarDetalhes(
                    codigoInterno,
                    dados.getDescripcion(),
                    dados.getImagen(),
                    dados.getImagenes()
            );
            return ResponseEntity.ok(Map.of("mensaje", "Producto actualizado correctamente en la Base de Datos."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "Error al actualizar producto: " + e.getMessage()));
        }
    }

    @DeleteMapping("/productos/{codigoInterno}")
    public ResponseEntity<?> deletarProducto(
            @RequestHeader(value = "X-Admin-Pin", required = false) String pin,
            @PathVariable String codigoInterno) {
        if (!pinValido(pin)) return naoAutorizado();
        try {
            produtoService.deletarPorCodigo(codigoInterno);
            return ResponseEntity.ok(Map.of("mensaje", "Producto eliminado permanentemente."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "Error al eliminar producto."));
        }
    }

    @PostMapping("/importar-csv")
    public ResponseEntity<?> importarCsv(
            @RequestHeader(value = "X-Admin-Pin", required = false) String pin,
            @RequestParam("archivo") MultipartFile arquivo) {
        if (!pinValido(pin)) return naoAutorizado();
        try {
            return ResponseEntity.ok(produtoService.importarCsv(arquivo));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("erro", e.getMessage()));
        }
    }
}