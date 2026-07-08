import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  LeaveStatsResponse,
  LeaveDetailAdminDto,
  AbsenteeismResponse,
  PageResponse,
} from '../models/admin-report.model';

@Injectable({ providedIn: 'root' })
export class AdminReportService {
  private readonly baseUrl = `${environment.apiUrl}/admin/reports`;

  constructor(private http: HttpClient) {}

  /** Statistiques globales des congés */
  getLeaveStats(): Observable<LeaveStatsResponse> {
    return this.http.get<LeaveStatsResponse>(`${this.baseUrl}/leaves/stats`);
  }

  /** Liste paginée de toutes les demandes de congé (lecture seule) */
  getLeaves(params: {
    status?: string;
    type?: string;
    search?: string;
    page?: number;
    size?: number;
  } = {}): Observable<PageResponse<LeaveDetailAdminDto>> {
    let p = new HttpParams();
    if (params.status) p = p.set('status', params.status);
    if (params.type) p = p.set('type', params.type);
    if (params.search) p = p.set('search', params.search);
    if (params.page !== undefined) p = p.set('page', params.page);
    if (params.size !== undefined) p = p.set('size', params.size);
    return this.http.get<PageResponse<LeaveDetailAdminDto>>(`${this.baseUrl}/leaves`, { params: p });
  }

  /** Taux d'absentéisme */
  getAbsenteeism(): Observable<AbsenteeismResponse> {
    return this.http.get<AbsenteeismResponse>(`${this.baseUrl}/absenteeism`);
  }
}
