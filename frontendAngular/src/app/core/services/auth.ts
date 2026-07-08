import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import Keycloak from 'keycloak-js';
import { environment } from '../../../environments/environment';
import { NotificationService } from './notification.service';
import { AccountCreationRequestDto } from '../models/account-request.model';
import { MessageResponse } from '../models/message-response.model';

@Injectable({
  providedIn: 'root',
})
export class Auth {
  private get kc(): Keycloak {
    return (window as any).__keycloak;
  }

  constructor(
    private http: HttpClient,
    private notificationService: NotificationService
  ) {}

  // ============ LOGIN (via Keycloak redirect) ============
  login(): void {
    this.kc.login();
  }

  // ============ LOGOUT ============
  logout(): void {
    this.notificationService.reset();
    this.kc.logout({ redirectUri: window.location.origin });
  }

  // ============ HELPERS ============
  isLoggedIn(): boolean {
    return !!this.kc?.authenticated;
  }

  getToken(): string {
    return this.kc?.token ?? '';
  }

  getCurrentUser(): Record<string, unknown> | null {
    return (this.kc?.tokenParsed as Record<string, unknown>) ?? null;
  }

  getUserRoles(): string[] {
    return this.kc?.realmAccess?.roles ?? [];
  }

  getUsername(): string {
    return this.kc?.tokenParsed?.['preferred_username'] ?? '';
  }

  getFullName(): string {
    const parsed = this.kc?.tokenParsed;
    const first = parsed?.['given_name'] ?? '';
    const last = parsed?.['family_name'] ?? '';
    return [first, last].filter(Boolean).join(' ') || this.getUsername();
  }

  getUserInitials(): string {
    const parsed = this.kc?.tokenParsed;
    const first = (parsed?.['given_name'] as string || '').charAt(0);
    const last = (parsed?.['family_name'] as string || '').charAt(0);
    return (first + last).toUpperCase() || 'U';
  }

  // ============ demande de creation de compte ============
  submitAccountRequest(request: AccountCreationRequestDto): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(
      `${environment.apiUrl}/public/account-requests`,
      request
    );
  }
}