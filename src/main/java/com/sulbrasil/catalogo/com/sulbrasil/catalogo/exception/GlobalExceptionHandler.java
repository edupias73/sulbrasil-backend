package com.sulbrasil.catalogo.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleSecurity(SecurityException ex) {
        log.warn("Falha de segurança detectada: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("erro", "Acceso denegado o manipulación no permitida."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("erro", "El archivo excede el tamaño máximo permitido."));
    }

    // NOVO: Deteta se o Angular mandou o nome errado (ex: 'file' em vez de 'archivo')
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> handleMissingParams(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(Map.of("erro", "Falta el parámetro esperado: " + ex.getParameterName()));
    }

    // NOVO: Deteta se a foto corrompeu no meio do caminho ou estourou o limite
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, String>> handleMultipart(MultipartException ex) {
        return ResponseEntity.badRequest().body(Map.of("erro", "Error al procesar el archivo. ¿Es muy pesado o está corrupto?"));
    }

    // MODIFICADO: Agora devolve a MENSAGEM EXATA do erro do servidor em vez de um "500" genérico!
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAllExceptions(Exception ex) {
        log.error("Erro interno não tratado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("erro", "Error interno en el servidor: " + ex.getMessage()));
    }
}