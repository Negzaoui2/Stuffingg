import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ManagerService, TeamQueryParams } from '../../../core/services/manager.service';
import { Collaborator } from '../../../core/models/manager.model';

@Component({
  selector: 'app-team',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './team.component.html',
  styleUrls: ['./team.component.css'],
})
export class TeamComponent implements OnInit, OnDestroy {
  collaborators: Collaborator[] = [];
  total = 0;
  page = 0;
  size = 10;
  isLoading = false;
  errorMessage = '';

  searchQuery = '';
  skillFilter = '';
  availabilityFilter = '';

  // Detail panel
  selectedCollab: Collaborator | null = null;
  isDetailOpen = false;
  isDetailLoading = false;

  private searchTimeout: ReturnType<typeof setTimeout> | null = null;

  constructor(private managerService: ManagerService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading = true;
    this.errorMessage = '';

    const params: TeamQueryParams = { page: this.page, size: this.size };
    if (this.searchQuery.trim()) params.search = this.searchQuery.trim();
    if (this.skillFilter) params.skill = this.skillFilter;
    if (this.availabilityFilter) params.availability = this.availabilityFilter;

    this.managerService.getTeam(params).subscribe({
      next: (res) => {
        this.collaborators = res.items ?? [];
        this.total = res.total ?? 0;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.collaborators = [];
        this.total = 0;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  onSearchChange(): void {
    if (this.searchTimeout) clearTimeout(this.searchTimeout);
    this.searchTimeout = setTimeout(() => { this.page = 0; this.load(); }, 350);
  }

  onFilterChange(): void {
    this.page = 0;
    this.load();
  }

  openDetail(collab: Collaborator): void {
    this.selectedCollab = collab;
    this.isDetailOpen = true;
    if (!collab.assignments || collab.assignments.length === 0) {
      this.isDetailLoading = true;
      this.managerService.getCollaborator(collab.id).subscribe({
        next: (full) => { this.selectedCollab = full; this.isDetailLoading = false; this.cdr.detectChanges(); },
        error: () => { this.isDetailLoading = false; this.cdr.detectChanges(); },
      });
    }
  }

  closeDetail(): void {
    this.isDetailOpen = false;
    this.selectedCollab = null;
  }

  get totalPages(): number {
    return Math.ceil(this.total / this.size);
  }

  prevPage(): void {
    if (this.page > 0) { this.page--; this.load(); }
  }

  nextPage(): void {
    if (this.page + 1 < this.totalPages) { this.page++; this.load(); }
  }

  getInitials(c: Collaborator): string {
    return `${c.firstName.charAt(0)}${c.lastName.charAt(0)}`.toUpperCase();
  }

  getAvailabilityLabel(status: string): string {
    const map: Record<string, string> = {
      STAFFED: 'Staffé',
      AVAILABLE: 'Disponible',
      SOON_AVAILABLE: 'Bientôt dispo',
    };
    return map[status] || status;
  }

  getAvailabilityClass(status: string): string {
    const map: Record<string, string> = {
      STAFFED: 'avail-staffed',
      AVAILABLE: 'avail-available',
      SOON_AVAILABLE: 'avail-soon',
    };
    return map[status] || '';
  }

  get staffedCount(): number {
    return this.collaborators.filter(c => c.availability === 'STAFFED').length;
  }

  get availableCount(): number {
    return this.collaborators.filter(c => c.availability === 'AVAILABLE').length;
  }

  get soonAvailableCount(): number {
    return this.collaborators.filter(c => c.availability === 'SOON_AVAILABLE').length;
  }

  getEffectiveAssignmentStatus(a: { status: string; endDate?: string | null }): 'ACTIVE' | 'COMPLETED' | 'ON_HOLD' {
    if (a.status === 'ACTIVE' && a.endDate) {
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      if (new Date(a.endDate) < today) {
        return 'COMPLETED';
      }
    }
    return a.status as 'ACTIVE' | 'COMPLETED' | 'ON_HOLD';
  }

  ngOnDestroy(): void {
    if (this.searchTimeout) clearTimeout(this.searchTimeout);
  }
}
