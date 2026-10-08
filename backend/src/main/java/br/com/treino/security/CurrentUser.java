package br.com.treino.security;

import br.com.treino.exception.UnauthorizedException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// Contrato de isolamento por usuário: F3/F4/F5 usam currentUsuarioId()
// para filtrar toda query; recurso de outro usuário responde 404 nos services.
public final class CurrentUser {

    private CurrentUser() {
    }

    public static UUID currentUsuarioId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID usuarioId)) {
            throw new UnauthorizedException("Usuário não autenticado");
        }
        return usuarioId;
    }
}
