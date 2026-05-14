export type RecipientStatus = 'PENDING' | 'APPROVED' | 'SENDING' | 'SENT' | 'FAILED' | 'DELETED' | 'SCHEDULED';

export interface AuthResponse {
  token: string;
  userId: number;
  name: string;
  email: string;
}

export interface Campaign {
  id: number;
  campaignName: string;
  subjectTemplate: string;
  bodyTemplate: string;
  status: string;
  createdAt: string;
  counts: Record<RecipientStatus, number>;
}

export interface Recipient {
  id: number;
  name: string;
  email: string;
  company: string;
  role: string;
  customDataJson: Record<string, string>;
  status: RecipientStatus;
  renderedSubject: string;
  renderedBody: string;
  errorMessage?: string;
  sentAt?: string;
}

export interface Attachment {
  id: number;
  fileName: string;
  fileUrl: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface DashboardStats {
  campaigns: number;
  recipients: number;
  sent: number;
  failed: number;
  pending: number;
  deleted: number;
  successRate: number;
}

export interface GmailSettings {
  connected: boolean;
  smtpUsername: string;
  maskedAppPassword: string;
}
