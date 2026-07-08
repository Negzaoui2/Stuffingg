import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Departement, CreateDepartementRequest } from '../models/departement.model';

@Injectable({ providedIn: 'root' })
export class DepartementService {
  private readonly baseUrl = `${environment.apiUrl}/admin/departements`;

  constructor(private http: HttpClient) {}

  /** Liste de tous les départements */
  getAll(): Observable<Departement[]> {
    return this.http.get<Departement[]>(this.baseUrl);
  }

  /** Détail d'un département */
  getById(id: number): Observable<Departement> {
    return this.http.get<Departement>(`${this.baseUrl}/${id}`);
  }

  /** Création d'un département */
  create(req: CreateDepartementRequest): Observable<Departement> {
    return this.http.post<Departement>(this.baseUrl, req);
  }

  /** Modification d'un département */
  update(id: number, req: CreateDepartementRequest): Observable<Departement> {
    return this.http.put<Departement>(`${this.baseUrl}/${id}`, req);
  }

  /** Suppression d'un département */
  delete(id: number): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.baseUrl}/${id}`);
  }
}
