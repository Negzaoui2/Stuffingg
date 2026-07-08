import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ManagerService, ProjectQueryParams, TeamQueryParams } from '../../../core/services/manager.service';
import { Project, ProjectCreateDto, ProjectStatus, Collaborator, AssignmentCreateDto } from '../../../core/models/manager.model';

@Component({
  selector: 'app-projects',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './projects.component.html',
  styleUrls: ['./projects.component.css'],
})
export class ProjectsComponent implements OnInit, OnDestroy {
  projects: Project[] = [];
  total = 0;
  page = 0;
  size = 10;
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  searchQuery = '';
  statusFilter = '';
  deletingProjectId: number | null = null;

  // Detail panel
  selectedProject: Project | null = null;
  isDetailOpen = false;
  isDetailLoading = false;

  // Project form modal
  showProjectForm = false;
  editingProject: Project | null = null;
  projectForm: ProjectCreateDto = this.emptyProjectForm();

  projectFormSubmitting = false;
  projectFormError = '';

  // Assign modal
  showAssignForm = false;
  assignForm: AssignmentCreateDto = { collaboratorId: 0, roleName: '', startDate: '', endDate: '' };
  assignFormSubmitting = false;
  assignFormError = '';
  availableCollaborators: Collaborator[] = [];
  loadingCollaborators = false;

  // Unassign
  unassigningId: number | null = null;

  // Status change
  showStatusDropdown = false;
  statusOptions: ProjectStatus[] = ['PLANNED', 'ACTIVE', 'ON_HOLD', 'COMPLETED'];

  private searchTimeout: ReturnType<typeof setTimeout> | null = null;

  constructor(private managerService: ManagerService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
  }

