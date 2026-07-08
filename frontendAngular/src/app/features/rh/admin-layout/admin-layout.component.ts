import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { Auth } from '../../../core/services/auth';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './admin-layout.component.html',
  styleUrls: ['./admin-layout.component.css'],
})
export class AdminLayoutComponent {
  constructor(private auth: Auth) {}

  get userInitials(): string {
    return this.auth.getUserInitials() || 'AD';
  }

  get displayName(): string {
    return this.auth.getFullName() || 'Admin';
  }

  logout(): void {
    this.auth.logout();
  }
}
