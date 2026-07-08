import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CollaboratorDashboard,
  CollaboratorAssignment,
  AssignmentDetail,
  CollaboratorCalendarEvent,
  LeaveRequest,
  LeaveCreateRequest,
  LeaveBalance,
  CollaboratorProfile,
  SkillDto,
  PageResponse,
} from '../models/collaborator.model';

@Injectable({ providedIn: 'root' })
export class CollaboratorService {
  private readonly base = `${environment.apiUrl}/collaborator`;

  constructor(private http: HttpClient) {}

  // ─── Dashboard ──────────────────────
  getDashboard(): Observable<CollaboratorDashboard> {
    return this.http.get<CollaboratorDashboard>(`${this.base}/dashboard`);
  }

  // ─── Assignments ────────────────────
  getAssignments(params: { status?: string; page?: number; size?: number } = {}): Observable<PageResponse<CollaboratorAssignment>> {
    let p = new HttpParams();
    if (params.status) p = p.set('status', params.status);
    if (params.page !== undefined) p = p.set('page', params.page);
    if (params.size !== undefined) p = p.set('size', params.size);
    return this.http.get<PageResponse<CollaboratorAssignment>>(`${this.base}/assignments`, { params: p });
  }

  getAssignment(id: number): Observable<AssignmentDetail> {
    return this.http.get<AssignmentDetail>(`${this.base}/assignments/${id}`);
  }

  completeAssignment(id: number): Observable<any> {
    return this.http.post(`${this.base}/assignments/${id}/complete`, {});
  }

  // ─── Calendar ───────────────────────
  getCalendarEvents(start: string, end: string): Observable<CollaboratorCalendarEvent[]> {
    const p = new HttpParams().set('start', start).set('end', end);
    return this.http.get<CollaboratorCalendarEvent[]>(`${this.base}/calendar/events`, { params: p });
  }

  // ─── Leaves ─────────────────────────
  getLeaves(params: { status?: string; page?: number; size?: number } = {}): Observable<PageResponse<LeaveRequest>> {
    let p = new HttpParams();
    if (params.status) p = p.set('status', params.status);
    if (params.page !== undefined) p = p.set('page', params.page);
    if (params.size !== undefined) p = p.set('size', params.size);
    return this.http.get<PageResponse<LeaveRequest>>(`${this.base}/leaves`, { params: p });
  }

  createLeave(body: LeaveCreateRequest): Observable<LeaveRequest> {
    return this.http.post<LeaveRequest>(`${this.base}/leaves`, body);
  }

  cancelLeave(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/leaves/${id}`);
  }

  getLeaveBalance(): Observable<LeaveBalance> {
    return this.http.get<LeaveBalance>(`${this.base}/leaves/balance`);
  }

  // ─── Profile & Skills ───────────────
  getProfile(): Observable<CollaboratorProfile> {
    return this.http.get<CollaboratorProfile>(`${this.base}/profile`);
  }

  updateProfile(body: { phone: string }): Observable<CollaboratorProfile> {
    return this.http.put<CollaboratorProfile>(`${this.base}/profile`, body);
  }

  updateSkills(skills: { name: string; level: string }[]): Observable<CollaboratorProfile> {
    return this.http.put<CollaboratorProfile>(`${this.base}/profile/skills`, { skills });
  }

  getSkillSuggestions(query: string): Observable<string[]> {
    const p = new HttpParams().set('query', query);
    return this.http.get<string[]>(`${this.base}/skills/suggestions`, { params: p });
  }
}
