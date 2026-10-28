# IdeaStitch Industrial Redesign — Batch 1

This is the first implementation batch based on the supplied source archives. The working source is a frontend-only redesign; backend source has not been modified.

## Architecture map from the supplied source

### Frontend

- **Runtime/build:** React 18, Vite 5, TypeScript, Tailwind CSS **3.4**, React Router 6, `lucide-react`, CodeMirror, shadcn/Radix UI components, and React Query provider.
- **Important dependency correction:** despite the design brief mentioning Tailwind CSS 4 and daisyUI 5, the supplied `package.json` actually has Tailwind CSS 3.4.17 and no daisyUI package. This batch preserves the existing stack rather than upgrading core dependencies during the visual redesign.
- **Routing (`src/App.tsx`):** `/` public homepage; `/login`; `/signup`; `/projects`; `/projects/:projectId`; wildcard not-found. Before this batch, `/` was a redirect to projects or login, not a public marketing homepage.
- **API client (`src/lib/api.ts`):** authentication, project list/create/update/delete, file tree/content reads, deploy/preview, ZIP download, project-member/share operations, chat history, and SSE chat streaming. Base URL in the source is `https://api.ideastitch.in`.
- **Project/dashboard:** `src/pages/ProjectsDashboard.tsx`; project operations use existing API wrappers and local React state.
- **Builder/workspace:** `src/pages/ProjectView.tsx` coordinates chat, file tree/code display, preview, share and export. Supporting components include `ChatPanel`, `ChatEventRenderer`, `CodePanel`, `FileTree`, `FileTabs`, `CodeEditor`, `PreviewPanel`, `ShareDialog`, and runtime-error handling.
- **State:** auth token and user info are in local storage; preview URL and editor-tab state use local storage; chat/project interaction is mainly local React state. React Query is provided globally but is not the central state store for most current screens.
- **Tests:** the supplied frontend contains a trivial Vitest example. This batch adds isolated tests for the prompt-name fallback.

### Existing prompt → project → generation flow

1. `ProjectView` sends chat text through `api.streamChat(projectId, message, ...)`.
2. `api.streamChat` POSTs `{ message, projectId }` to `/api/v1/intelligence/chat/stream`, reads SSE `data:` lines, parses `text`, and streams chunks to the UI.
3. The intelligence service's `ChatController` exposes `POST /chat/stream`; it calls `AiGenerationService.streamResponse(...)`.
4. The generation service uses Spring AI `ChatClient`, its code-generation system prompt, `CodeGenerationTools` and `FileTreeContextAdvisor`. Completion saves chat history, parses file-related events, and sends `FileStoreRequestEvent` messages to Kafka.
5. The workspace service's file-storage consumer processes the event, saves generated files in MinIO, and records metadata in Postgres.

The browser-facing prefix and gateway routing depend on the current deployed gateway/configuration. The source archive does not include all remote gateway routes/configuration, so no new routes are inferred in this batch.

### Backend service map

- **Account service (`acc`):** signup/login, account/profile-related endpoints, subscription/checkout/portal controller and payment-webhook handling.
- **Workspace service (`ws`):** project lifecycle, owner/member roles, file tree/content reads, ZIP export, preview deploy, and initial template copy to MinIO.
- **Intelligence service (`intell`):** chat history, SSE generation, model/tool/advisor integration, usage logging, and generated-file/event handoff.
- **Gateway (`gtw`):** API gateway and JWT/CORS infrastructure; deployed route configuration is not fully present in the source archive.
- **Config service (`config`):** configuration service code; remote application configuration is not included in the supplied source.
- **Common library (`common`):** shared DTOs, enums, events, security helpers and errors.
- **Infrastructure:** GKE/Kubernetes manifests are included in the archive.

**Backend build limitation:** no Maven `pom.xml`, Maven wrapper, Gradle build file, or Gradle wrapper was present in the supplied backend source archive. Backend compilation could not be run from this source bundle.

## What this batch changes

- Adds an editable SVG IdeaStitch mark, horizontal lockup, monochrome mark, and refreshed PNG favicon.
- Adds an app-wide light/dark `ThemeProvider` and accessible theme toggle. The initial theme uses a saved choice when available and otherwise follows the OS preference; the selected mode persists in local storage.
- Replaces the old root redirect with a real public homepage using the Industrial visual language: cool-grey chassis, raised controls, inset dark display areas, restrained orange highlights, clear typography, and responsive sections. The financial-dashboard preview is labelled as schematic rather than represented as a live user project.
- Redesigns the login and signup layouts around the same identity. Password visibility uses open-eye/closed-eye states, and the toggle attempts to preserve focus/caret position. Signup shows a live eight-character client-side requirement.
- Applies shared Industrial tokens and responsive/focus/reduced-motion styling in `src/index.css`; configures the existing Tailwind stack to use Host Grotesk and IBM Plex Mono with fallbacks.
- Makes CodeMirror follow IdeaStitch’s light/dark theme.
- Adds a frontend-only homepage prompt handoff: the original prompt is retained in session storage across signup/login, a concise deterministic fallback name is used to create a project through the existing API, and the original prompt is sent to the existing SSE generation endpoint after the project/history load.
- Adds focused tests for fallback project naming.

