import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CollaboratorService } from '../../../core/services/collaborator.service';
import { CollaboratorDashboard } from '../../../core/models/collaborator.model';

@Component({
  selector: 'app-collab-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
})
export class CollabDashboardComponent implements OnInit {
  dashboard: CollaboratorDashboard | null = null;
  isLoading = true;

  constructor(private svc: CollaboratorService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.isLoading = true;
    this.svc.getDashboard().subscribe({
      next: (d) => { this.dashboard = d; this.isLoading = false; this.cdr.detectChanges(); },
      error: () => {
        this.dashboard = {
          fullName: 'Collaborateur', department: '', currentAssignment: null,
          leaveBalance: { paidLeaveRemaining: 0, paidLeaveTotal: 0, paidLeaveUsed: 0, rttRemaining: 0, rttTotal: 0, rttUsed: 0, sickLeaveTaken: 0 },
          skillCount: 0, upcomingEvents: [], recentNotifications: [],
        };
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  getEventIcon(type: string): string {
    const m: Record<string, string> = { ASSIGNMENT_END: '📋', LEAVE_APPROVED: '✅', LEAVE_PENDING: '⏳' };
    return m[type] ?? '📌';
  }

  getNotifClass(type: string): string {
    const m: Record<string, string> = { SUCCESS: 'notif-success', INFO: 'notif-info', WARNING: 'notif-warning', ERROR: 'notif-error' };
    return m[type] ?? 'notif-info';
  }
}
