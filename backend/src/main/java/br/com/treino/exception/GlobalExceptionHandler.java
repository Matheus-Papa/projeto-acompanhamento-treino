package br.com.treino.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErroCampo(String campo, String mensagem) {
    }

    public record ErroResponse(
            OffsetDateTime timestamp,
            int status,
            String mensagem,
            String caminho,
            List<ErroCampo> errosCampos) {
    }

    private ResponseEntity<ErroResponse> responder(
            HttpStatus status, String mensagem, String caminho, List<ErroCampo> errosCampos) {
        ErroResponse corpo = new ErroResponse(
                OffsetDateTime.now(),
                status.value(),
                mensagem,
                caminho,
                errosCampos == null ? List.of() : errosCampos);
        return ResponseEntity.status(status).body(corpo);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErroResponse> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErroResponse> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        return responder(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErroResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErroCampo> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> new ErroCampo(erro.getField(), erro.getDefaultMessage()))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "Erro de validação", request.getRequestURI(), campos);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErroResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        List<ErroCampo> campos = ex.getConstraintViolations().stream()
                .map(violacao -> new ErroCampo(violacao.getPropertyPath().toString(), violacao.getMessage()))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "Erro de validação", request.getRequestURI(), campos);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        return responder(HttpStatus.CONFLICT, "Violação de integridade", request.getRequestURI(), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleGenerico(Exception ex, HttpServletRequest request) {
        return responder(
                HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", request.getRequestURI(), List.of());
    }
}
