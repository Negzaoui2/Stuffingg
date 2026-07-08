import { TestBed } from '@angular/core/testing';
import { CanActivateFn } from '@angular/router';
import { Router } from '@angular/router';

import { authGuard } from './auth-guard';
import { Auth } from '../services/auth';

describe('authGuard', () => {
  const executeGuard: CanActivateFn = (...guardParameters) => 
      TestBed.runInInjectionContext(() => authGuard(...guardParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: Auth,
          useValue: {
            isLoggedIn: () => false,
            getCurrentUser: () => null,
          },
        },
        {
          provide: Router,
          useValue: {
            parseUrl: (url: string) => ({ url } as any),
          },
        },
      ],
    });
  });

  it('redirects to /auth/login when logged out', () => {
    const result = executeGuard({ data: {} } as any, {} as any) as any;
    expect(result?.url).toBe('/auth/login');
  });
});
