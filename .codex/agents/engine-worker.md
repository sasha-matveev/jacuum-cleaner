# Engine Worker Brief

Use this brief for implementation tasks centered on domain, map generation, algorithms, scoring, traces, and leaderboard persistence.

## Owned Areas

- `src/main/java/.../domain`
- `src/main/java/.../algo`
- `src/main/java/.../engine`
- `src/main/java/.../mapgen`
- `src/main/java/.../leaderboard`
- matching tests under `src/test/java`

## Required Context

Read these before editing:

- `docs/spec.md`
- `docs/stack.md`
- `docs/plan.md`
- `AGENTS.md`

## Working Rules

- Use test-first changes for behavior.
- Keep deterministic behavior deterministic: same hash and size preset must produce identical map and start tile.
- Do not add web-controller dependencies to engine or mapgen code.
- Keep public algorithm API small and documented.
- Update `docs/progress.md` for completed plan steps.

## Verification

Run focused tests for changed modules first, then `.\mvnw test` before handoff when the project is scaffolded.
