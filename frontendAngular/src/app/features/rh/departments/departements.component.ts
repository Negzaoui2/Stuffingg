import { Component, OnInit, ChangeDetectorRef, ApplicationRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { Departement } from '../../../core/models/departement.model';
import { DepartementService } from '../../../core/services/departement.service';
import { User } from '../../../core/models/User.model';
import { UserService } from '../../../core/services/user.service';
import { NotificationBellComponent } from '../../../shared/notification-bell/notification-bell.component';

@Component({
  selector: 'app-departements',
  standalone: true,
  imports: [CommonModule, FormsModule, NotificationBellComponent],
  templateUrl: './departements.component.html',
  styleUrls: ['./departements.component.css'],
})
export class DepartementsComponent implements OnInit {
  departements: Departement[] = [];
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  // Search (filtrage côté client)
  searchQuery = '';

  // Formulaire création / édition
  showForm = false;
  editingDepartement: Departement | null = null;
  formName = '';
  formSubmitting = false;
  formError = '';

  // Confirmation de suppression
  departementToDelete: Departement | null = null;
  deleteError = '';
  deletingId: number | null = null;

  // Panneau de détail : collaborateurs du département
  selectedDepartement: Departement | null = null;
  isPanelOpen = false;
  panelLoading = false;
  panelError = '';
  panelCollaborators: User[] = [];

  private successTimeout: ReturnType<typeof setTimeout> | null = null;

  constructor(
    private departementService: DepartementService,
    private userService: UserService,
    private cdr: ChangeDetectorRef,
    private appRef: ApplicationRef
  ) {}

  ngOnInit(): void {
    this.load();
  }

  // ─── Chargement ────────────────────────
  load(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.departementService.getAll().subscribe({
      next: (res) => {
        this.departements = res ?? [];
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.departements = [];
        this.errorMessage = 'Impossible de charger les départements.';
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  get filteredDepartements(): Departement[] {
    const q = this.searchQuery.trim().toLowerCase();
    if (!q) return this.departements;
    return this.departements.filter((d) => d.name.toLowerCase().includes(q));
  }

  get totalEmployees(): number {
    return this.departements.reduce((acc, d) => acc + (d.employeeCount ?? 0), 0);
  }

  // ─── Panneau de détail (collaborateurs) ──────────────────────────
  openDepartement(dep: Departement): void {
    this.selectedDepartement = dep;
    this.isPanelOpen = true;
    this.panelError = '';
    this.panelCollaborators = [];
    this.panelLoading = true;
    this.cdr.detectChanges();

    // L'API départements ne fournit pas la liste des collaborateurs :
    // on récupère les utilisateurs et on filtre par nom de département.
    this.userService.getAll({ page: 0, size: 500 }).subscribe({
      next: (res) => {
        const items = res.items ?? [];
        this.panelCollaborators = items.filter(
          (u) => (u.department ?? '').trim().toLowerCase() === dep.name.trim().toLowerCase()
        );
        this.panelLoading = false;
        this.cdr.detectChanges();
        this.appRef.tick();
      },
      error: () => {
        this.panelError = 'Impossible de charger les collaborateurs.';
        this.panelLoading = false;
        this.cdr.detectChanges();
        this.appRef.tick();
      },
    });
  }

  closePanel(): void {
    this.isPanelOpen = false;
    this.selectedDepartement = null;
    this.panelCollaborators = [];
    this.panelError = '';
    this.cdr.detectChanges();
  }

  // ─── Formulaire création / édition ────────────────────────
  openCreateForm(): void {
    this.editingDepartement = null;
    this.formName = '';
    this.formError = '';
    this.showForm = true;
    this.cdr.detectChanges();
  }

  openEditForm(dep: Departement, event: Event): void {
    event.stopPropagation();
    this.editingDepartement = dep;
    this.formName = dep.name;
    this.formError = '';
    this.showForm = true;
    this.cdr.detectChanges();
  }

  closeForm(): void {
    this.showForm = false;
    this.editingDepartement = null;
    this.formName = '';
    this.formError = '';
    this.cdr.detectChanges();
  }

  submitForm(): void {
    const name = this.formName.trim();
    if (!name) {
      this.formError = 'Le nom du département est requis.';
      return;
    }

    this.formSubmitting = true;
    this.formError = '';

    const obs = this.editingDepartement
      ? this.departementService.update(this.editingDepartement.id, { name })
      : this.departementService.create({ name });

    const isEdit = !!this.editingDepartement;

    obs.subscribe({
      next: () => {
        this.formSubmitting = false;
        this.showForm = false;
        this.editingDepartement = null;
        this.formName = '';
        this.showSuccess(isEdit ? 'Département modifié avec succès.' : 'Département créé avec succès.');
        this.load();
        this.cdr.detectChanges();
        this.appRef.tick();
      },
      error: (err: unknown) => {
        this.formSubmitting = false;
        this.formError = this.extractErrorMessage(err, 'Erreur lors de l\'enregistrement.');
        this.cdr.detectChanges();
        this.appRef.tick();
      },
    });
  }

  // ─── Suppression ────────────────────────
  confirmDelete(dep: Departement, event: Event): void {
    event.stopPropagation();
    this.deleteError = '';
    this.departementToDelete = dep;
    this.cdr.detectChanges();
  }

  cancelDelete(): void {
    this.departementToDelete = null;
    this.deleteError = '';
    this.cdr.detectChanges();
  }

  doDelete(): void {
    if (!this.departementToDelete) return;
    const dep = this.departementToDelete;
    this.deletingId = dep.id;
    this.deleteError = '';

    this.departementService.delete(dep.id).subscribe({
      next: (res) => {
        this.deletingId = null;
        this.departementToDelete = null;
        this.showSuccess(res?.message || 'Département supprimé avec succès.');
        this.load();
        this.cdr.detectChanges();
        this.appRef.tick();
      },
      error: (err: unknown) => {
        this.deletingId = null;
        this.deleteError = this.extractErrorMessage(
          err,
          'Erreur lors de la suppression du département.'
        );
        this.cdr.detectChanges();
        this.appRef.tick();
      },
    });
  }

  // ─── Helpers ────────────────────────
  getInitials(name: string): string {
    const parts = name.trim().split(/\s+/);
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return (parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
  }

  getUserInitials(u: User): string {
    return `${u.firstName?.charAt(0) ?? ''}${u.lastName?.charAt(0) ?? ''}`.toUpperCase();
  }

  getRoleLabel(role: string): string {
    const map: Record<string, string> = {
      ADMIN: 'Admin',
      DELIVERY_MANAGER: 'Delivery Manager',
      COLLABORATEUR: 'Collaborateur',
    };
    return map[role] || role;
  }

  private showSuccess(message: string): void {
    this.successMessage = message;
    if (this.successTimeout) clearTimeout(this.successTimeout);
    this.successTimeout = setTimeout(() => {
      this.successMessage = '';
      this.cdr.detectChanges();
      this.appRef.tick();
    }, 4000);
  }

  /** Extrait le message d'erreur du format ApiErrorResponse du backend */
  private extractErrorMessage(err: unknown, fallback: string): string {
    const e = err as { error?: { message?: string } };
    return e?.error?.message?.trim() || fallback;
  }

  trackByDepId(_index: number, dep: Departement): number {
    return dep.id;
  }
}
