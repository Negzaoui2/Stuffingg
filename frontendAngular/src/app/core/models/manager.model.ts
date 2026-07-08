/* ===== Manager-specific models ===== */

export type AvailabilityStatus = 'STAFFED' | 'AVAILABLE' | 'SOON_AVAILABLE';
export type ProjectStatus = 'ACTIVE' | 'COMPLETED' | 'ON_HOLD' | 'PLANNED' | 'ARCHIVED';

/** Collaborateur dans l'équipe du manager */
export interface Collaborator {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  department?: string;
  skills: string[];
  availability: AvailabilityStatus;
  currentProject?: string;
  availableFrom?: string;
  assignments: CollaboratorAssignment[];
}

export interface CollaboratorAssignment {
  id: number;
  projectName: string;
  clientName?: string;
  roleName?: string;
  startDate: string;
  endDate?: string;
  status: 'ACTIVE' | 'COMPLETED' | 'ON_HOLD';
}

/** Projet / Mission géré par le manager */
export interface Project {
  id: number;
  name: string;
  clientName: string;
  description?: string;
  status: ProjectStatus;
  startDate: string;
  endDate?: string;
  teamSize: number;
  team: ProjectMember[];
  technologies?: string[];
  neededRessource?: string;
}

export interface ProjectMember {
  id: number;
  assignmentId?: number;
  firstName: string;
  lastName: string;
  role: string;
  skills: string[];
  startDate?: string;
  endDate?: string;
}

/** DTO pour créer/modifier un projet */
export interface ProjectCreateDto {
  name: string;
  clientName: string;
  startDate: string;
  endDate?: string;
  status: ProjectStatus;
  technologies?: string;
  description?: string;
  neededRessource?: string;
}

/** DTO pour assigner un collaborateur */
export interface AssignmentCreateDto {
  collaboratorId: number;
  roleName: string;
  startDate: string;
  endDate?: string;
}

/** KPI du tableau de bord manager */
export interface ManagerDashboard {
  totalCollaborators: number;
  activeProjects: number;
  occupancyRate: number;
  availableCollaborators: number;
  soonAvailableCollaborators: number;
  skillDistribution: { name: string; count: number }[];
  projectStatusDistribution: { status: ProjectStatus; count: number }[];
  recentAssignments: RecentAssignment[];
}

export interface RecentAssignment {
  collaboratorName: string;
  projectName: string;
  date: string;
  type: 'ASSIGNED' | 'COMPLETED' | 'ENDING_SOON';
}

/** Réponse paginée pour les listes */
export interface PageResponse<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
  totalPages?: number;
}

/** Demande de congé vue par le manager */
export type LeaveType = 'PAID_LEAVE' | 'RTT' | 'SICK_LEAVE' | 'UNPAID_LEAVE';
export type LeaveStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface LeaveRequestManager {
  id: number;
  collaboratorName: string;
  collaboratorEmail: string;
  type: LeaveType;
  startDate: string;
  endDate: string;
  days: number;
  reason: string;
  status: LeaveStatus;
  createdAt: string;
}
