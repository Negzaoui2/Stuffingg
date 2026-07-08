import { Component, OnInit, OnDestroy, ChangeDetectorRef, ApplicationRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { User } from '../../../core/models/User.model';
import { UserService, UserQueryParams } from '../../../core/services/user.service';
import { NotificationBellComponent } from '../../../shared/notification-bell/notification-bell.component';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, FormsModule, NotificationBellComponent],
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.css'],
})
export class UsersComponent implements OnInit, OnDestroy {
  users: User[] = [];
  total = 0;
  page = 0;
  size = 10;
  isLoading = false;
  errorMessage = '';

  // Filters
  searchQuery = '';
  roleFilter = '';
  statusFilter = '';

  // Stats
  activeCount = 0;
  inactiveCount = 0;

  // Detail panel
  selectedUser: User | null = null;
  isDetailOpen = false;
  isDetailLoading = false;

  // Delete confirmation
  userToDelete: User | null = null;

  // Track in-flight toggle requests to prevent double-clicks
  togglingIds = new Set<number>();

  // Debounce
  private searchTimeout: ReturnType<typeof setTimeout> | null = null;

  constructor(private userService: UserService, private cdr: ChangeDetectorRef, private appRef: ApplicationRef) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading = true;
    this.errorMessage = '';

    const params: UserQueryParams = { page: this.page, size: this.size };
    if (this.searchQuery.trim()) params.search = this.searchQuery.trim();
    if (this.roleFilter) params.role = this.roleFilter;
    if (this.statusFilter !== '') params.isActive = this.statusFilter === 'true';

    this.userService.getAll(params).subscribe({
      next: (res) => {
        this.users = res.items ?? [];
        this.total = res.total ?? 0;
        this.activeCount = this.users.filter(u => u.isActive).length;
        this.inactiveCount = this.users.filter(u => !u.isActive).length;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les utilisateurs.';
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  onSearchChange(): void {
    if (this.searchTimeout) clearTimeout(this.searchTimeout);
    this.searchTimeout = setTimeout(() => {
      this.page = 0;
      this.load();
    }, 350);
  }

  onFilterChange(): void {
    this.page = 0;
    this.load();
  }

  openDetail(user: User): void {
    this.selectedUser = user;
    this.isDetailOpen = true;
    // Fetch full details if not already loaded
    if (user.skills === undefined && user.assignments === undefined) {
      this.isDetailLoading = true;
      this.userService.getById(user.id).subscribe({
        next: (full) => {
          this.selectedUser = full;
          this.isDetailLoading = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.isDetailLoading = false;
          this.cdr.detectChanges();
        },
      });
    }
  }

  closeDetail(): void {
    this.isDetailOpen = false;
    this.selectedUser = null;
  }

  toggleStatus(user: User, event: Event): void {
    event.stopPropagation();
    if (this.togglingIds.has(user.id)) return; // already in-flight
    this.togglingIds.add(user.id);

    this.userService.toggleStatus(user.id).subscribe({
      next: (updated) => {
        this.togglingIds.delete(user.id);
        const idx = this.users.findIndex(u => u.id === user.id);
        if (idx > -1) {
          this.users[idx] = { ...this.users[idx], isActive: updated.isActive };
        }
        if (this.selectedUser?.id === user.id) {
          this.selectedUser = { ...this.selectedUser, isActive: updated.isActive };
        }
        this.activeCount = this.users.filter(u => u.isActive).length;
        this.inactiveCount = this.users.filter(u => !u.isActive).length;
        this.cdr.detectChanges();
        this.appRef.tick();
      },
      error: () => {
        this.togglingIds.delete(user.id);
        this.cdr.detectChanges();
        this.appRef.tick();
      },
    });
  }

  trackByUserId(index: number, user: User): number {
    return user.id;
  }

  confirmDelete(user: User, event: Event): void {
    event.stopPropagation();
    this.userToDelete = user;
    this.cdr.detectChanges();
  }

  cancelDelete(): void {
    this.userToDelete = null;
    this.cdr.detectChanges();
  }

  doDelete(): void {
    if (!this.userToDelete) return;
    const id = this.userToDelete.id;
    this.userToDelete = null;
    this.cdr.detectChanges();

    this.userService.delete(id).subscribe({
      next: () => {
        this.users = this.users.filter(u => u.id !== id);
        this.total = Math.max(0, this.total - 1);
        if (this.selectedUser?.id === id) this.closeDetail();
        this.activeCount = this.users.filter(u => u.isActive).length;
        this.inactiveCount = this.users.filter(u => !u.isActive).length;
        this.cdr.detectChanges();
        this.appRef.tick();
      },
      error: (err) => {
        console.error('Delete failed:', err);
        if (err?.status === 500) {
          this.errorMessage = 'Impossible de supprimer cet utilisateur car il possède des données liées (affectations, projets…). Désactivez-le à la place.';
        } else {
          this.errorMessage = 'Erreur lors de la suppression de l\'utilisateur.';
        }
        this.cdr.detectChanges();
        this.appRef.tick();
      },
    });
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

  getRoleLabel(role: string): string {
    const map: Record<string, string> = {
      ADMIN: 'Admin',
      DELIVERY_MANAGER: 'Delivery Manager',
      COLLABORATEUR: 'Collaborateur',
    };
    return map[role] || role;
  }

  getRoleClass(role: string): string {
    const map: Record<string, string> = {
      ADMIN: 'role-admin',
      DELIVERY_MANAGER: 'role-dm',
      COLLABORATEUR: 'role-collab',
    };
    return map[role] || 'role-collab';
  }

  getInitials(user: User): string {
    return `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase();
  }

  getAssignmentStatusLabel(status: string): string {
    const map: Record<string, string> = {
      ACTIVE: 'En cours',
      COMPLETED: 'Terminé',
      ON_HOLD: 'En pause',
    };
    return map[status] || status;
  }

  ngOnDestroy(): void {
    if (this.searchTimeout) clearTimeout(this.searchTimeout);
  }
}
