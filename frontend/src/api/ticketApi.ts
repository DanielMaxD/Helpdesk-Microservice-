import { ticketServiceClient } from "./client";
import type {
  AssignRequest,
  Comment,
  CommentRequest,
  StatusUpdateRequest,
  Ticket,
  TicketCreateRequest,
  TicketStatistics,
  TicketUpdateRequest,
} from "@/types/ticket";

export const ticketApi = {
  create: async (request: TicketCreateRequest): Promise<Ticket> => {
    const { data } = await ticketServiceClient.post<Ticket>("/api/tickets", request);
    return data;
  },

  getById: async (id: string): Promise<Ticket> => {
    const { data } = await ticketServiceClient.get<Ticket>(`/api/tickets/${id}`);
    return data;
  },

  getMy: async (): Promise<Ticket[]> => {
    const { data } = await ticketServiceClient.get<Ticket[]>("/api/tickets/my");
    return data;
  },

  getAssigned: async (): Promise<Ticket[]> => {
    const { data } = await ticketServiceClient.get<Ticket[]>("/api/tickets/assigned");
    return data;
  },

  getAll: async (): Promise<Ticket[]> => {
    const { data } = await ticketServiceClient.get<Ticket[]>("/api/tickets");
    return data;
  },

  update: async (id: string, request: TicketUpdateRequest): Promise<Ticket> => {
    const { data } = await ticketServiceClient.put<Ticket>(`/api/tickets/${id}`, request);
    return data;
  },

  updateStatus: async (id: string, request: StatusUpdateRequest): Promise<Ticket> => {
    const { data } = await ticketServiceClient.put<Ticket>(`/api/tickets/${id}/status`, request);
    return data;
  },

  assign: async (id: string, request: AssignRequest): Promise<Ticket> => {
    const { data } = await ticketServiceClient.put<Ticket>(`/api/tickets/${id}/assign`, request);
    return data;
  },

  remove: async (id: string): Promise<void> => {
    await ticketServiceClient.delete(`/api/tickets/${id}`);
  },

  addComment: async (id: string, request: CommentRequest): Promise<Comment> => {
    const { data } = await ticketServiceClient.post<Comment>(`/api/tickets/${id}/comments`, request);
    return data;
  },

  getComments: async (id: string): Promise<Comment[]> => {
    const { data } = await ticketServiceClient.get<Comment[]>(`/api/tickets/${id}/comments`);
    return data;
  },

  getStatistics: async (): Promise<TicketStatistics> => {
    const { data } = await ticketServiceClient.get<TicketStatistics>("/api/tickets/statistics");
    return data;
  },
};
