package com.sulbrasil.catalogo.controller;

import com.sulbrasil.catalogo.dto.CatalogoContenidoDto;
import com.sulbrasil.catalogo.dto.ContenidoProductoDto;
import com.sulbrasil.catalogo.dto.ContenidoSeccionDto;
import com.sulbrasil.catalogo.dto.UploadImagenResponse;
import com.sulbrasil.catalogo.service.CatalogoContenidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminCatalogoController {

    private final CatalogoContenidoService catalogoContenidoService;

    @Value("${catalogo.admin.pin:1234}")
    private String adminPin;

    @GetMapping("/contenido")
    public ResponseEntity<?> obterContenido(@RequestHeader(value = "X-Admin-Pin", required = false) String pin) {
        if (!pinValido(pin)) {
            return naoAutorizado();
        }
        try {
            return ResponseEntity.ok(catalogoContenidoService.obterContenido());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "No se pudo leer el contenido."));
        }
    }

    @PutMapping("/contenido")
    public ResponseEntity<?> salvarContenido(
            @RequestHeader(value = "X-Admin-Pin", required = false) String pin,
            @RequestBody CatalogoContenidoDto contenido) {
        if (!pinValido(pin)) {
            return naoAutorizado();
        }
        try {
            catalogoContenidoService.salvarContenido(contenido);
            return ResponseEntity.ok(Map.of("mensaje", "Contenido guardado."));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "No se pudo guardar."));
        }
    }

    @PatchMapping("/secciones/{seccionId}")
    public ResponseEntity<?> atualizarSeccion(
            @RequestHeader(value = "X-Admin-Pin", required = false) String pin,
            @PathVariable String seccionId,
            @RequestBody ContenidoSeccionDto dados) {
        if (!pinValido(pin)) {
            return naoAutorizado();
        }
        try {
            catalogoContenidoService.atualizarSeccion(seccionId, dados);
            return ResponseEntity.ok(Map.of("mensaje", "Sección actualizada."));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "No se pudo actualizar la sección."));
        }
    }

    @PatchMapping("/productos/{codigoInterno}")
    public ResponseEntity<?> atualizarProducto(
            @RequestHeader(value = "X-Admin-Pin", required = false) String pin,
            @PathVariable String codigoInterno,
            @RequestBody ContenidoProductoDto dados) {
        if (!pinValido(pin)) {
            return naoAutorizado();
        }
        try {
            catalogoContenidoService.atualizarProducto(codigoInterno, dados);
            return ResponseEntity.ok(Map.of("mensaje", "Producto actualizado."));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "No se pudo actualizar el producto."));
        }
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImagem(
            @RequestHeader(value = "X-Admin-Pin", required = false) String pin,
            @RequestParam("tipo") String tipo,
            @RequestParam("id") String id,
            @RequestParam("archivo") MultipartFile archivo) {
        if (!pinValido(pin)) {
            return naoAutorizado();
        }
        if (!tipo.equals("secciones") && !tipo.equals("productos")) {
            return ResponseEntity.badRequest().body(Map.of("erro", "tipo debe ser 'secciones' o 'productos'."));
        }
        try {
            String url = catalogoContenidoService.salvarImagem(tipo, id, archivo);
            return ResponseEntity.ok(new UploadImagenResponse(url));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("erro", "No se pudo subir la imagen."));
        }
    }

    @PostMapping("/verificar-pin")
    public ResponseEntity<?> verificarPin(@RequestBody Map<String, String> body) {
        String pin = body.get("pin");
        if (pinValido(pin)) {
            return ResponseEntity.ok(Map.of("valido", true));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("valido", false));
    }

    private boolean pinValido(String pin) {
        return pin != null && pin.equals(adminPin);
    }

    private ResponseEntity<Map<String, String>> naoAutorizado() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", "PIN incorrecto."));
    }
}
