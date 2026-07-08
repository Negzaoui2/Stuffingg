import { Component } from '@angular/core';
import { Auth } from '../../../core/services/auth';

/**
 * Plus utilisé — Keycloak gère la réinitialisation du mot de passe.
 */
@Component({
  selector: 'app-reset-password',
  standalone: true,
  template: `<p>Redirection…</p>`,
})
export class ResetPasswordComponent {
  constructor(private auth: Auth) {
    if (!this.auth.isLoggedIn()) {
      this.auth.login();
    }
  }
}
