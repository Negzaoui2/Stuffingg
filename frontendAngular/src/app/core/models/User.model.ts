export interface Assignment {
  id: number;
  projectName: string;
  clientName?: string;
  roleName?: string;
  startDate: string;
  endDate?: string;
  status: 'ACTIVE' | 'COMPLETED' | 'ON_HOLD';
}

export interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  isActive: boolean;
  createdAt?: string;
  lastLogin?: string;
  phone?: string;
  department?: string;
  skills?: string[];
  assignments?: Assignment[];
}
