import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AccountRequest,
  AccountRequestStatus,
  ApproveRequestDto,
  RejectRequestDto,
  ManagerSummary,
} from '../models/account-request.model';

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // page courante (0-based)
  size: number;   // taille de page
}

export interface AccountRequestStats {
  total: number;
  pending: number;
  approved: number;
  rejected: number;
}

@Injectable({ providedIn: 'root' })
export class AccountRequestsService {
  private apiUrl = `${environment.apiUrl}/hr/account-requests`;

  constructor(private http: HttpClient) {}

  /**
   * Récupère les demandes côté API avec filtre optionnel + pagination.
   * Retourne un format unifié: { items, total, page, size }
   */
  getAll(opts?: {
    status?: AccountRequestStatus;
    page?: number;   // 0-based
    size?: number;
    sort?: string;   // ex: 'createdAt,DESC'
  }): Observable<{ items: AccountRequest[]; total: number; page: number; size: number }> {
    let params = new HttpParams()
      .set('page', String(opts?.page ?? 0))
      .set('size', String(opts?.size ?? 10));

    if (opts?.status) params = params.set('status', opts.status);
    if (opts?.sort) params = params.set('sort', opts.sort);

    return this.http.get<unknown>(this.apiUrl, { params }).pipe(
      map((res) => {
        if (res == null) {
          return { items: [], total: 0, page: opts?.page ?? 0, size: opts?.size ?? 10 };
        }

        // Spring Page<T>
        if (typeof res === 'object') {
          const anyRes = res as any;
          if (Array.isArray(anyRes.content)) {
            return {
              items: anyRes.content as AccountRequest[],
              total: Number(anyRes.totalElements ?? anyRes.content.length ?? 0),
              page: Number(anyRes.number ?? opts?.page ?? 0),
              size: Number(anyRes.size ?? opts?.size ?? 10),
            };
          }
          // Autres formats (fallbacks)
          if (Array.isArray(anyRes.data)) {
            return {
              items: anyRes.data as AccountRequest[],
              total: Number(anyRes.total ?? anyRes.data.length ?? 0),
              page: Number(opts?.page ?? 0),
              size: Number(opts?.size ?? anyRes.data.length ?? 10),
            };
          }
          if (Array.isArray(anyRes.items)) {
            return {
              items: anyRes.items as AccountRequest[],
              total: Number(anyRes.total ?? anyRes.items.length ?? 0),
              page: Number(opts?.page ?? 0),
              size: Number(opts?.size ?? anyRes.items.length ?? 10),
            };
          }
        }

        // Tableau brut
        if (Array.isArray(res)) {
          return {
            items: res as AccountRequest[],
            total: (res as any[]).length,
            page: Number(opts?.page ?? 0),
            size: Number(opts?.size ?? (res as any[]).length ?? 10),
          };
        }

        return { items: [], total: 0, page: opts?.page ?? 0, size: opts?.size ?? 10 };
      })
    );
  }

  getStats(): Observable<AccountRequestStats> {
    return this.http.get<AccountRequestStats>(`${this.apiUrl}/stats`);
  }

  approveRequest(id: number | string, data: ApproveRequestDto): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${id}/approve`, data);
  }

  rejectRequest(id: number | string, data: RejectRequestDto): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${id}/reject`, data ?? {});
  }

  getManagers(): Observable<ManagerSummary[]> {
    return this.http.get<ManagerSummary[]>(`${environment.apiUrl}/hr/managers`);
  }
}