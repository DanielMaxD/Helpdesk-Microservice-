export type Priority = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export type Status = "OPEN" | "IN_PROGRESS" | "RESOLVED" | "CLOSED";
export type Category = "ACCOUNT" | "PAYMENT" | "TECHNICAL" | "BILLING" | "OTHER";
export type SlaState = "ON_TRACK" | "AT_RISK" | "BREACHED";
export type NotificationType =
  | "TICKET_ASSIGNED"
  | "TICKET_UPDATED"
  | "TICKET_RESOLVED"
  | "SLA_WARNING";

export interface Ticket {
  id: string;
  title: string;
  description: string;
  priority: Priority;
  status: Status;
  category: Category;
  createdBy: string;
  assignedAgent: string | null;
  createdAt: string;
  updatedAt: string;
  resolvedAt: string | null;
  dueAt: string;
  slaState: SlaState;
}

export interface TicketCreateRequest {
  title: string;
  description: string;
  priority: Priority;
  category: Category;
}

export interface TicketUpdateRequest {
  title?: string;
  description?: string;
  priority?: Priority;
  category?: Category;
}

export interface StatusUpdateRequest {
  status: Status;
}

export interface AssignRequest {
  agentId: string;
}

export interface Comment {
  id: string;
  ticketId: string;
  userId: string;
  message: string;
  createdAt: string;
}

export interface CommentRequest {
  message: string;
}

export interface TicketStatistics {
  totalTickets: number;
  openTickets: number;
  inProgressTickets: number;
  resolvedTickets: number;
  closedTickets: number;
  breachedTickets: number;
}

export interface Notification {
  id: string;
  type: NotificationType;
  message: string;
  read: boolean;
  createdAt: string;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
