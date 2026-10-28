import { AvailablePlan, ChatMessage, DeployResponse, FileNode, LoginCredentials, LoginResponse, ProjectSummaryResponse, ProjectRequest, ProjectResponse, ProjectMember, ProjectRole, SignupRequest, AuthResponse, CurrentSubscriptionResponse } from "./types";

const DEFAULT_API_URL = "https://api.ideastitch.in";
const BASE_URL = (import.meta.env.VITE_API_URL || DEFAULT_API_URL).trim().replace(/\/+$/, "");

export const getAuthToken = () => localStorage.getItem("auth_token");

export const setAuthToken = (token: string) => localStorage.setItem("auth_token", token);

export const removeAuthToken = () => localStorage.removeItem("auth_token");

/** Returns the JWT expiry in milliseconds. A value of 0 means the token is malformed. */
export function getTokenExpiryMs(token = getAuthToken()): number | null {
  if (!token) return null;
  const parts = token.split(".");
  if (parts.length !== 3 || !parts[1]) return 0;

  try {
    const base64 = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, "=");
    const binary = atob(padded);
    const bytes = Uint8Array.from(binary, (character) => character.charCodeAt(0));
    const payload = JSON.parse(new TextDecoder().decode(bytes)) as { exp?: unknown };
    return typeof payload.exp === "number" && Number.isFinite(payload.exp) ? payload.exp * 1000 : 0;
  } catch {
    return 0;
  }
}

export function clearAuthSession() {
  removeAuthToken();
  removeUserInfo();
}

export const isAuthenticated = () => {
  const token = getAuthToken();
  if (!token) return false;
  const expiry = getTokenExpiryMs(token);
  if (expiry === 0 || (expiry !== null && expiry <= Date.now() + 5_000)) {
    clearAuthSession();
    return false;
  }
  return true;
};

function redirectForExpiredSession() {
  clearAuthSession();
  if (typeof window === "undefined") return;
  const target = "/login?reason=session-expired";
  if (`${window.location.pathname}${window.location.search}` !== target) {
    window.location.replace(target);
  }
}

/** Every authenticated API request shares one expiry/401 handler. */
async function authenticatedFetch(input: RequestInfo | URL, init: RequestInit = {}): Promise<Response> {
  const token = getAuthToken();
  if (token) {
    const expiry = getTokenExpiryMs(token);
    if (expiry === 0 || (expiry !== null && expiry <= Date.now() + 5_000)) {
      redirectForExpiredSession();
      throw new Error("Your session has expired. Sign in again to continue.");
    }
  }

  const headers = new Headers(init.headers);
  if (token && !headers.has("Authorization")) headers.set("Authorization", `Bearer ${token}`);
  const response = await fetch(input, { ...init, headers });
  // This helper is only used by protected account/workspace endpoints. A 401
  // means the stored credentials are absent, expired, or no longer accepted.
  if (response.status === 401) redirectForExpiredSession();
  return response;
};

const getAuthHeaders = (): HeadersInit => {
  const token = getAuthToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
};

// User info storage
export const setUserInfo = (user: { id: number; username: string; name: string }) => {
  localStorage.setItem("user_info", JSON.stringify(user));
};

export const getUserInfo = (): { id: number; username: string; name: string } | null => {
  const userInfo = localStorage.getItem("user_info");
  if (!userInfo) return null;
  try {
    return JSON.parse(userInfo) as { id: number; username: string; name: string };
  } catch {
    localStorage.removeItem("user_info");
    return null;
  }
};

export const removeUserInfo = () => localStorage.removeItem("user_info");

// LocalStorage keys
export const PREVIEW_URL_KEY = "preview_url";
export const OPEN_TABS_KEY = "open_tabs";
export const ACTIVE_TAB_KEY = "active_tab";

// API response format for files endpoint

interface FilesApiResponse {
  files: { path: string }[];
}

