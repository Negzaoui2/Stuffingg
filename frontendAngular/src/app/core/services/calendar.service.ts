import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CalendarResource, CalendarEvent } from '../models/calendar.model';

@Injectable({ providedIn: 'root' })
export class CalendarService {
  private readonly baseUrl = `${environment.apiUrl}/manager/calendar`;

  constructor(private http: HttpClient) {}

  getResources(): Observable<CalendarResource[]> {
    return this.http.get<CalendarResource[]>(`${this.baseUrl}/resources`);
  }

  getEvents(start: string, end: string): Observable<CalendarEvent[]> {
    const params = new HttpParams().set('start', start).set('end', end);
    return this.http.get<CalendarEvent[]>(`${this.baseUrl}/events`, { params });
  }
}
