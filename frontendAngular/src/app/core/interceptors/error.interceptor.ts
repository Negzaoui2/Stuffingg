import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import Keycloak from 'keycloak-js';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const kc: Keycloak = (window as any).__keycloak;

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {

      if (error.status === 401) {
        console.warn('Token expiré ou invalide - Redirection vers Keycloak');
        kc?.login();
      }

      if (error.status === 403) {
        console.error('Accès refusé - Permissions insuffisantes');
        router.navigate(['/']);
      }

      return throwError(() => error);
    })
  );
};