function buildFileTree(paths: { path: string }[]): FileNode[] {
  const root: FileNode[] = [];
  const nodeMap = new Map<string, FileNode>();

  const sortedPaths = [...paths].sort((a, b) =>
    a.path.localeCompare(b.path)
  );

  for (const { path } of sortedPaths) {
    const parts = path.split("/");
    let currentPath = "";

    for (let i = 0; i < parts.length; i++) {
      const part = parts[i];
      const parentPath = currentPath;
      currentPath = currentPath
        ? `${currentPath}/${part}`
        : part;

      if (nodeMap.has(currentPath)) continue;

      const isFile = i === parts.length - 1;

      const node: FileNode = {
        name: part,
        path: currentPath,
        type: isFile ? "file" : "directory",
        children: isFile ? undefined : [],
      };

      nodeMap.set(currentPath, node);

      if (parentPath) {
        const parent = nodeMap.get(parentPath);
        if (parent?.children) {
          parent.children.push(node);
        }
      } else {
        root.push(node);
      }
    }
  }

  const sortNodes = (nodes: FileNode[]) => {
    nodes.sort((a, b) => {
      if (a.type === "directory" && b.type === "file") return -1;
      if (a.type === "file" && b.type === "directory") return 1;
      return a.name.localeCompare(b.name);
    });

    nodes.forEach((node) => {
      if (node.children) sortNodes(node.children);
    });
  };

  sortNodes(root);
  return root;
}

