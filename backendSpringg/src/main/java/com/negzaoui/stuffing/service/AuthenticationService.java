package com.negzaoui.stuffing.service;

import com.negzaoui.stuffing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * L'authentification (login/register) est maintenant déléguée à Keycloak.
 * Ce service est conservé pour d'éventuelles opérations de synchronisation.
 */
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
}
