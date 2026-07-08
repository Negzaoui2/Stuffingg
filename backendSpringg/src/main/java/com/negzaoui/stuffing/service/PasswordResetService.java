package com.negzaoui.stuffing.service;

import com.negzaoui.stuffing.entity.PasswordResetToken;
import com.negzaoui.stuffing.exception.InvalidTokenException;
import com.negzaoui.stuffing.repository.PasswordResetTokenRepository;
import com.negzaoui.stuffing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${password-reset.expiration-minutes:30}")
    private long expirationMinutes;

    /**
     * Crée un token de reset et le retourne.
     * IMPORTANT: en prod, on n'envoie jamais le token dans la réponse; on l'envoie par email.
     * Ici, on le retourne pour pouvoir avancer sans config mail.
     */
    public String createResetToken(String email) {
        var userOpt = userRepository.findByEmail(email);
        // Pour éviter l'énumération des utilisateurs, on ne révèle pas si l'email existe.
        if (userOpt.isEmpty()) {
            return null;
        }

        var user = userOpt.get();
        var token = generateToken();

        var reset = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES))
                .used(false)
                .build();

        tokenRepository.save(reset);

        // Envoi email (ou log si app.mail.enabled=false)
        emailService.sendPasswordResetEmail(user.getEmail(), token);

        return token;
    }

    @Transactional
    public void confirmReset(String token, String newPassword) {
        var reset = tokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Token invalide"));

        if (reset.isUsed()) throw new InvalidTokenException("Token déjà utilisé");
        if (reset.getExpiresAt().isBefore(Instant.now())) throw new InvalidTokenException("Token expiré");

        var user = reset.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        reset.setUsed(true);
        tokenRepository.save(reset);
    }

    private String generateToken() {
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }
}
