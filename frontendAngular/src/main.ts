import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';
import Keycloak from 'keycloak-js';
import { environment } from './environments/environment';

/**
 * Initialize Keycloak BEFORE Angular bootstrap.
 * This ensures all Angular code runs inside NgZone naturally.
 */
const keycloak = new Keycloak({
  url: environment.keycloak.url,
  realm: environment.keycloak.realm,
  clientId: environment.keycloak.clientId,
});

keycloak
  .init({
    onLoad: 'check-sso',
    checkLoginIframe: false,
    pkceMethod: 'S256',
    silentCheckSsoRedirectUri:
      window.location.origin + '/assets/silent-check-sso.html',
  })
  .then((authenticated) => {
    // Store reference globally so Angular services can use it
    (window as any).__keycloak = keycloak;

    // Bootstrap Angular regardless of auth status (public pages like /auth/register)
    bootstrapApplication(App, appConfig).catch((err) => console.error(err));
  })
  .catch((err) => {
    console.error('Keycloak init failed:', err);
    document.body.innerHTML =
      '<div style="display:flex;align-items:center;justify-content:center;height:100vh;font-family:sans-serif;">' +
      '<div style="text-align:center"><h2>Impossible de se connecter à Keycloak</h2>' +
      '<p>Vérifiez que Keycloak est démarré sur ' + environment.keycloak.url + '</p>' +
      '<button onclick="location.reload()" style="padding:8px 20px;cursor:pointer">Réessayer</button></div></div>';
  });
