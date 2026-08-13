import { ticketServiceClient } from "./client";
import type { Notification } from "@/types/ticket";

export const notificationApi = {
  getAll: async (): Promise<Notification[]> => {
    const { data } = await ticketServiceClient.get<Notification[]>("/api/notifications");
    return data;
  },

  markAsRead: async (id: string): Promise<Notification> => {
    const { data } = await ticketServiceClient.put<Notification>(`/api/notifications/${id}/read`);
    return data;
  },
};