export const api = {
  async getAvailablePlans(): Promise<AvailablePlan[]> {
    const response = await fetch(`${BASE_URL}/api/v1/account/api/payment/plans`, {
      headers: { Accept: "application/json" },
    });
    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || `Could not load subscription plans (${response.status}).`);
    }
    const result: unknown = await response.json();
    if (!Array.isArray(result)) throw new Error("The account service returned an invalid plan catalog.");
    return result as AvailablePlan[];
  },

  async createCheckoutSession(planId: number): Promise<{ checkOutUrl: string }> {
    const response = await authenticatedFetch(`${BASE_URL}/api/v1/account/api/payment/checkout`, {
      method: "POST",
      headers: { "Content-Type": "application/json", ...getAuthHeaders() },
      body: JSON.stringify({ planId }),
    });
    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || `Could not start checkout (${response.status}).`);
    }
    const result = await response.json() as { checkOutUrl?: string; checkoutUrl?: string };
    const checkOutUrl = result.checkOutUrl || result.checkoutUrl;
    if (!checkOutUrl) throw new Error("The account service did not return a Stripe Checkout URL.");
    const parsedUrl = new URL(checkOutUrl);
    if (parsedUrl.protocol !== "https:") throw new Error("The checkout URL was not a secure HTTPS URL.");
    return { checkOutUrl: parsedUrl.toString() };
  },

  async getCurrentSubscription(): Promise<CurrentSubscriptionResponse> {
    const response = await authenticatedFetch(`${BASE_URL}/api/v1/account/api/me/subscription`, {
      headers: { ...getAuthHeaders() },
    });
    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || `Failed to fetch subscription (${response.status})`);
    }
    return response.json();
  },

  async openBillingPortal(): Promise<{ portalUrl: string }> {
    const response = await authenticatedFetch(`${BASE_URL}/api/v1/account/api/payment/portal`, {
      method: "POST",
      headers: { ...getAuthHeaders() },
    });
    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || `Could not open billing portal (${response.status})`);
    }
    const result = await response.json() as { portalUrl?: string };
    if (!result.portalUrl) throw new Error("Billing portal URL was not returned by the server.");
    return { portalUrl: result.portalUrl };
  },

  async login(credentials: LoginCredentials): Promise<LoginResponse> {
    const response = await fetch(`${BASE_URL}/api/v1/account/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(credentials),
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || "Login failed");
    }

    return response.json();
  },

  async signup(data: SignupRequest): Promise<AuthResponse> {
    const response = await fetch(`${BASE_URL}/api/v1/account/auth/signup`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || "Signup failed");
    }

    return response.json();
  },

  async getFiles(projectId: string): Promise<FileNode[]> {
    const response = await authenticatedFetch(`${BASE_URL}/api/v1/workspace/projects/${projectId}/files`, {
      headers: { ...getAuthHeaders() },
    });

    if (!response.ok) {
      throw new Error("Failed to fetch files");
    }

    const data: FilesApiResponse = await response.json();
    return buildFileTree(data.files);
  },

  async getFileContent(projectId: string, path: string): Promise<string> {
    const response = await authenticatedFetch(
      `${BASE_URL}/api/v1/workspace/projects/${projectId}/files/content?path=${path}`,
      {
        headers: { ...getAuthHeaders() },
      }
    );

    const data = await response.json();

    if (!response.ok) {
      console.error(`Error fetching file: ${response.status} ${response.statusText}`);
      throw new Error("Failed to fetch file content");
    }

    return data.content;
  },

  async deploy(projectId: string): Promise<DeployResponse> {
    const response = await authenticatedFetch(`${BASE_URL}/api/v1/workspace/projects/${projectId}/deploy`, {
      method: "POST",
      headers: { ...getAuthHeaders() },
    });

    if (!response.ok) {
      throw new Error("Deployment failed");
    }

    return response.json();
  },


  async getProjects(): Promise<ProjectSummaryResponse[]> {
    const response = await authenticatedFetch(`${BASE_URL}/api/v1/workspace/projects`, {
      headers: { ...getAuthHeaders() },
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(`Failed to fetch projects (${response.status}): ${error}`);
    }

    const projects: unknown = await response.json();
    if (!Array.isArray(projects)) {
      throw new Error("Expected the projects API to return an array");
    }

    // The Workspace Service list endpoint returns `projectId`; keep that API
    // contract throughout the frontend. Do not add a second `id` field.
    return projects as ProjectSummaryResponse[];
  },


async createProject(name: string): Promise<ProjectSummaryResponse> {
  // Capture existing IDs so duplicate project names are handled correctly.
  const existingProjects = await api.getProjects();
  const existingIds = new Set(
    existingProjects.map((project) => project.projectId),
  );

  const response = await authenticatedFetch(
    `${BASE_URL}/api/v1/workspace/projects`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...getAuthHeaders(),
      },
      body: JSON.stringify({ name }),
    },
  );

  if (!response.ok) {
    const error = await response.text();
    throw new Error(
      `Failed to create project (${response.status}): ${error}`,
    );
  }

  // The production endpoint has returned an empty successful response.
  await response.text();

  // Refresh the list to obtain the new project's actual projectId.
  const refreshedProjects = await api.getProjects();

  const created = refreshedProjects.find(
    (project) =>
      project.name === name &&
      !existingIds.has(project.projectId),
  );

  if (!created) {
    throw new Error(
      "Project creation succeeded, but the new project could not be identified. Refresh the dashboard.",
    );
  }

  return created;
},


  async getProject(id: string): Promise<ProjectResponse> {
    const response = await authenticatedFetch(`${BASE_URL}/api/v1/workspace/projects/${id}`, {
      headers: { ...getAuthHeaders() },
    });

    if (!response.ok) {
      throw new Error("Failed to fetch project");
    }

    return response.json();
  },

  async updateProject(id: string, name: string): Promise<ProjectResponse> {
    const response = await authenticatedFetch(`${BASE_URL}/api/v1/workspace/projects/${id}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json", ...getAuthHeaders() },
      body: JSON.stringify({ name }),
    });

    if (!response.ok) {
      throw new Error("Failed to update project");
    }

    return response.json();
  },

  async deleteProject(id: string): Promise<void> {
    const response = await authenticatedFetch(`${BASE_URL}/api/v1/workspace/projects/${id}`, {
      method: "DELETE",
      headers: { ...getAuthHeaders() },
    });

    if (!response.ok) {
      throw new Error("Failed to delete project");
    }
  },

  async downloadProjectZip(id: string): Promise<Blob> {
    const response = await authenticatedFetch(`${BASE_URL}/api/v1/workspace/projects/${id}/files/download-zip`, {
      headers: { ...getAuthHeaders() },
    });

    if (!response.ok) {
      throw new Error("Failed to download project");
    }

    return response.blob();
  },

  async getProjectMembers(projectId: string): Promise<ProjectMember[]> {
    const response = await authenticatedFetch(`${BASE_URL}/api/v1/workspace/projects/${projectId}/members`, {
      headers: { ...getAuthHeaders() },
    });

    if (!response.ok) {
      throw new Error("Failed to fetch project members");
    }

    return response.json();
  },

  async inviteMember(projectId: string, username: string, role: ProjectRole): Promise<void> {
    const response = await authenticatedFetch(`${BASE_URL}/api/v1/workspace/projects/${projectId}/members`, {
      method: "POST",
      headers: { "Content-Type": "application/json", ...getAuthHeaders() },
      body: JSON.stringify({ username, role }),
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || "Failed to invite member");
    }
  },

  async updateMemberRole(projectId: string, userId: number, role: ProjectRole): Promise<void> {
    const response = await authenticatedFetch(`${BASE_URL}/api/v1/workspace/projects/${projectId}/members/${userId}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json", ...getAuthHeaders() },
      body: JSON.stringify({ role }),
    });

    if (!response.ok) {
      throw new Error("Failed to update member role");
    }
  },

  async removeMember(projectId: string, userId: number): Promise<void> {
    const response = await authenticatedFetch(`${BASE_URL}/api/v1/workspace/projects/${projectId}/members/${userId}`, {
      method: "DELETE",
      headers: { ...getAuthHeaders() },
    });

    if (!response.ok) {
      throw new Error("Failed to remove member");
    }
  },

  async getChatHistory(projectId: string): Promise<ChatMessage[]> {
    const response = await authenticatedFetch(`${BASE_URL}/api/v1/intelligence/chat/projects/${projectId}`, {
      headers: { ...getAuthHeaders() },
    });

    if (!response.ok) {
      throw new Error("Failed to fetch chat history");
    }

    return response.json();
  },

  async streamChat(
    projectId: string,
    message: string,
    onChunk: (chunk: string) => void,
    onFile: (path: string, content: string) => void,
    onComplete: () => void,
    onError: (error: Error) => void
  ) {
    const controller = new AbortController();

    authenticatedFetch(`${BASE_URL}/api/v1/intelligence/chat/stream`, {
      method: "POST",
      headers: { "Content-Type": "application/json", ...getAuthHeaders() },
      body: JSON.stringify({ message, projectId }),
      signal: controller.signal,
    })
      .then(async (response) => {
        if (!response.ok) throw new Error("Chat stream failed");

        const reader = response.body?.getReader();
        if (!reader) throw new Error("No reader available");

        const decoder = new TextDecoder();

        // Buffers
        let sseBuffer = ""; // To handle split SSE lines
        let fullContentBuffer = ""; // To accumulate clean text for file regex
        const lastProcessedIndex = 0; // Optimization for regex

        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          const chunk = decoder.decode(value, { stream: true });
          sseBuffer += chunk;

          // Process line by line to handle SSE format (data: ...)
          const lines = sseBuffer.split("\n");
          sseBuffer = lines.pop() || "";

          for (const line of lines) {
            const trimmedLine = line.trim();
            if (!trimmedLine || !trimmedLine.startsWith("data:")) continue;

            const dataStr = trimmedLine.slice(5).trim();
            if (!dataStr) continue;

            try {
              // FIX: Parse JSON to get the real text with newlines preserved
              const parsed = JSON.parse(dataStr);
              const content = parsed.text;

              // 1. Send clean text to UI
              onChunk(content);

              // 2. Accumulate for file parsing (Same as before)
              fullContentBuffer += content;
              // ... (rest of regex logic) ...

            } catch (e) {
              console.error("Failed to parse SSE JSON:", e);
            }
          }
        }

        onComplete();
      })
      .catch((error) => {
        if (error.name !== "AbortError") {
          console.error("Stream error:", error);
          onError(error);
        }
      });

    return () => controller.abort();
  }

};
