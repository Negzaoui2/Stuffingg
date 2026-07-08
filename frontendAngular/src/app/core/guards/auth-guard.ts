import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import Keycloak from 'keycloak-js';

export const authGuard: CanActivateFn = (route) => {
  const router = inject(Router);
  const kc: Keycloak = (window as any).__keycloak;

  if (!kc?.authenticated) {
    kc?.login();
    return false;
  }

  const requiredRoles = (route.data?.['roles'] as string[]) ?? [];
  if (requiredRoles.length === 0) return true;

  const userRoles = kc.realmAccess?.roles ?? [];
  const hasRole = requiredRoles.some((role) => userRoles.includes(role));

  return hasRole ? true : router.parseUrl('/');
};
