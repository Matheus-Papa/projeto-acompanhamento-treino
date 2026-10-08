package br.com.treino.exception;

// Mapeada pelo handler global da Fase 3 para 401 JSON padronizado
// {timestamp, status, mensagem, caminho, errosCampos[]}.
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
