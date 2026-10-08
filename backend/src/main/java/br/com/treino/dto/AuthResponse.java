package br.com.treino.dto;

public record AuthResponse(String token, long expiresIn) {
}
