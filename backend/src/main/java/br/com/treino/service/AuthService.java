package br.com.treino.service;

import br.com.treino.domain.Usuario;
import br.com.treino.dto.AuthResponse;
import br.com.treino.dto.LoginRequest;
import br.com.treino.dto.RegisterRequest;
import br.com.treino.exception.ConflictException;
import br.com.treino.exception.UnauthorizedException;
import br.com.treino.repository.UsuarioRepository;
import br.com.treino.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("E-mail já cadastrado");
        }
        Usuario usuario = new Usuario(
                request.nome().trim(),
                email,
                passwordEncoder.encode(request.senha()));
        usuarioRepository.save(usuario);
        return new AuthResponse(
                jwtService.generateToken(usuario.getId()),
                jwtService.getExpiresInSeconds());
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Credenciais inválidas"));
        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            throw new UnauthorizedException("Credenciais inválidas");
        }
        return new AuthResponse(
                jwtService.generateToken(usuario.getId()),
                jwtService.getExpiresInSeconds());
    }
}
