import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminReportService } from '../../../core/services/admin-report.service';
import {
  LeaveStatsResponse,
  LeaveDetailAdminDto,
  AbsenteeismResponse,
  PageResponse,
} from '../../../core/models/admin-report.model';

@Component({
  selector: 'app-admin-reports',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reports.component.html',
  styleUrls: ['./reports.component.css'],
})
export class AdminReportsComponent implements OnInit {
  // Stats
  stats: LeaveStatsResponse | null = null;
  absenteeism: AbsenteeismResponse | null = null;
  statsLoading = true;

  // Leaves list
  leaves: LeaveDetailAdminDto[] = [];
  total = 0;
  page = 0;
  size = 15;
  isLoading = false;
  errorMessage = '';

  // Filters
  statusFilter = '';
  typeFilter = '';
  searchQuery = '';

  private searchTimeout: ReturnType<typeof setTimeout> | null = null;

  constructor(private reportService: AdminReportService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.loadStats();
    this.loadLeaves();
  }

  loadStats(): void {
    this.statsLoading = true;
    this.reportService.getLeaveStats().subscribe({
      next: (res) => {
        this.stats = res;
        this.statsLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.statsLoading = false;
        this.cdr.detectChanges();
      },
    });

    this.reportService.getAbsenteeism().subscribe({
      next: (res) => {
        this.absenteeism = res;
        this.cdr.detectChanges();
      },
      error: () => {},
    });
  }

  loadLeaves(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.reportService.getLeaves({
      status: this.statusFilter || undefined,
      type: this.typeFilter || undefined,
      search: this.searchQuery.trim() || undefined,
      page: this.page,
      size: this.size,
    }).subscribe({
      next: (res) => {
        this.leaves = res.items ?? [];
        this.total = res.total ?? 0;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.leaves = [];
        this.total = 0;
        this.isLoading = false;
        this.errorMessage = 'Erreur lors du chargement des données.';
        this.cdr.detectChanges();
      },
    });
  }

  onSearch(): void {
    if (this.searchTimeout) clearTimeout(this.searchTimeout);
    this.searchTimeout = setTimeout(() => {
      this.page = 0;
      this.loadLeaves();
    }, 350);
  }

  setStatusFilter(status: string): void {
    this.statusFilter = status;
    this.page = 0;
    this.loadLeaves();
  }

  setTypeFilter(type: string): void {
    this.typeFilter = type;
    this.page = 0;
    this.loadLeaves();
  }

  get totalPages(): number {
    return Math.ceil(this.total / this.size);
  }

  prevPage(): void {
    if (this.page > 0) { this.page--; this.loadLeaves(); }
  }

  nextPage(): void {
    if (this.page < this.totalPages - 1) { this.page++; this.loadLeaves(); }
  }

  // ─── Helpers ────────────────────────
  leaveTypeLabel(type: string): string {
    const map: Record<string, string> = {
      PAID_LEAVE: 'Congé payé', RTT: 'RTT',
      SICK_LEAVE: 'Arrêt maladie', UNPAID_LEAVE: 'Congé sans solde',
    };
    return map[type] || type;
  }

  leaveTypeClass(type: string): string {
    const map: Record<string, string> = {
      PAID_LEAVE: 'blue', RTT: 'purple',
      SICK_LEAVE: 'red', UNPAID_LEAVE: 'gray',
    };
    return map[type] || 'gray';
  }

  statusLabel(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'En attente', APPROVED: 'Approuvé', REJECTED: 'Rejeté',
    };
    return map[status] || status;
  }

  statusClass(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'pending', APPROVED: 'approved', REJECTED: 'rejected',
    };
    return map[status] || '';
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  formatDateTime(dateStr: string): string {
    if (!dateStr) return '—';
    const d = new Date(dateStr);
    return d.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' })
      + ' ' + d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  }
}
