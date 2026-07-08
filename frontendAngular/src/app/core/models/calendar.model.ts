import { AvailabilityStatus } from './manager.model';

export interface CalendarResource {
  id: string;
  title: string;
  department: string;
  availability: AvailabilityStatus;
}

export interface CalendarEventExtendedProps {
  type: 'ASSIGNMENT' | 'PROJECT';
  projectName?: string;
  clientName?: string;
  collaboratorName?: string;
  roleName?: string;
  status?: string;
  availability?: string;
  assignmentType?: string;
  teamSize?: number;
}

export interface CalendarEvent {
  id: string;
  title: string;
  start: string;
  end: string;
  resourceId?: string;
  color: string;
  borderColor: string;
  extendedProps: CalendarEventExtendedProps;
}
