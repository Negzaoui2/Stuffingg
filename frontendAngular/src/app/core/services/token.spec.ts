import { TestBed } from '@angular/core/testing';

import { Token } from './token';

describe('Token', () => {
  let service: Token;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Token);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should save and clear token', () => {
    service.saveToken('abc');
    expect(service.getToken()).toBe('abc');
    expect(service.hasToken()).toBe(true);

    service.clearToken();
    expect(service.getToken()).toBeNull();
    expect(service.hasToken()).toBe(false);
  });
});
