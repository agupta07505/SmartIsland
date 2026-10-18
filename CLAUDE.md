# CLAUDE.md

Follow the rules in [CLAUDE_RULES.md](CLAUDE_RULES.md).

## graphify

Project questions go to graphify first. A knowledge graph of this project is in `graphify-out/`. For any codebase question, run `/graphify query "<question>"` before reading files or grepping, then open only the files the graph points to. Rebuild with `/graphify . --update` only after significant code changes.
