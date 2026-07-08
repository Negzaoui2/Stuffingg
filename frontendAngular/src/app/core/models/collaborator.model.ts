/* ===== Collaborator-specific models ===== */

export type LeaveType = 'PAID_LEAVE' | 'RTT' | 'SICK_LEAVE' | 'UNPAID_LEAVE';
export type LeaveStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export type SkillLevel = 'BEGINNER' | 'INTERMEDIATE' | 'EXPERT';
export type AssignmentStatus = 'ACTIVE' | 'COMPLETED' | 'ON_HOLD' | 'PLANNED';

/** Current assignment summary for dashboard */
export interface CurrentAssignment {
  projectName: string;
  clientName: string;
  roleName: string;
  startDate: string;
  endDate: string;
  daysRemaining: number;
  progressPercent: number;
}

/** Leave balance */
export interface LeaveBalance {
  paidLeaveRemaining: number;
  paidLeaveTotal: number;
  paidLeaveUsed: number;
  rttRemaining: number;
  rttTotal: number;
  rttUsed: number;
  sickLeaveTaken: number;
}

/** Dashboard event */
export interface UpcomingEvent {
  type: string;
  label: string;
  date: string;
}

/** Dashboard notification */
export interface DashboardNotification {
  message: string;
  date: string;
  type: 'SUCCESS' | 'INFO' | 'WARNING' | 'ERROR';
}

/** Collaborator dashboard */
export interface CollaboratorDashboard {
  fullName: string;
  department: string;
  currentAssignment: CurrentAssignment | null;
  leaveBalance: LeaveBalance;
  skillCount: number;
  upcomingEvents: UpcomingEvent[];
  recentNotifications: DashboardNotification[];
}

/** Assignment in list */
export interface CollaboratorAssignment {
  id: number;
  projectName: string;
  clientName: string;
  roleName: string;
  startDate: string;
  endDate: string;
  status: AssignmentStatus;
  technologies: string[];
}

/** Assignment detail  */
export interface AssignmentDetail extends CollaboratorAssignment {
  projectDescription?: string;
  team: { firstName: string; lastName: string; role: string }[];
}

/** Calendar event */
export interface CollaboratorCalendarEvent {
  id: string;
  title: string;
  start: string;
  end: string;
  color: string;
  extendedProps: {
    type: 'ASSIGNMENT' | 'LEAVE';
    projectName?: string;
    clientName?: string;
    roleName?: string;
    status?: string;
    leaveType?: LeaveType;
    reason?: string;
  };
}

/** Leave request */
export interface LeaveRequest {
  id: number;
  type: LeaveType;
  startDate: string;
  endDate: string;
  days: number;
  reason?: string;
  status: LeaveStatus;
  reviewedBy?: string;
  createdAt: string;
}

/** Leave creation payload */
export interface LeaveCreateRequest {
  type: LeaveType;
  startDate: string;
  endDate: string;
  reason?: string;
}

/** Skill */
export interface SkillDto {
  id?: number;
  name: string;
  level: SkillLevel;
}

/** Collaborator profile */
export interface CollaboratorProfile {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  department: string;
  role: string;
  skills: SkillDto[];
  joinedAt: string;
}

/** Paginated response */
export interface PageResponse<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
  totalPages?: number;
}
