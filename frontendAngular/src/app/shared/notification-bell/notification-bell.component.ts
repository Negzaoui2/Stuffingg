import { Component, OnInit, OnDestroy, HostListener, ElementRef, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { NotificationService } from '../../core/services/notification.service';
import { Notification } from '../../core/models/notification.model';

@Component({
  selector: 'app-notification-bell',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notification-bell.component.html',
  styleUrls: ['./notification-bell.component.css'],
})
export class NotificationBellComponent implements OnInit, OnDestroy {
  isOpen = false;
  unreadCount = 0;
  notifications: Notification[] = [];
  isLoading = false;
  errorMessage = '';

  private pollSub?: Subscription;
  private notifSub?: Subscription;

  constructor(
    private notificationService: NotificationService,
    private elementRef: ElementRef,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Polling du compteur toutes les 30s
    this.pollSub = this.notificationService.startPolling(30_000).subscribe();

    // Écouter le compteur
    this.notifSub = this.notificationService.unreadCount.subscribe(
      (count: number) => (this.unreadCount = count)
    );
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
    this.notifSub?.unsubscribe();
  }

  togglePanel(): void {
    this.isOpen = !this.isOpen;
    if (this.isOpen) {
      this.loadNotifications();
    }
  }

  /** Clic en dehors du panneau → ferme */
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (this.isOpen && !this.elementRef.nativeElement.contains(event.target)) {
      this.isOpen = false;
    }
  }

  loadNotifications(): void {
    this.isLoading = true;
    this.errorMessage = '';
    console.log('[NotificationBell] loading notifications...');
    this.notificationService.fetchAll().subscribe({
      next: (list: Notification[]) => {
        console.log('[NotificationBell] received', list.length, 'notifications, isLoading → false');
        this.notifications = list;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        console.error('[NotificationBell] error:', err);
        this.errorMessage = 'Erreur de chargement';
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  markAsRead(notif: Notification): void {
    if (notif.isRead) return;
    this.notificationService.markAsRead(notif.id).subscribe(() => {
      notif.isRead = true;
    });
  }

  markAllAsRead(): void {
    this.notificationService.markAllAsRead().subscribe(() => {
      this.notifications.forEach((n) => (n.isRead = true));
    });
  }

  getIcon(type: string): string {
    switch (type) {
      case 'ACCOUNT_REQUEST':
        return '📋';
      case 'ACCOUNT_APPROVED':
        return '✅';
      case 'ACCOUNT_REJECTED':
        return '❌';
      default:
        return '🔔';
    }
  }

  getTimeAgo(dateStr: string): string {
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMin = Math.floor(diffMs / 60_000);
    if (diffMin < 1) return "À l'instant";
    if (diffMin < 60) return `Il y a ${diffMin} min`;
    const diffH = Math.floor(diffMin / 60);
    if (diffH < 24) return `Il y a ${diffH}h`;
    const diffD = Math.floor(diffH / 24);
    return `Il y a ${diffD}j`;
  }
}
