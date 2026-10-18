# CLAUDE.md

Follow the rules in [CLAUDE_RULES.md](CLAUDE_RULES.md).

## graphify

Project questions go to graphify first. A knowledge graph of this project is in `graphify-out/`. For any codebase question, run `/graphify query "<question>"` before reading files or grepping, then open only the files the graph points to. Rebuild with `/graphify . --update` only after significant code changes.

## Alert rules

How the island picks the main pill, the companion bubble and what expires (tiers, two-slot cap, 7s/5s timers,
message grouping) is documented in [Rule_Book.md](Rule_Book.md) (sections 1 and 3). Read it before changing
`alert/AlertEngine.kt`, `IslandViewModel`, `PillLayout` or how the overlay service sizes the window; keep the
doc and `AlertEngineTest` in step with any behaviour change.

## Motion

Spring presets and the kinetic glow are documented in [Rule_Book.md](Rule_Book.md) (sections 2.2 and 2.3); change the maths in
`ui/IslandMotion.kt` with its tests, not inline in the overlay view.
