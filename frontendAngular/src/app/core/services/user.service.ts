import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { User } from '../models/User.model';
import { environment } from '../../../environments/environment';

export interface UserQueryParams {
  page?: number;
  size?: number;
  search?: string;
  role?: string;
  isActive?: boolean;
}

export interface UserPage {
  items: User[];
  total: number;
  page: number;
  size: number;
  totalPages?: number;
}

export interface ToggleStatusResponse {
  id: number;
  isActive: boolean;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly baseUrl = `${environment.apiUrl}/admin/users`;

  constructor(private http: HttpClient) {}

  getAll(params: UserQueryParams = {}): Observable<UserPage> {
    let httpParams = new HttpParams();
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size);
    if (params.search) httpParams = httpParams.set('search', params.search);
    if (params.role) httpParams = httpParams.set('role', params.role);
    if (params.isActive !== undefined) httpParams = httpParams.set('isActive', params.isActive);
    return this.http.get<UserPage>(this.baseUrl, { params: httpParams });
  }

  getById(id: number): Observable<User> {
    return this.http.get<User>(`${this.baseUrl}/${id}`);
  }

  toggleStatus(id: number): Observable<ToggleStatusResponse> {
    return this.http.put<ToggleStatusResponse>(`${this.baseUrl}/${id}/toggle-status`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
