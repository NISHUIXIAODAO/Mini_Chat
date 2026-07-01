export interface ApiResponse<T = unknown> {
  code: number;
  message?: string;
  msg?: string;
  data?: T;
}

export interface LoginResult {
  token: string;
  userId: number;
  nickName: string;
}

export interface Contact {
  contactId: number;
  nickName: string;
  contactType?: number;
  lastMessage?: string;
  lastTime?: number;
}

export interface ChatMessage {
  messageId?: number;
  sessionId?: string;
  messageType?: number;
  messageContent: string;
  sendUserId?: number | string;
  senderId?: number | string;
  sendUserNickName?: string;
  sendTime?: number | string;
  contactId?: number | string;
  contactType?: number;
  status?: number;
}

const API_PREFIX = "/api";

export function getToken() {
  return localStorage.getItem("token") || "";
}

export function authHeader() {
  const token = getToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export function getErrorMessage(response: ApiResponse, fallback = "请求失败") {
  return response.message || response.msg || fallback;
}

async function parseResponse<T>(response: Response): Promise<ApiResponse<T>> {
  if (response.status === 401) {
    return { code: 401, message: "登录状态已失效，请重新登录" };
  }
  return response.json();
}

export async function apiGet<T>(path: string) {
  const response = await fetch(`${API_PREFIX}${path}`, {
    headers: {
      ...authHeader(),
    },
  });
  return parseResponse<T>(response);
}

export async function apiPost<T>(path: string, body?: unknown) {
  const headers: Record<string, string> = {
    ...authHeader(),
  };
  const options: RequestInit = {
    method: "POST",
    headers,
  };

  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
    options.body = JSON.stringify(body);
  }

  const response = await fetch(`${API_PREFIX}${path}`, options);
  return parseResponse<T>(response);
}
