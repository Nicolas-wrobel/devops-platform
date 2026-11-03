# ADR-0023 : Frontend Design System (shadcn/ui) and Test Runner (Vitest)

## Status
Accepted

## Date
2026-11-02

## Context
The frontend was functionally complete through roadmap step 9 but visually
generic: no design tokens, no dark mode, no component library, four pages
hand-rolling identical Tailwind utility strings with zero shared components,
no landing page, and zero automated frontend test coverage. Since this
project exists to showcase the author's engineering profile, the UI needed
to look and feel like a real product. This covers the tooling decisions
behind that redesign: the component library, the testing setup, and the
new information architecture (public landing page + authenticated
dashboard overview).

## Options Under Consideration
- **Component library: shadcn/ui vs. a hand-built design system vs. a
  packaged UI kit (e.g. MUI/Chakra) (chosen: shadcn/ui)** — shadcn/ui
  copies component source directly into the repo rather than being an
  opaque npm dependency (full control, no black-box styling fights),
  ships accessible-by-default primitives (fixing the label/focus/keyboard
  gaps already flagged in this codebase), and uses CSS-variable theming
  that maps naturally onto Tailwind v4 (already in use, see
  `package.json`). A hand-built system would mean redesigning every
  accessibility concern from scratch; a packaged kit (MUI/Chakra) brings
  its own styling engine, fighting Tailwind rather than complementing it.
- **Primitive layer actually delivered: Base UI, not Radix (discovered
  during implementation, accepted)** — this project's shadcn/ui CLI
  version (`shadcn@4.23.0`, as of this writing) defaults to Base UI
  (`@base-ui/react`) rather than the Radix UI primitives most
  shadcn/ui material online still assumes. Functionally equivalent for
  this project's purposes (same accessibility guarantees, same
  CSS-variable theming model), but its composition API differs from
  Radix's `asChild` pattern (no `asChild` support on `Button`; a styled
  `react-router Link` uses the exported `buttonVariants()` class
  function directly instead) and its `Select.onValueChange` can pass
  `null` (handled by falling back to `''`).
- **Dark mode: CSS-variable tokens + a `.dark` class toggle vs.
  OS-only `prefers-color-scheme` (chosen: class toggle, OS-aware by
  default)** — a persisted, user-togglable mode needs an explicit
  `@custom-variant dark (&:is(.dark *));` selector (emitted by shadcn's
  own `init`), not Tailwind's default media-query-only dark variant.
  `lib/theme.ts` stores the choice (`light`/`dark`/`system`) in
  `localStorage` and applies it synchronously before React mounts (in
  `main.tsx`), avoiding a flash of the wrong theme.
- **Test runner: Vitest + React Testing Library vs. no frontend tests
  (chosen: add them now)** — zero automated frontend coverage existed
  before this, and a redesign touching all four existing pages at once
  was exactly the kind of change a regression safety net is for. Scope
  is deliberately smoke + key-logic tests per new/changed component
  (render-without-crashing, a few behavior assertions), not exhaustive
  coverage — a safety net for this redesign, not a coverage drive.
- **Public landing page data: a new narrow public backend endpoint vs.
  a static-only page (chosen: new endpoint)** — see ADR-0022; covered
  here only as the frontend-side consequence: `/` is now a real public
  route, and the previous catch-all (unmatched paths → `/environments`)
  now goes to `/` instead, with the landing page itself branching its
  CTA on auth state.
- **Dashboard data: a new aggregate backend endpoint vs. client-side
  aggregation of existing authenticated GETs (chosen: client-side)** —
  `/dashboard` reuses the already-authenticated `/environments`,
  `/applications`, `/deployments` list endpoints and aggregates counts/
  recent items in the browser, avoiding a second backend endpoint for
  data the client can already fully see.

## Decision
`shadcn/ui` (Base UI primitives, CSS-variable theming, `lucide-react`
icons, `tw-animate-css` for CSS-only animation — no Framer Motion) is
the frontend's design-system layer going forward. All four existing
pages were migrated to it; two new pages were added: a public
`LandingPage` (`/`, live stats from `/api/public/stats`, CTA branching
on auth state) and an authenticated `DashboardPage` (`/dashboard`,
summary cards + recent deployments with color-coded status badges via
a new shared `DeploymentStatusBadge`). `LoginPage` now redirects to
`/dashboard` instead of `/environments`. Vitest + React Testing Library
is the frontend test runner, wired into CI (`npm run test` between
`lint` and `build`).

## Consequences
- Every new or hand-edited component should use the `src/components/ui/`
  primitives and CSS-variable tokens (`bg-background`,
  `text-foreground`, `border-border`, etc.) instead of raw Tailwind
  palette classes — this is what makes dark mode automatic rather than
  something to re-implement per page.
- `src/components/ui/**` is treated as vendored/generated code (added
  via `npx shadcn add <component>`, not hand-authored) — including a
  targeted ESLint override for `react-refresh/only-export-components`,
  since shadcn components conventionally export a `*Variants` helper
  alongside the component itself.
- The shadcn CLI's `@/*` alias resolution did not work correctly in
  this Tailwind v4 / Vite 7 / no-config-file setup (it wrote generated
  files to a literal `apps/frontend/@/` directory instead of `src/`);
  each `add` run needs its output checked and, if needed, moved into
  `src/` by hand.
- The generated `Sonner` (toast) wrapper depended on `next-themes` (a
  Next.js-specific theming library) for its color scheme — replaced
  with a small `useIsDarkMode()` hook (`useSyncExternalStore` + a
  `MutationObserver` on `<html>`'s class list) in `lib/theme.ts`,
  keeping a single theme source of truth instead of two.
- Frontend bundle size grew measurably (shadcn/Base UI + testing
  tooling): Vite now warns about a >500kB JS chunk. Not addressed in
  this pass (no code-splitting) — acceptable for this project's size
  and traffic, but a candidate follow-up if the bundle keeps growing.
- `docker-frontend-1`'s dev container uses an anonymous Docker volume
  for `node_modules` (see `compose.dev.yml`), which does **not**
  automatically pick up newly-added npm dependencies on a plain
  rebuild — it needs `docker compose up --force-recreate -V` (or
  equivalent) to renew that volume after `npm install`ing anything new
  on the host. Not new to this ADR, but this redesign's frequent
  dependency additions made the gap very visible; worth remembering
  for future frontend dependency changes.
