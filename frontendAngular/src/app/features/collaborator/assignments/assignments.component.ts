import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CollaboratorService } from '../../../core/services/collaborator.service';
import { CollaboratorAssignment, AssignmentDetail, PageResponse } from '../../../core/models/collaborator.model';

@Component({
  selector: 'app-collab-assignments',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './assignments.component.html',
  styleUrls: ['./assignments.component.css'],
})
export class CollabAssignmentsComponent implements OnInit {
  assignments: CollaboratorAssignment[] = [];
  total = 0; page = 0; size = 10;
  isLoading = false;
  statusFilter = '';

  // Detail modal
  showDetail = false;
  detailLoading = false;
  selectedAssignment: AssignmentDetail | null = null;
  completingId: number | null = null;

  constructor(private svc: CollaboratorService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.isLoading = true;
    const params: any = { page: this.page, size: this.size };
    if (this.statusFilter) params.status = this.statusFilter;
    this.svc.getAssignments(params).subscribe({
      next: (res) => { this.assignments = res.items ?? []; this.total = res.total ?? 0; this.isLoading = false; this.cdr.detectChanges(); },
      error: () => { this.assignments = []; this.isLoading = false; this.cdr.detectChanges(); },
    });
  }

  onFilterChange(): void { this.page = 0; this.load(); }

  openDetail(a: CollaboratorAssignment): void {
    this.showDetail = true;
    this.detailLoading = true;
    this.svc.getAssignment(a.id).subscribe({
      next: (d) => { this.selectedAssignment = d; this.detailLoading = false; this.cdr.detectChanges(); },
      error: () => { this.detailLoading = false; this.cdr.detectChanges(); },
    });
  }

  closeDetail(): void { this.showDetail = false; this.selectedAssignment = null; this.completingId = null; }

  completeAssignment(): void {
    if (!this.selectedAssignment || this.completingId !== null) return;
    this.completingId = this.selectedAssignment.id;
    this.svc.completeAssignment(this.selectedAssignment.id).subscribe({
      next: () => {
        this.closeDetail();
        this.load();
      },
      error: () => {
        this.completingId = null;
        this.cdr.detectChanges();
      },
    });
  }

  get totalPages(): number { return Math.ceil(this.total / this.size); }
  prevPage(): void { if (this.page > 0) { this.page--; this.load(); } }
  nextPage(): void { if (this.page + 1 < this.totalPages) { this.page++; this.load(); } }

  getStatusLabel(s: string): string {
    const m: Record<string, string> = { ACTIVE: 'En cours', COMPLETED: 'Terminé', ON_HOLD: 'En pause', PLANNED: 'Planifié' };
    return m[s] ?? s;
  }
  getStatusClass(s: string): string {
    const m: Record<string, string> = { ACTIVE: 'st-active', COMPLETED: 'st-completed', ON_HOLD: 'st-hold', PLANNED: 'st-planned' };
    return m[s] ?? '';
  }
}
