# IdeaStitch frontend

This archive is the complete React/Vite frontend assembled at the project root. It preserves the supplied React 18, Vite 5, TypeScript, Tailwind CSS 3.4, and the supplied shadcn/Radix component stack and existing Industrial visual system.

## Local development

Requirements: Node.js 20+ and npm.

```bash
cp .env.example .env.local
npm ci
npm run dev
```

Set `VITE_API_URL` in `.env.local` only when you need a different API origin. The default is `https://api.ideastitch.in`.

## Production build

```bash
npm ci
npm run build
npm run test
npm run lint
```

The Dockerfile builds static assets with Vite and serves them using Nginx. It does not start a Vite dev server for the production frontend. The Nginx config supports direct refreshes of React Router URLs and avoids caching `index.html` indefinitely.

For a custom API origin, build with:

```bash
docker build --build-arg VITE_API_URL=https://api.ideastitch.in -t ideastitch-frontend:latest .
```

## Included changes

- JWT expiry validation, route guards, a single authenticated fetch path, redirect/cleanup on 401, and cross-tab sign-out handling.
- Homepage Pricing link and a `/pricing` page backed by the account-service plan catalog.
- “My subscriptions” remains under the profile menu and uses the existing Stripe billing portal endpoint.
- Checkout return polling confirms the subscription through the account API instead of trusting the Stripe return URL.
- Branded app metadata and a starter-template archive for newly generated preview applications.
- Static production hosting with SPA fallback and production asset caching rules.

## Important deployment dependency

The existing account service does not yet expose the public `GET /api/payment/plans` endpoint that the pricing screen expects. Checkout stays disabled while that endpoint is missing, rather than guessing a Stripe plan ID. Apply `docs/BACKEND-INTEGRATION.md` before enabling live subscriptions.

## Branded starter template

`deployment-assets/react-vite-tailwind-daisyui-starter-branded.zip` is the starter archive with IdeaStitch title, description, theme color, favicon and touch icon. Upload/extract its inner project into the same MinIO starter prefix that `KubernetesDeploymentServiceImpl` currently mirrors when initializing each generated project. Use the exact prefix and bucket configured for the deployed environment; do not overwrite user project folders.

## Validation note

The TypeScript/TSX sources were syntax-transpiled locally with zero syntax diagnostics. A complete dependency install and Vite build could not be run in this environment because npm registry access/dependencies were unavailable. The ZIP is complete, but run the build/test commands above after restoring registry access and before deploying.
