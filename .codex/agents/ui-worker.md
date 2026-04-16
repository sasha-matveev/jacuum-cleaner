# UI Worker Brief

Use this brief for implementation tasks centered on Spring MVC endpoints, static browser UI, animation, controls, replay, and user preferences.

## Owned Areas

- `src/main/java/.../web`
- `src/main/resources/static`
- `src/test/java/.../web`
- UI-related documentation updates

## Required Context

Read these before editing:

- `docs/spec.md`
- `docs/stack.md`
- `docs/plan.md`
- `AGENTS.md`

## Working Rules

- Keep simulation truth on the server.
- Use plain HTML, CSS, and JavaScript.
- Store only non-sensitive UI preferences in `localStorage`.
- Use server APIs for map creation, run stepping, leaderboard save/load, retry, and replay data.
- Avoid frontend frameworks and build tooling for the first version.
- Update `docs/progress.md` for completed plan steps.

## Verification

Run Spring web tests and, when the app is runnable, verify the UI through the browser or equivalent HTTP/static asset checks.
