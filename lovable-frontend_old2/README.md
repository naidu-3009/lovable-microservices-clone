# IdeaStitch — Industrial Frontend

IdeaStitch is an AI application builder interface. This repository contains the React/Vite frontend, including the public homepage, authentication screens, project dashboard, AI builder workspace, and account billing status screens.

## Stack

- React 18 and TypeScript
- Vite 5
- React Router 6
- Tailwind CSS 3.4 and the existing shadcn/Radix UI components
- lucide-react icons
- CodeMirror for source inspection
- Existing IdeaStitch API at `https://api.ideastitch.in` (configured in `src/lib/api.ts`)

The source uses Tailwind CSS 3.4. It is intentionally not upgraded to Tailwind CSS 4 or daisyUI as part of this redesign.

## Run locally

Requirements: Node.js compatible with the installed Vite version and npm.

```bash
npm ci
npm run dev
```

Vite prints the local development URL after startup.

## Build, tests and lint

```bash
npm run build
npm test
npm run lint
```

`npm run build` produces the static production bundle in `dist/`. The application is a single-page app; configure the static host to serve `index.html` for application routes such as `/projects`, `/billing`, `/success.html`, and `/cancel.html`.

## Main routes

| Route | Purpose |
|---|---|
| `/` | Public marketing homepage and prompt entry |
| `/login` | Sign in |
| `/signup` | Account registration |
| `/projects` | Project dashboard |
| `/projects/:projectId` | Builder workspace with chat, source and preview |
| `/billing` | Current subscription status and billing portal |
| `/success.html` | Checkout return; verifies status with the account API |
| `/cancel.html` | Checkout cancellation return |

## Design and behavior notes

- The Industrial design system uses cool-grey chassis surfaces, raised panels, recessed work areas, the orange accent, Host Grotesk/IBM Plex Mono typography and a complementary dark palette.
- The theme toggle persists the selected theme in local storage. The first visit follows the operating-system preference.
- The marketing footer uses four equally spaced columns on desktop and a two-column layout on narrow screens.
- The homepage stores the original prompt in session storage so it can survive the signup/login route. After authentication, the current frontend creates a project using a deterministic fallback title and passes the original prompt to the existing generation stream.
- The current workspace backend copies one fixed starter template. AI-driven project naming and selectable starter templates are not implemented in this frontend-only batch.
- Billing shows only the subscription fields returned by the account API. The supplied backend source does not expose a public plan catalogue, so the UI does not invent plan tiers or prices.
- The provided account service has `@Email` validation on its username fields. Plain username login/signup therefore requires an approved backend validation change; this frontend batch does not change backend code.
- Source inspection is currently read-only in the supplied frontend. A browser file-save workflow is not added because no matching public save-file API contract was present in the supplied code.

## API configuration

The API base URL is currently a constant in `src/lib/api.ts`. If you need to run against a different backend, update that value intentionally and ensure the matching gateway routes and CORS settings are configured.

## Deployment

Build the frontend and deploy the generated `dist/` contents to the existing static frontend hosting setup. Keep SPA history fallback enabled for the routes listed above. Do not publish backend credentials in frontend code; the frontend should only call authenticated public API routes and rely on the backend to enforce authorization.
