import { userServiceClient } from "./client";
import type { UpdateUserRequest, User } from "@/types/auth";

export const userApi = {
  getById: async (id: string): Promise<User> => {
    const { data } = await userServiceClient.get<User>(`/api/users/${id}`);
    return data;
  },

  getAll: async (): Promise<User[]> => {
    const { data } = await userServiceClient.get<User[]>("/api/users");
    return data;
  },

  getAgents: async (): Promise<User[]> => {
    const { data } = await userServiceClient.get<User[]>("/api/agents");
    return data;
  },

  update: async (id: string, request: UpdateUserRequest): Promise<User> => {
    const { data } = await userServiceClient.put<User>(`/api/users/${id}`, request);
    return data;
  },

  remove: async (id: string): Promise<void> => {
    await userServiceClient.delete(`/api/users/${id}`);
  },
};
