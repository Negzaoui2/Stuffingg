import { Component, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { Auth } from '../../core/services/auth';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="landing-container">
      <div class="landing-card">
        <div class="landing-header">
          <svg width="60" height="60" viewBox="0 0 50 50" fill="none">
            <rect width="50" height="50" rx="10" fill="url(#grad)" />
            <path d="M15 25L22 32L35 18" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
            <defs>
              <linearGradient id="grad" x1="0" y1="0" x2="50" y2="50">
                <stop offset="0%" stop-color="#5B2C91" />
                <stop offset="100%" stop-color="#3D1A5C" />
              </linearGradient>
            </defs>
          </svg>
          <h1>Stuffing Intelligent</h1>
          <p>Plateforme de gestion des ressources et du staffing</p>
        </div>
        <div class="landing-actions">
          <button class="btn-login" (click)="login()">Se connecter</button>
          <div class="separator">ou</div>
          <a routerLink="/auth/register" class="btn-register">
            Pas encore de compte ? Demander un accès
          </a>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .landing-container {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      background: linear-gradient(135deg, #f5f0fa 0%, #e8dff5 100%);
    }
    .landing-card {
      background: white;
      border-radius: 16px;
      padding: 48px;
      box-shadow: 0 10px 40px rgba(91, 44, 145, 0.1);
      text-align: center;
      max-width: 420px;
      width: 90%;
    }
    .landing-header h1 {
      margin: 16px 0 8px;
      color: #2d1553;
      font-size: 1.6rem;
    }
    .landing-header p {
      color: #666;
      margin: 0 0 32px;
      font-size: 0.95rem;
    }
    .landing-actions {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .btn-login {
      background: linear-gradient(135deg, #5B2C91, #3D1A5C);
      color: white;
      border: none;
      padding: 14px 24px;
      border-radius: 8px;
      font-size: 1rem;
      font-weight: 600;
      cursor: pointer;
      transition: opacity 0.2s;
    }
    .btn-login:hover { opacity: 0.9; }
    .separator {
      color: #999;
      font-size: 0.85rem;
      margin: 4px 0;
    }
    .btn-register {
      color: #5B2C91;
      text-decoration: none;
      font-weight: 500;
      font-size: 0.9rem;
      padding: 10px;
      border-radius: 8px;
      transition: background 0.2s;
    }
    .btn-register:hover {
      background: #f5f0fa;
    }
  `],
})
export class HomeComponent implements OnInit {
  constructor(
    private auth: Auth,
    private router: Router
  ) {}

  ngOnInit(): void {
    if (!this.auth.isLoggedIn()) return;

    const roles = this.auth.getUserRoles();

    if (roles.includes('ADMIN')) {
      this.router.navigate(['/admin']);
    } else if (roles.includes('DELIVERY_MANAGER')) {
      this.router.navigate(['/manager']);
    } else if (roles.includes('COLLABORATEUR')) {
      this.router.navigate(['/collaborator']);
    }
  }

  login(): void {
    this.auth.login();
  }
}
