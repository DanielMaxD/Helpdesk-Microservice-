import { userServiceClient } from "./client";
import type { AuthResponse, LoginRequest, RegisterRequest } from "@/types/auth";

export const authApi = {
  login: async (request: LoginRequest): Promise<AuthResponse> => {
    const { data } = await userServiceClient.post<AuthResponse>("/api/auth/login", request);
    return data;
  },

  register: async (request: RegisterRequest): Promise<AuthResponse> => {
    const { data } = await userServiceClient.post<AuthResponse>("/api/auth/register", request);
    return data;
  },
};
