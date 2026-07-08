import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class Token {
  private readonly storageKey = 'auth_token';
  private readonly roleStorageKey = 'auth_role';

  // ✅ AJOUTEZ CE CONSTRUCTOR
  constructor() {
    this.checkAndCleanExpiredToken();
  }

  saveToken(token: string): void {
    const normalized = this.normalizeToken(token);
    localStorage.setItem(this.storageKey, normalized);
  }

  getToken(): string | null {
    const token = localStorage.getItem(this.storageKey);
    return token ? this.normalizeToken(token) : null;
  }

  clearToken(): void {
    localStorage.removeItem(this.storageKey);
  }

  saveRole(role: string): void {
    const normalized = String(role ?? '').trim();
    if (!normalized) return;
    localStorage.setItem(this.roleStorageKey, normalized);
  }

  getRole(): string | null {
    const role = localStorage.getItem(this.roleStorageKey);
    return role ? role.trim() : null;
  }

  clearRole(): void {
    localStorage.removeItem(this.roleStorageKey);
  }

  hasToken(): boolean {
    return !!this.getToken();
  }

  decodeToken(): unknown | null {
    const token = this.getToken();
    if (!token) return null;
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    try {
      const payloadJson = this.base64UrlDecode(parts[1]);
      return JSON.parse(payloadJson);
    } catch {
      return null;
    }
  }

  isTokenExpired(): boolean {
    const token = this.getToken();
    if (!token) return true;
    if (token.split('.').length !== 3) return false;
    const payload = this.decodeToken();
    if (!payload || typeof payload !== 'object') return false;
    const exp = (payload as any).exp;
    if (typeof exp !== 'number') return false;
    const nowSeconds = Math.floor(Date.now() / 1000);
    return nowSeconds >= exp;
  }

  // ✅ AJOUTEZ CETTE NOUVELLE MÉTHODE
  private checkAndCleanExpiredToken(): void {
    if (this.hasToken() && this.isTokenExpired()) {
      console.warn('Token expiré détecté au démarrage - Suppression automatique');
      this.clearToken();
      this.clearRole();
    }
  }

  private normalizeToken(rawToken: string): string {
    const trimmed = String(rawToken ?? '').trim();
    return trimmed.toLowerCase().startsWith('bearer ')
      ? trimmed.slice('bearer '.length).trim()
      : trimmed;
  }

  private base64UrlDecode(base64Url: string): string {
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=');
    const binary = atob(padded);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i);
    }
    return new TextDecoder('utf-8').decode(bytes);
  }
}