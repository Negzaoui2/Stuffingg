/* ===== Admin Report Models ===== */

export interface LeaveTypeStatDto {
  type: string;
  label: string;
  count: number;
  totalDays: number;
}

export interface MonthlyDistributionDto {
  month: string;
  totalDays: number;
  requestCount: number;
}

export interface LeaveStatsResponse {
  totalRequests: number;
  pending: number;
  approved: number;
  rejected: number;
  byType: LeaveTypeStatDto[];
  absenteeismRate: number;
  monthlyDistribution: MonthlyDistributionDto[];
}

export interface LeaveDetailAdminDto {
  id: number;
  collaboratorId: number;
  collaboratorName: string;
  collaboratorEmail: string;
  department: string;
  type: string;
  startDate: string;
  endDate: string;
  days: number;
  reason: string;
  status: string;
  createdAt: string;
  reviewedBy: string;
  reviewedAt: string;
}

export interface DepartmentAbsenteeismDto {
  department: string;
  rate: number;
  totalDaysAbsent: number;
  collaboratorCount: number;
}

export interface AbsenteeismResponse {
  globalRate: number;
  period: string;
  byDepartment: DepartmentAbsenteeismDto[];
}

export interface PageResponse<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}