## Prompt handoff: what is and is not implemented

Implemented now: original prompt preservation, deterministic fallback naming, project creation using the existing API, and handoff into the existing generation stream. This avoids losing the prompt while the user authenticates.

Not implemented yet: AI-generated naming, starter-template selection, or a new backend orchestration contract. The supplied workspace service currently copies one hard-coded starter (`react-vite-tailwind-daisyui-starter-main` from the `starter-projects` bucket) on project creation. It has no template-selection field in the current project creation DTO. Naming/template metadata needs a bounded backend design based on that real contract; this batch deliberately does not pretend that a choice of multiple templates already exists.

## Confirmed gaps that need an explicit decision before backend expansion

1. **Username login/signup contract mismatch:** the frontend presents a username field, but `LoginRequest.username` and `SignUpRequest.username` in the account service are both annotated `@Email`. Plain usernames will fail validation. The password DTO constraints are also currently minimum 4 characters, while this batch’s signup UI guides/enforces 8 characters client-side. No auth backend files have been changed. The minimum username compatibility patch is to remove email-format validation from those two DTO fields while retaining required-field checks; password validation policy should be aligned separately with explicit agreement.
2. **File editing/saving is not wired end-to-end:** current `CodeEditor` is `editable={false}`. The frontend has no save-file API wrapper and the public `FileController` provides reads and ZIP download, not a file-update endpoint. `ProjectFileServiceImpl.saveFile()` is used internally by the generated-file consumer; that alone does not establish a browser save contract. A working editor-save feature needs approval for a minimal backend/API addition before changing the UI to imply editing is saved.
3. **Pricing/plan catalogue:** the account source has subscription, checkout and portal endpoints, but no public plan-list endpoint surfaced in the provided controller. The frontend currently has no pricing/subscription routes or corresponding API wrappers. This batch does not invent plan names/prices or payment outcomes. A frontend using real plans needs a source of public plan catalogue data or approval for a small backend endpoint.
4. **Streaming stages:** the inspected contract streams text, with file changes delivered through the current generation/file pipeline. No reliable percentage or explicit phase-progress event contract was found. Loading feedback should remain indeterminate unless real events support a stage.

## Verification performed

- TypeScript/TSX syntax transpile: **84 source files**, no syntax diagnostics.
- CSS parse of `src/index.css`: **227 top-level rules**, zero CSS syntax errors.
- Prompt-title helper: four isolated cases passed (normal prompt title, descriptive prompt title, empty fallback, 48-character cap).
- `npm ci --no-audit --no-fund` could not finish because the environment could not resolve `registry.npmjs.org` (`EAI_AGAIN`).
- Consequently the Vite binary was not installed; `npm run build` failed with `vite: not found`. `tsc --noEmit` also could not run successfully because the incomplete dependency installation lacks `vitest/globals`. These are **environment/dependency-install blockers**, not a passing build. No browser render at desktop/mobile widths was possible in this environment, so visual verification is still required after dependencies can be installed.

## Local verification commands

From the frontend directory, after network access is available:

```bash
npm ci
npm run build
npm test
npm run lint
```

## Manual regression checklist

1. Open `/` at desktop width (~1440 px), tablet (~768 px), and mobile (~390 px); check the content order, prompt area, no horizontal overflow, and visible keyboard focus.
2. Switch light/dark, reload, navigate to `/login`, `/signup`, `/projects`, and a project; verify the selected theme persists.
3. Submit a homepage prompt while signed out; complete signup/login; verify the project is created and the **original** prompt is sent exactly once to the existing generator.
4. Open `/login` and `/signup`; check the distinct eye/eye-off icons, caret/focus preservation, password length indicator, loading states and backend errors. Plain non-email usernames cannot pass until the auth DTO mismatch is resolved.
5. Exercise existing project list, rename/delete, share/member actions, preview deploy and ZIP download, checking real server errors and download behavior.
6. Confirm reduced-motion preferences do not trigger distracting animations.

## Next phase sequence

1. Finish the dashboard and builder visual adaptation while preserving current API contracts; add real empty/loading/error states.
2. Before promising file save, confirm whether a save endpoint or supported event/API exists; request approval if not.
3. Build pricing/subscription screens only around real plan/catalogue data and existing checkout/portal capabilities.
4. Design the bounded prompt → title/template metadata backend flow: there is one current starter, so first define the supported template catalogue and fallback. Compare deterministic classification vs. one structured model call (latency/cost/retries); preserve original prompt and current generation pipeline.
5. Request authorization for the minimal username-validation change, then build/test it once a complete backend build setup is available.
