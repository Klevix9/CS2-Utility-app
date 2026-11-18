## Agent workflow

Agents live in `.claude/agents/` (architect=fable, implementer=opus, tester=sonnet, scout=haiku).

- New feature or refactor → call `architect` first, then dispatch its tasks.
- Hard implementation → `implementer`. Small, obvious edits → do directly.
- Any codebase searching → `scout` instead of reading many files in the main session.
- Running tests / fixing trivial failures → `tester`.
- Run independent tasks in parallel.
- Keep the main session lean: delegate token-heavy work, keep summaries.

Test: `./gradlew :core:test -PcoreOnly` · Build: `./gradlew :app:assembleDebug`
