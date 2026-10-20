export interface LoginCredentials {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  projectId: string;
  user?: {
    id: number;
    username: string;
    name: string;
  };
}

export interface FileNode {
  name: string;
  path: string;
  type: "file" | "directory";
  children?: FileNode[];
}

export interface DeployResponse {
  previewUrl: string;
}

export interface ChatHistoryMessage {
  id: number;
  role: "USER" | "ASSISTANT";
  content: string;
  createdAt: string;
}

export enum ChatEventType {
  THOUGHT = 'THOUGHT',
  MESSAGE = 'MESSAGE',
  FILE_EDIT = 'FILE_EDIT',
  TOOL_LOG = 'TOOL_LOG'
}

export interface ChatEvent {
  id?: number;
  type: ChatEventType;
  content: string; // Markdown, Code, or Tool Summary
  metadata?: string; // Tool args (e.g. "src/App.tsx")
  filePath?: string; // For FILE_EDIT
  sequenceOrder?: number;
}

export interface ChatMessage {
  id: number;
  role: 'USER' | 'ASSISTANT';
  content?: string; // Fallback raw text
  events: ChatEvent[]; // The granular events
  createdAt?: string;
}

export interface ProjectSummaryResponse {
  projectId: number;
  name: string;
  description?: string;
  thumbnailUrl?: string;
  role?: ProjectRole;
  createdAt: string;
}

export interface ProjectResponse {
  id: number;
  name: string;
  role?: ProjectRole; // Added to check user's permission in the project
  createdAt: string;
  updatedAt?: string;
}

export interface ProjectRequest {
  name: string;
}

export type ProjectRole = 'OWNER' | 'EDITOR' | 'VIEWER';

export interface ProjectMember {
  userId: number;
  username?: string | null;
  name?: string | null;
  role: ProjectRole;
  invitedAt?: string;
}

export interface InviteMemberRequest {
  username: string;
  role: ProjectRole;
}

export interface SignupRequest {
  username: string;
  name: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  user: {
    id: number;
    username: string;
    name: string;
    plan?: unknown;
  };
}
