import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CollaboratorService } from '../../../core/services/collaborator.service';
import { LeaveRequest, LeaveBalance, LeaveCreateRequest, PageResponse } from '../../../core/models/collaborator.model';

@Component({
  selector: 'app-collab-leaves',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './leaves.component.html',
  styleUrls: ['./leaves.component.css'],
})
export class CollabLeavesComponent implements OnInit {
  leaves: LeaveRequest[] = [];
  balance: LeaveBalance | null = null;
  total = 0; page = 0; size = 10;
  isLoading = false;
  statusFilter = '';

  // Form
  showForm = false;
  formData: LeaveCreateRequest = { type: 'PAID_LEAVE', startDate: '', endDate: '', reason: '' };
  formError = '';
  formSubmitting = false;

  // Cancel
  cancellingId: number | null = null;

  constructor(private svc: CollaboratorService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.loadBalance();
    this.load();
  }

  loadBalance(): void {
    this.svc.getLeaveBalance().subscribe({
      next: (b) => { this.balance = b; this.cdr.detectChanges(); },
      error: () => { this.balance = { paidLeaveRemaining: 0, paidLeaveTotal: 0, paidLeaveUsed: 0, rttRemaining: 0, rttTotal: 0, rttUsed: 0, sickLeaveTaken: 0 }; this.cdr.detectChanges(); },
    });
  }

  load(): void {
    this.isLoading = true;
    const params: any = { page: this.page, size: this.size };
    if (this.statusFilter) params.status = this.statusFilter;
    this.svc.getLeaves(params).subscribe({
      next: (res) => { this.leaves = res.items ?? []; this.total = res.total ?? 0; this.isLoading = false; this.cdr.detectChanges(); },
      error: () => { this.leaves = []; this.isLoading = false; this.cdr.detectChanges(); },
    });
  }

  onFilterChange(): void { this.page = 0; this.load(); }

  openForm(): void {
    this.formData = { type: 'PAID_LEAVE', startDate: '', endDate: '', reason: '' };
    this.formError = '';
    this.showForm = true;
  }

  closeForm(): void { this.showForm = false; }

  submitLeave(): void {
    if (!this.formData.startDate || !this.formData.endDate) { this.formError = 'Les dates sont requises'; return; }
    if (this.formData.startDate >= this.formData.endDate) { this.formError = 'La date de fin doit être après la date de début'; return; }
    const today = new Date().toISOString().split('T')[0];
    if (this.formData.startDate < today) { this.formError = 'La date de début doit être dans le futur'; return; }

    this.formSubmitting = true;
    this.formError = '';
    this.svc.createLeave(this.formData).subscribe({
      next: () => {
        this.formSubmitting = false;
        this.showForm = false;
        this.cdr.detectChanges();
        this.load();
        this.loadBalance();
      },
      error: (err) => {
        this.formSubmitting = false;
        this.formError = err?.error?.message ?? 'Erreur lors de la création';
        this.cdr.detectChanges();
      },
    });
  }

  cancelLeave(id: number): void {
    this.cancellingId = id;
    this.svc.cancelLeave(id).subscribe({
      next: () => { this.cancellingId = null; this.cdr.detectChanges(); this.load(); this.loadBalance(); },
      error: () => { this.cancellingId = null; this.cdr.detectChanges(); },
    });
  }

  get totalPages(): number { return Math.ceil(this.total / this.size); }
  prevPage(): void { if (this.page > 0) { this.page--; this.load(); } }
  nextPage(): void { if (this.page + 1 < this.totalPages) { this.page++; this.load(); } }

  getTypeLabel(t: string): string {
    const m: Record<string, string> = { PAID_LEAVE: 'Congé payé', RTT: 'RTT', SICK_LEAVE: 'Maladie', UNPAID_LEAVE: 'Sans solde' };
    return m[t] ?? t;
  }
  getStatusLabel(s: string): string {
    const m: Record<string, string> = { PENDING: 'En attente', APPROVED: 'Approuvé', REJECTED: 'Refusé' };
    return m[s] ?? s;
  }
  getStatusClass(s: string): string {
    const m: Record<string, string> = { PENDING: 'st-pending', APPROVED: 'st-approved', REJECTED: 'st-rejected' };
    return m[s] ?? '';
  }
}
