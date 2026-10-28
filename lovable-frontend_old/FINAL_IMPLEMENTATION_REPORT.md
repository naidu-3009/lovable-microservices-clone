# IdeaStitch Industrial Frontend — Final Source Batch

## Implementation summary

This package contains the complete frontend source tree with the Industrial redesign refinements applied. It preserves the supplied React/Vite/Tailwind 3.4 stack and existing backend API contracts.

### Visual and UX refinements

- Replaced the old product identity with the editable IdeaStitch SVG mark and wordmark, plus PNG app icon.
- Added a persistent light/dark theme provider that starts from the saved preference or operating-system preference. Both the initial document script and the theme provider set a matching browser theme-color.
- Improved technical-label and preview metadata sizes/contrast; set reading text to more comfortable sizes.
- Refined the landing hero and prompt composer so the primary action appears directly alongside prompt input. Added form validation, examples, focus treatment and a lower CTA that returns focus to the prompt.
- Made the homepage footer four evenly spaced desktop columns and a deliberate two-column layout on narrow screens. Footer labels, links and the bottom row are more legible.
- Added a small decorative code/preview schematic to the desktop authentication panel to use the space intentionally.
- Added industrial styling for project dashboard controls/cards, the builder's chat/code/preview panels, billing status and checkout return screens.
- Added project dashboard sort control and a distinct loading-failure state with retry rather than showing an empty result when the API fails.
- Improved visible password controls with different eye/closed-eye glyphs and retained focus/caret when toggled.
- Added a theme-aware, restrained project thumbnail background instead of a random colorful gradient.

### Working frontend flows added or improved

- Public `/` homepage with prompt entry and examples.
- Prompt persistence through signup/login using `sessionStorage`; after authentication the dashboard creates the initial project with a deterministic fallback name and hands the original prompt to the existing builder/SSE stream.
- `/billing` shows only current subscription details returned by the current account API, retries load errors, and opens the existing billing portal endpoint.
- `/success.html` verifies the current account subscription before showing the plan as active; `/cancel.html` avoids claiming a successful payment.
- Existing dashboard project operations, builder chat streaming, preview, file browsing, share and ZIP download remain routed through the existing API code.

## Architecture map

| Screen / capability | Source |
|---|---|
| Application routes | `src/App.tsx` |
| Theme + first-render preference | `src/components/ThemeProvider.tsx`, `src/components/ThemeToggle.tsx`, `index.html` |
| Branding | `src/components/IdeaStitchBrand.tsx`, `public/ideastitch-*.svg`, `public/ideastitch-icon.png` |
| Public homepage / prompt entry | `src/pages/Index.tsx` |
| Signup and login | `src/pages/Signup.tsx`, `src/components/LoginModal.tsx`, `src/components/AuthLayout.tsx` |
| Project lifecycle | `src/pages/ProjectsDashboard.tsx`, `src/lib/api.ts` |
| Builder workspace | `src/pages/ProjectView.tsx`, `src/components/ChatPanel.tsx`, `CodePanel.tsx`, `PreviewPanel.tsx`, related workspace components |
| Current subscription and payment portal | `src/pages/Billing.tsx`, `src/pages/CheckoutReturn.tsx`, `src/lib/api.ts`, `src/lib/types.ts` |
| Shared visual system | `src/index.css`, existing shadcn/Radix components |

The API base remains `https://api.ideastitch.in` in `src/lib/api.ts`. The supplied project uses React 18, TypeScript, Vite 5, Tailwind CSS 3.4, React Router 6, lucide-react, CodeMirror, and existing shadcn/Radix components. This package intentionally does not upgrade Tailwind or add daisyUI.

## Backend boundary and known limitations

No backend source was modified.

1. **Username authentication requires backend approval/change.** The supplied account service applies `@Email` validation to username fields. Signup/login with a plain username will fail server validation. The frontend requests username/password as specified but does not silently rewrite authentication validation.
2. **Starter template selection remains backend-limited.** The supplied workspace service initializes from one hard-coded starter. The frontend passes the original prompt to generation but does not fake a choice among starter templates or claim AI-generated naming; the project name is a local deterministic fallback until the separately scoped generation metadata workflow exists.
3. **No public pricing catalogue was found.** The billing UI uses the current-subscription and portal APIs actually present. It does not fabricate pricing plans or checkout choices.
4. **Browser file editing/save is not newly claimed.** The supplied editor is read-only and no matching public browser file-save endpoint was found. Existing source inspection and ZIP export remain available.

## Verification

Checks that did run against the source files in this package:

- TypeScript/TSX syntax transpilation: **84 source files, 0 syntax diagnostics**.
- Local import path scan: **0 unresolved relative or `@/` imports**.
- CSS parser: **460 top-level nodes, 0 CSS syntax errors**.
- Isolated project-title helper cases: **4/4 passed** (normal project prompt, description prompt, empty prompt fallback and 48-character cap).

Checks that could not be completed:

- `npm run build` could not run because `vite` is not installed in the current partial dependency tree.
- `npm test` could not run because `vitest` is not installed in the current partial dependency tree.
- `npm ci` could not finish because the environment could not resolve/reach `registry.npmjs.org` (`EAI_AGAIN`).
- Full TypeScript type checking could not run because `vitest/globals` and other dependency contents were unavailable.
- Browser-rendered desktop/mobile visual checks were not possible in this environment.

Therefore this package is **syntax- and import-checked**, but a clean production build and live API end-to-end run have not been verified here. After installing dependencies with network access, run:

```bash
npm ci
npm run build
npm test
npm run lint
```

## Manual regression checklist

- Open `/` signed out; toggle light/dark mode and reload. Confirm the selected theme persists and no wrong-theme flash is visible.
- At a laptop width (1366 × 768), verify the prompt input and orange Start building action are immediately visible, and the workspace preview is legible.
- Submit an example prompt; confirm it persists to signup/login, project creation occurs once, and the original prompt begins generation in the builder after project/history loading.
- Check password eye controls, password requirement messaging, missing-field errors, failed login/signup feedback, and loading states.
- Open `/projects`; test search, both sort options, open, rename, ZIP download, delete confirmation, refresh after load error, and the account menu.
- Open a project; check stream completion and stream errors, switching Code/Preview, file browsing, sharing and ZIP export.
- Open `/billing`; check API loading/error/retry, account-backed subscription fields and billing portal navigation.
- Test `/success.html` and `/cancel.html` with an authenticated test account; ensure an active subscription is only reported when the account API returns active status.
- Check footer at desktop width (four balanced columns), tablet, and mobile width (two-column footer), and verify text remains readable without horizontal overflow.
