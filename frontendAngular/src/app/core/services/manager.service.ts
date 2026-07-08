import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ManagerDashboard,
  Collaborator,
  Project,
  ProjectCreateDto,
  ProjectStatus,
  AssignmentCreateDto,
  PageResponse,
  LeaveRequestManager,
} from '../models/manager.model';

export interface TeamQueryParams {
  page?: number;
  size?: number;
  search?: string;
  skill?: string;
  availability?: string;
}

export interface ProjectQueryParams {
  page?: number;
  size?: number;
  search?: string;
  status?: string;
}

@Injectable({ providedIn: 'root' })
export class ManagerService {
  private readonly baseUrl = `${environment.apiUrl}/manager`;

  constructor(private http: HttpClient) {}

  /** KPI du tableau de bord */
  getDashboard(): Observable<ManagerDashboard> {
    return this.http.get<ManagerDashboard>(`${this.baseUrl}/dashboard`);
  }

  /** Liste des collaborateurs de l'équipe */
  getTeam(params: TeamQueryParams = {}): Observable<PageResponse<Collaborator>> {
    let p = new HttpParams();
    if (params.page !== undefined) p = p.set('page', params.page);
    if (params.size !== undefined) p = p.set('size', params.size);
    if (params.search) p = p.set('search', params.search);
    if (params.skill) p = p.set('skill', params.skill);
    if (params.availability) p = p.set('availability', params.availability);
    return this.http.get<PageResponse<Collaborator>>(`${this.baseUrl}/team`, { params: p });
  }

  /** Détail d'un collaborateur */
  getCollaborator(id: number): Observable<Collaborator> {
    return this.http.get<Collaborator>(`${this.baseUrl}/team/${id}`);
  }

  /** Liste des projets gérés */
  getProjects(params: ProjectQueryParams = {}): Observable<PageResponse<Project>> {
    let p = new HttpParams();
    if (params.page !== undefined) p = p.set('page', params.page);
    if (params.size !== undefined) p = p.set('size', params.size);
    if (params.search) p = p.set('search', params.search);
    if (params.status) p = p.set('status', params.status);
    return this.http.get<PageResponse<Project>>(`${this.baseUrl}/projects`, { params: p });
  }

  /** Détail d'un projet */
  getProject(id: number): Observable<Project> {
    return this.http.get<Project>(`${this.baseUrl}/projects/${id}`);
  }

  /** Créer un projet */
  createProject(dto: ProjectCreateDto): Observable<Project> {
    return this.http.post<Project>(`${this.baseUrl}/projects`, dto);
  }

  /** Modifier un projet */
  updateProject(id: number, dto: ProjectCreateDto): Observable<Project> {
    return this.http.put<Project>(`${this.baseUrl}/projects/${id}`, dto);
  }

  /** Supprimer (archiver) un projet */
  deleteProject(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/projects/${id}`);
  }

  /** Changer le statut d'un projet */
  changeProjectStatus(id: number, status: ProjectStatus): Observable<Project> {
    return this.http.patch<Project>(`${this.baseUrl}/projects/${id}/status`, null, {
      params: new HttpParams().set('status', status),
    });
  }

  /** Assigner un collaborateur à un projet */
  assignCollaborator(projectId: number, dto: AssignmentCreateDto): Observable<any> {
    return this.http.post(`${this.baseUrl}/projects/${projectId}/assignments`, dto);
  }

  /** Retirer un collaborateur (marque COMPLETED) */
  unassign(assignmentId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/assignments/${assignmentId}`);
  }

  /** Liste des demandes de congé de l'équipe */
  getTeamLeaves(status?: string, page?: number, size?: number): Observable<PageResponse<LeaveRequestManager>> {
    let p = new HttpParams();
    if (status) p = p.set('status', status);
    if (page !== undefined) p = p.set('page', page);
    if (size !== undefined) p = p.set('size', size);
    return this.http.get<PageResponse<LeaveRequestManager>>(`${this.baseUrl}/leaves`, { params: p });
  }

  /** Approuver une demande de congé */
  approveLeave(leaveId: number): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/leaves/${leaveId}/approve`, null);
  }

  /** Rejeter une demande de congé */
  rejectLeave(leaveId: number): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/leaves/${leaveId}/reject`, null);
  }
}
