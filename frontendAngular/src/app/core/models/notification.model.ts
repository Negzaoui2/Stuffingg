export interface Notification {
  id: number;
  message: string;
  type: string; // ACCOUNT_REQUEST, ACCOUNT_APPROVED, ACCOUNT_REJECTED, etc.
  isRead: boolean;
  createdAt: string; // ISO date string
  targetUser?: {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
}
}