import { Component, OnInit } from '@angular/core';
import { Auth } from '../../../core/services/auth';

/**
 * Ce composant n'est plus routé (Keycloak gère le login).
 * Conservé comme fallback : s'il est chargé, il redirige vers Keycloak.
 */
@Component({
  selector: 'app-login',
  standalone: true,
  template: `<p>Redirection vers la page de connexion…</p>`,
})
export class LoginComponent implements OnInit {
  constructor(private auth: Auth) {}

  ngOnInit(): void {
    if (!this.auth.isLoggedIn()) {
      this.auth.login();
    }
  }
}