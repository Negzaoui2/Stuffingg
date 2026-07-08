import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap, interval, switchMap, startWith, catchError, of, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Notification } from '../models/notification.model';
import { MessageResponse } from '../models/message-response.model';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private apiUrl = `${environment.apiUrl}/notifications`;

  /** Nombre de non-lues (observable réactif pour la cloche) */
  private unreadCount$ = new BehaviorSubject<number>(0);

  /** Liste des notifications (cache local) */
  private notifications$ = new BehaviorSubject<Notification[]>([]);

  constructor(private http: HttpClient) {}

  // ─── Observables publics ─────────────────────────────────

  get unreadCount(): Observable<number> {
    return this.unreadCount$.asObservable();
  }

  get unreadCountValue(): number {
    return this.unreadCount$.value;
  }

  get notifications(): Observable<Notification[]> {
    return this.notifications$.asObservable();
  }

  // ─── API calls ───────────────────────────────────────────

  /** Toutes les notifications de l'utilisateur connecté */
  fetchAll(): Observable<Notification[]> {
    return this.http.get<any[]>(this.apiUrl).pipe(
      map((list) => (list ?? []).map((n) => this.normalizeNotification(n))),
      tap((list) => {
        console.log('[NotificationService] fetchAll →', list.length, 'notifications');
        this.notifications$.next(list);
      }),
      catchError((err) => {
        console.error('[NotificationService] fetchAll error:', err?.status, err?.message || err);
        return of([] as Notification[]);
      })
    );
  }

  /** Notifications non lues */
  fetchUnread(): Observable<Notification[]> {
    return this.http.get<Notification[]>(`${this.apiUrl}/unread`);
  }

  /** Nombre de non-lues (met à jour le BehaviorSubject) */
  fetchUnreadCount(): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/count`).pipe(
      tap((count) => {
        console.log('[NotificationService] unread count →', count);
        this.unreadCount$.next(count);
      }),
      catchError((err) => {
        console.error('[NotificationService] count error:', err?.status, err?.message || err);
        return of(0);
      })
    );
  }

  /** Marquer une notification comme lue */
  markAsRead(id: number): Observable<MessageResponse> {
    return this.http.put<MessageResponse>(`${this.apiUrl}/${id}/read`, {}).pipe(
      tap(() => {
        // Met à jour le cache local
        const current = this.notifications$.value.map((n) =>
          n.id === id ? { ...n, isRead: true } : n
        );
        this.notifications$.next(current);
        this.unreadCount$.next(Math.max(0, this.unreadCount$.value - 1));
      })
    );
  }

  /** Marquer toutes comme lues */
  markAllAsRead(): Observable<MessageResponse> {
    return this.http.put<MessageResponse>(`${this.apiUrl}/read-all`, {}).pipe(
      tap(() => {
        const current = this.notifications$.value.map((n) => ({ ...n, isRead: true }));
        this.notifications$.next(current);
        this.unreadCount$.next(0);
      })
    );
  }

  // ─── Polling (optionnel) ────────────────────────────────

  /**
   * Lance un polling toutes les `intervalMs` ms pour rafraîchir le compteur.
   * Retourne un Observable à souscrire (se désabonner pour stopper).
   */
  startPolling(intervalMs = 30_000): Observable<number> {
    return interval(intervalMs).pipe(
      startWith(0),
      switchMap(() => this.fetchUnreadCount())
    );
  }

  /** Réinitialise les caches (appeler au logout) */
  reset(): void {
    this.unreadCount$.next(0);
    this.notifications$.next([]);
  }

  /**
   * Jackson sérialise `boolean isRead` en `"read"` (convention JavaBean).
   * On normalise ici pour que le front ait toujours `isRead`.
   */
  private normalizeNotification(raw: any): Notification {
    return {
      id: raw.id,
      message: raw.message,
      type: raw.type,
      createdAt: raw.createdAt,
      targetUser: raw.targetUser ?? null,
      isRead: raw.isRead ?? raw.read ?? false,
    };
  }
}