  // ─── Data loading ────────────────────────
  load(): void {
    this.isLoading = true;
    this.errorMessage = '';

    const params: ProjectQueryParams = { page: this.page, size: this.size };
    if (this.searchQuery.trim()) params.search = this.searchQuery.trim();
    if (this.statusFilter) params.status = this.statusFilter;

    this.managerService.getProjects(params).subscribe({
      next: (res) => {
        this.projects = res.items ?? [];
        this.total = res.total ?? 0;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.projects = [];
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

  // ─── Detail panel ────────────────────────
  openDetail(project: Project): void {
    this.selectedProject = project;
    this.isDetailOpen = true;
    this.isDetailLoading = true;
    this.managerService.getProject(project.id).subscribe({
      next: (full) => { this.selectedProject = full;   
 this.isDetailLoading = false; this.cdr.detectChanges(); },
      error: () => { this.isDetailLoading = false; this.cdr.detectChanges(); },
    });
  }

  closeDetail(): void {
    this.isDetailOpen = false;
    this.selectedProject = null;
    this.showStatusDropdown = false;
  }

  refreshDetail(): void {
    if (!this.selectedProject) return;
    this.managerService.getProject(this.selectedProject.id).subscribe({
      next: (full) => { this.selectedProject = full; this.cdr.detectChanges(); },
      error: () => {},
    });
  }

  // ─── Create/Edit project ────────────────────────
  openCreateForm(): void {
    this.editingProject = null;
    this.projectForm = this.emptyProjectForm();
    this.projectFormError = '';
    this.showProjectForm = true;
  }

  openEditForm(): void {
    if (!this.selectedProject) return;
    this.editingProject = this.selectedProject;
    this.projectForm = {
      name: this.selectedProject.name,
      clientName: this.selectedProject.clientName,
      startDate: this.selectedProject.startDate,
      endDate: this.selectedProject.endDate,
      status: this.selectedProject.status,
      technologies: this.selectedProject.technologies?.join(',') ?? '',
      description: this.selectedProject.description ?? '',
      neededRessource: this.selectedProject.neededRessource ?? '',
    };
    this.projectFormError = '';
    this.showProjectForm = true;
  }

  closeProjectForm(): void {
    this.showProjectForm = false;
    this.editingProject = null;
  }

  submitProject(): void {
    if (!this.projectForm.name || !this.projectForm.clientName || !this.projectForm.startDate) {
      this.projectFormError = 'Le nom, le client et la date de début sont requis.';
      return;
    }

    this.projectFormSubmitting = true;
    this.projectFormError = '';

    const obs = this.editingProject
      ? this.managerService.updateProject(this.editingProject.id, this.projectForm)
      : this.managerService.createProject(this.projectForm);

    obs.subscribe({
      next: () => {
        this.projectFormSubmitting = false;
        this.showProjectForm = false;
        this.showSuccess(this.editingProject ? 'Projet modifié avec succès' : 'Projet créé avec succès');
        this.load();
        if (this.editingProject) this.refreshDetail();
        this.editingProject = null;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.projectFormSubmitting = false;
        this.projectFormError = err?.error?.message ?? 'Erreur lors de l\'enregistrement.';
        this.cdr.detectChanges();
      },
    });
  }

  // ─── Change status ────────────────────────
  toggleStatusDropdown(event: Event): void {
    event.stopPropagation();
    this.showStatusDropdown = !this.showStatusDropdown;
  }

  changeStatus(status: ProjectStatus): void {
    if (!this.selectedProject || this.selectedProject.status === status) {
      this.showStatusDropdown = false;
      return;
    }
    this.managerService.changeProjectStatus(this.selectedProject.id, status).subscribe({
      next: (updated) => {
        this.showStatusDropdown = false;
        if (this.selectedProject) this.selectedProject.status = status;
        // Also update in list
        const idx = this.projects.findIndex(p => p.id === this.selectedProject?.id);
        if (idx > -1) this.projects[idx] = { ...this.projects[idx], status };
        this.showSuccess('Statut mis à jour');
        this.cdr.detectChanges();
      },
      error: () => {
        this.showStatusDropdown = false;
        this.showError('Erreur lors du changement de statut');
        this.cdr.detectChanges();
      },
    });
  }

  // ─── Assign collaborator ────────────────────────
  openAssignForm(): void {
    this.assignForm = { collaboratorId: 0, roleName: '', startDate: '', endDate: '' };
    this.assignFormError = '';
    this.showAssignForm = true;
    this.loadAvailableCollaborators();
  }

  closeAssignForm(): void {
    this.showAssignForm = false;
  }

  loadAvailableCollaborators(): void {
    this.loadingCollaborators = true;
    this.managerService.getTeam({ page: 0, size: 100 }).subscribe({
      next: (res) => {
        this.availableCollaborators = res.items ?? [];
        this.loadingCollaborators = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.availableCollaborators = [];
        this.loadingCollaborators = false;
        this.cdr.detectChanges();
      },
    });
  }

  submitAssignment(): void {
    if (!this.assignForm.collaboratorId || !this.assignForm.roleName || !this.assignForm.startDate) {
      this.assignFormError = 'Le collaborateur, le rôle et la date de début sont requis.';
      return;
    }
    if (!this.selectedProject) return;

    this.assignFormSubmitting = true;
    this.assignFormError = '';

    this.managerService.assignCollaborator(this.selectedProject.id, this.assignForm).subscribe({
      next: () => {
        this.assignFormSubmitting = false;
        this.showAssignForm = false;
        this.showSuccess('Collaborateur assigné avec succès');
        this.refreshDetail();
        this.load();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.assignFormSubmitting = false;
        this.assignFormError = err?.error?.message ?? 'Erreur lors de l\'assignation.';
        this.cdr.detectChanges();
      },
    });
  }

  // ─── Unassign ────────────────────────
  unassign(assignmentId: number): void {
    if (!confirm('Voulez-vous retirer ce collaborateur du projet ?')) return;
    this.unassigningId = assignmentId;
    this.managerService.unassign(assignmentId).subscribe({
      next: () => {
        this.unassigningId = null;
        this.showSuccess('Collaborateur retiré du projet');
        this.refreshDetail();
        this.load();
        this.cdr.detectChanges();
      },
      error: () => {
        this.unassigningId = null;
        this.showError('Erreur lors du retrait');
        this.cdr.detectChanges();
      },
    });
  }

  // ─── Helpers ────────────────────────
  get totalPages(): number {
    return Math.ceil(this.total / this.size);
  }

  prevPage(): void {
    if (this.page > 0) { this.page--; this.load(); }
  }

  nextPage(): void {
    if (this.page + 1 < this.totalPages) { this.page++; this.load(); }
  }

  getStatusLabel(status: string): string {
    const map: Record<string, string> = {
      ACTIVE: 'En cours',
      COMPLETED: 'Terminé',
      ON_HOLD: 'En pause',
      PLANNED: 'Planifié',
      ARCHIVED: 'Archivé',
    };
    return map[status] || status;
  }

  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      ACTIVE: 'status-active',
      COMPLETED: 'status-done',
      ON_HOLD: 'status-hold',
      PLANNED: 'status-planned',
      ARCHIVED: 'status-archived',
    };
    return map[status] || '';
  }

  deleteProject(): void {
    if (!this.selectedProject) return;
    const msg = 'Êtes-vous sûr de vouloir supprimer ce projet ? \nLe projet sera archivé et toutes les affectations actives seront clôturées. Cette action ne peut pas être annulée facilement.';
    if (!confirm(msg)) return;

    this.deletingProjectId = this.selectedProject.id;
    this.managerService.deleteProject(this.selectedProject.id).subscribe({
      next: () => {
        this.deletingProjectId = null;
        this.closeDetail();
        this.showSuccess('Projet archivé avec succès');
        this.load();
        this.cdr.detectChanges();
      },
      error: () => {
        this.deletingProjectId = null;
        this.showError('Erreur lors de l\'archivage du projet');
        this.cdr.detectChanges();
      },
    });
  }

  get activeCount(): number {
    return this.projects.filter(p => p.status === 'ACTIVE').length;
  }

  get completedCount(): number {
    return this.projects.filter(p => p.status === 'COMPLETED').length;
  }

  getMemberInitials(m: { firstName: string; lastName: string }): string {
    return `${m.firstName.charAt(0)}${m.lastName.charAt(0)}`.toUpperCase();
  }

  private emptyProjectForm(): ProjectCreateDto {
    return { name: '', clientName: '', startDate: '', endDate: '', status: 'PLANNED', technologies: '', description: '', neededRessource: '' };
  }

  private showSuccess(msg: string): void {
    this.successMessage = msg;
    setTimeout(() => { this.successMessage = ''; this.cdr.detectChanges(); }, 3000);
  }

  private showError(msg: string): void {
    this.errorMessage = msg;
    setTimeout(() => { this.errorMessage = ''; this.cdr.detectChanges(); }, 4000);
  }

  ngOnDestroy(): void {
    if (this.searchTimeout) clearTimeout(this.searchTimeout);
  }
}
