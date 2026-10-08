package br.com.treino.exception;

// Mapeada pelo handler global da Fase 3 para 409 JSON padronizado
// {timestamp, status, mensagem, caminho, errosCampos[]}.
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
