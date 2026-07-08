import { ApplicationConfig, NgZone, inject, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpInterceptorFn } from '@angular/common/http';
import { switchMap, Observable } from 'rxjs';

import { routes } from './app.routes';

/**
 * Token interceptor that refreshes the token if needed before each request.
 * Wraps the entire HTTP pipeline inside NgZone so that responses always
 * trigger Angular change detection.
 */
const keycloakTokenInterceptor: HttpInterceptorFn = (req, next) => {
  const keycloak = (window as any).__keycloak;
  if (!keycloak?.token) return next(req);

  const ngZone = inject(NgZone);

  return new Observable((subscriber) => {
    keycloak
      .updateToken(30)
      .then(() => ngZone.run(() => {
        const cloned = req.clone({
          setHeaders: { Authorization: `Bearer ${keycloak.token}` },
        });
        next(cloned).subscribe({
          next: (event) => subscriber.next(event),
          error: (err) => subscriber.error(err),
          complete: () => subscriber.complete(),
        });
      }))
      .catch(() => ngZone.run(() => {
        const cloned = req.clone({
          setHeaders: { Authorization: `Bearer ${keycloak.token}` },
        });
        next(cloned).subscribe({
          next: (event) => subscriber.next(event),
          error: (err) => subscriber.error(err),
          complete: () => subscriber.complete(),
        });
      }));
  });
};

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([keycloakTokenInterceptor])),
  ],
};
