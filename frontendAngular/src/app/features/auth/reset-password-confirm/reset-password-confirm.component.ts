import { Component } from '@angular/core';
import { Auth } from '../../../core/services/auth';

/**
 * Plus utilisé — Keycloak gère la confirmation de réinitialisation.
 */
@Component({
  selector: 'app-reset-password-confirm',
  standalone: true,
  template: `<p>Redirection…</p>`,
})
export class ResetPasswordConfirmComponent {
  constructor(private auth: Auth) {
    if (!this.auth.isLoggedIn()) {
      this.auth.login();
    }
  }
}
