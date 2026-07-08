import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ManagerService } from '../../../core/services/manager.service';
import { LeaveRequestManager, LeaveStatus } from '../../../core/models/manager.model';

@Component({
  selector: 'app-manager-leaves',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './leaves.component.html',
  styleUrls: ['./leaves.component.css'],
})
export class ManagerLeavesComponent implements OnInit {
  leaves: LeaveRequestManager[] = [];
  total = 0;
  page = 0;
  size = 10;
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  activeFilter: LeaveStatus | '' = '';

  // Confirmation modal
  showConfirmModal = false;
  confirmAction: 'approve' | 'reject' = 'approve';
  confirmLeave: LeaveRequestManager | null = null;
  isSubmitting = false;

  constructor(private managerService: ManagerService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading = true;
    this.errorMessage = '';
    const status = this.activeFilter || undefined;
    this.managerService.getTeamLeaves(status, this.page, this.size).subscribe({
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
        this.errorMessage = 'Erreur lors du chargement des demandes de congé.';
        this.cdr.detectChanges();
      },
    });
  }

  setFilter(filter: LeaveStatus | ''): void {
    this.activeFilter = filter;
    this.page = 0;
    this.load();
  }

  get totalPages(): number {
    return Math.ceil(this.total / this.size);
  }

  prevPage(): void {
    if (this.page > 0) {
      this.page--;
      this.load();
    }
  }

  nextPage(): void {
    if (this.page < this.totalPages - 1) {
      this.page++;
      this.load();
    }
  }

  // ─── Actions ────────────────────────
  openConfirm(leave: LeaveRequestManager, action: 'approve' | 'reject'): void {
    this.confirmLeave = leave;
    this.confirmAction = action;
    this.showConfirmModal = true;
  }

  closeConfirm(): void {
    this.showConfirmModal = false;
    this.confirmLeave = null;
    this.isSubmitting = false;
  }

  executeAction(): void {
    if (!this.confirmLeave) return;
    this.isSubmitting = true;

    const obs = this.confirmAction === 'approve'
      ? this.managerService.approveLeave(this.confirmLeave.id)
      : this.managerService.rejectLeave(this.confirmLeave.id);

    obs.subscribe({
      next: () => {
        this.successMessage = this.confirmAction === 'approve'
          ? 'Congé approuvé avec succès.'
          : 'Congé rejeté avec succès.';
        this.closeConfirm();
        this.load();
        setTimeout(() => (this.successMessage = ''), 4000);
      },
      error: () => {
        this.errorMessage = 'Erreur lors du traitement de la demande.';
        this.isSubmitting = false;
        this.cdr.detectChanges();
        setTimeout(() => (this.errorMessage = ''), 4000);
      },
    });
  }

  // ─── Helpers ────────────────────────
  leaveTypeLabel(type: string): string {
    const map: Record<string, string> = {
      PAID_LEAVE: 'Congé payé',
      RTT: 'RTT',
      SICK_LEAVE: 'Arrêt maladie',
      UNPAID_LEAVE: 'Congé sans solde',
    };
    return map[type] || type;
  }

  leaveTypeClass(type: string): string {
    const map: Record<string, string> = {
      PAID_LEAVE: 'blue',
      RTT: 'purple',
      SICK_LEAVE: 'red',
      UNPAID_LEAVE: 'gray',
    };
    return map[type] || 'gray';
  }

  statusLabel(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'En attente',
      APPROVED: 'Approuvé',
      REJECTED: 'Rejeté',
    };
    return map[status] || status;
  }

  statusClass(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'pending',
      APPROVED: 'approved',
      REJECTED: 'rejected',
    };
    return map[status] || '';
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  formatDateTime(dateStr: string): string {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' }) +
      ' à ' + d.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  }
}
