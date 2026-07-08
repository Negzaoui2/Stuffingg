import { CommonModule } from '@angular/common';
import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AccountRequest, AccountRequestStatus, ManagerSummary, ApproveRequestDto } from '../../../core/models/account-request.model';
import { AccountRequestStats, AccountRequestsService } from '../../../core/services/account-requests';
import { Departement } from '../../../core/models/departement.model';
import { DepartementService } from '../../../core/services/departement.service';
import { NotificationBellComponent } from '../../../shared/notification-bell/notification-bell.component';

@Component({
  selector: 'app-account-requests-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, NotificationBellComponent],
  templateUrl: './account-requests-dashboard.component.html',
  styleUrls: ['./account-requests-dashboard.component.css'],
})
export class AccountRequestsDashboardComponent implements OnInit {
  isLoading = true;
  errorMessage = '';
  stats: AccountRequestStats = {
    total: 0,
    pending: 0,
    approved: 0,
    rejected: 0,
  };

  // Données affichées (page actuelle si pagination)
  requests: AccountRequest[] = [];

  // Filtre courant (null => toutes)
  filterStatus: AccountRequestStatus | null = null;

  // (Optionnel) pagination si tu veux l’utiliser ensuite
  page = 0;
  size = 10;
  total = 0;
  sort = 'createdAt,DESC';

  constructor(
    private accountRequestsService: AccountRequestsService,
    private departementService: DepartementService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadStats();
    this.load();
  }

  loadStats(): void {
    this.accountRequestsService.getStats().subscribe({
      next: (stats) => {
        this.stats = stats;
        this.cdr.detectChanges();
      },
      error: (err: unknown) => {
        console.error('Erreur chargement statistiques demandes:', err);
      },
    });
  }

  load(): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.accountRequestsService
      .getAll({
        // NB: on ne filtre pas côté API car le backend peut ne pas supporter
        // le paramètre ?status=... (sinon la liste devient vide au clic).
        page: this.page,
        size: this.size,
        sort: this.sort,
      })
      .subscribe({
        next: (res: { items: AccountRequest[]; total: number; page: number; size: number }) => {
          this.requests = res.items ?? [];
          this.total = res.total ?? 0;
          this.page = res.page ?? this.page;
          this.size = res.size ?? this.size;
          this.isLoading = false;
          this.cdr.detectChanges();
        },
        error: (err: unknown) => {
          console.error('Erreur chargement demandes:', err);
          this.errorMessage = 'Impossible de charger les demandes.';
          this.isLoading = false;
          this.cdr.detectChanges();
        },
      });
  }

  // ✅ Compteurs (sur la liste affichée)
  get pendingCount(): number {
    return this.requests.filter(r => r.status === AccountRequestStatus.PENDING).length;
  }
  get approvedCount(): number {
    return this.requests.filter(r => r.status === AccountRequestStatus.APPROVED).length;
  }
  get rejectedCount(): number {
    return this.requests.filter(r => r.status === AccountRequestStatus.REJECTED).length;
  }

  // ✅ Pour compat HTML existant
  get filteredRequests(): AccountRequest[] {
    if (this.filterStatus == null) return this.requests;
    return this.requests.filter((r) => r.status === this.filterStatus);
  }

  // ✅ Filtre côté back
  filterByStatus(status: string | null): void {
    if (status === null) {
      this.filterStatus = null;
    } else if (status === 'PENDING' || status === 'APPROVED' || status === 'REJECTED') {
      this.filterStatus = status as AccountRequestStatus;
    } else {
      this.filterStatus = null;
    }
    // Filtrage côté front: pas de rechargement nécessaire.
  }

  // Libellé
  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      PENDING: 'En attente',
      APPROVED: 'Approuvée',
      REJECTED: 'Rejetée',
    };
    return labels[status] || status;
  }

  // --- Actions

  // --- Approve Modal State
  showApproveModal = false;
  approveTarget: AccountRequest | null = null;
  selectedRole = 'COLLABORATEUR';
  selectedManagerId: number | null = null;
  managers: ManagerSummary[] = [];
  managersLoading = false;

  // Départements
  selectedDepartementId: number | null = null;
  departements: Departement[] = [];
  departementsLoading = false;

  openApproveModal(request: AccountRequest): void {
    this.approveTarget = request;
    this.selectedRole = 'COLLABORATEUR';
    this.selectedManagerId = null;
    this.selectedDepartementId = null;
    this.showApproveModal = true;
    this.loadManagers();
    this.loadDepartements();
  }

  closeApproveModal(): void {
    this.showApproveModal = false;
    this.approveTarget = null;
    this.selectedRole = 'COLLABORATEUR';
    this.selectedManagerId = null;
    this.selectedDepartementId = null;
  }

  onRoleChange(): void {
    // Le manager n'est pertinent que pour un collaborateur
    if (this.selectedRole !== 'COLLABORATEUR') {
      this.selectedManagerId = null;
    }
    // Le département n'est proposé que pour collaborateur et manager
    if (this.selectedRole !== 'COLLABORATEUR' && this.selectedRole !== 'DELIVERY_MANAGER') {
      this.selectedDepartementId = null;
    }
  }

  private loadManagers(): void {
    this.managersLoading = true;
    this.accountRequestsService.getManagers().subscribe({
      next: (managers) => {
        this.managers = managers;
        this.managersLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.managers = [];
        this.managersLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  private loadDepartements(): void {
    this.departementsLoading = true;
    this.departementService.getAll().subscribe({
      next: (departements) => {
        this.departements = departements ?? [];
        this.departementsLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.departements = [];
        this.departementsLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  submitApproval(): void {
    if (!this.approveTarget) return;

    const data: ApproveRequestDto = {
      role: this.selectedRole,
    };

    // Le manager n'est envoyé que pour un collaborateur
    if (this.selectedRole === 'COLLABORATEUR') {
      data.managerId = this.selectedManagerId ?? null;
    }

    // Le département est envoyé pour collaborateur et manager
    if (this.selectedRole === 'COLLABORATEUR' || this.selectedRole === 'DELIVERY_MANAGER') {
      data.departementId = this.selectedDepartementId ?? null;
    }

    this.approveRequest(this.approveTarget.id, data);
    this.closeApproveModal();
  }

  private approveRequest(id: string | number, data: ApproveRequestDto): void {
    this.isLoading = true;
    this.accountRequestsService.approveRequest(Number(id), data).subscribe({
      next: () => {
        alert('Demande approuvée avec succès');
        this.isLoading = false;
        this.loadStats();
        this.load(); // 🔁 recharge avec filtre courant
      },
      error: (err: unknown) => {
        console.error('Erreur approbation:', err);
        this.isLoading = false;
        const e = err as { error?: { message?: string } };
        alert(e?.error?.message?.trim() || 'Erreur lors de l\'approbation de la demande');
      }
    });
  }

  openRejectModal(request: AccountRequest): void {
    const reason = prompt('Raison du rejet (optionnel):') || undefined;
    this.rejectRequest(request.id, reason);
  }

  private rejectRequest(id: string | number, reason?: string): void {
    this.isLoading = true;
    this.accountRequestsService.rejectRequest(Number(id), { reason }).subscribe({
      next: () => {
        alert('Demande rejetée avec succès');
        this.isLoading = false;
        this.loadStats();
        this.load(); // 🔁 recharge avec filtre courant
      },
      error: (err: unknown) => {
        console.error('Erreur rejet:', err);
        this.isLoading = false;
        alert('Erreur lors du rejet de la demande');
      }
    });
  }
}
