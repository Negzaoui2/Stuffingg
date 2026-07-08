import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ManagerService } from '../../../core/services/manager.service';
import { ManagerDashboard } from '../../../core/models/manager.model';

@Component({
  selector: 'app-manager-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
})
export class ManagerDashboardComponent implements OnInit {
  dashboard: ManagerDashboard | null = null;
  isLoading = true;
  errorMessage = '';

  constructor(private managerService: ManagerService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
  }

  private static readonly EMPTY_DASHBOARD: ManagerDashboard = {
    totalCollaborators: 0,
    activeProjects: 0,
    occupancyRate: 0,
    availableCollaborators: 0,
    soonAvailableCollaborators: 0,
    skillDistribution: [],
    projectStatusDistribution: [],
    recentAssignments: [],
  };

  load(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.managerService.getDashboard().subscribe({
      next: (data) => {
        this.dashboard = data;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        // API pas encore prête ou pas de données → afficher un dashboard vide
        this.dashboard = { ...ManagerDashboardComponent.EMPTY_DASHBOARD };
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  getAssignmentTypeLabel(type: string): string {
    const map: Record<string, string> = {
      ASSIGNED: 'Affecté',
      COMPLETED: 'Terminé',
      ENDING_SOON: 'Fin proche',
    };
    return map[type] || type;
  }

  getAssignmentTypeClass(type: string): string {
    const map: Record<string, string> = {
      ASSIGNED: 'type-assigned',
      COMPLETED: 'type-completed',
      ENDING_SOON: 'type-ending',
    };
    return map[type] || '';
  }

  getStatusLabel(status: string): string {
    const map: Record<string, string> = {
      ACTIVE: 'En cours',
      COMPLETED: 'Terminé',
      ON_HOLD: 'En pause',
      PLANNED: 'Planifié',
    };
    return map[status] || status;
  }

  /** Calcul du max pour les barres de compétences */
  getMaxSkillCount(): number {
    if (!this.dashboard?.skillDistribution?.length) return 1;
    return Math.max(...this.dashboard.skillDistribution.map(s => s.count));
  }
}
