// DEPRECATED: Token injection is now handled by KeycloakBearerInterceptor.
// This file is kept for reference and will be removed in a future cleanup.
import { HttpInterceptorFn } from '@angular/common/http';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => next(req);
