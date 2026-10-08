package br.com.treino.exception;

// 404 padronizado: recurso de outro usuário também cai aqui (não 403, para não vazar existência).
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
