# GWR Chatbot

A chatbot in Claude Code that answers replenishment staff's questions about GWR data in plain language. Read-only. Decisions, findings, plan and next ideas: see `PLAN.md`.

## What works
- **Questions about GWR data** in plain words, in the user's language (default English), for example:
  - "How is article 1000200 doing at site 12?"
  - "Which articles of supplier 804006 at site 12 have a warning, and what do they mean?"
  - "Why can order proposal 500101 not be validated?"
  - "Which proposals at site 12 cannot be validated?"
  - Delivery schedules, truck capacity and order minimums, supply sectors, and the people linked to them.
- **7 read-only tools** in a Java MCP server (`staff/gwr-mcp-server.jar`), one per GET endpoint of the GWR API. The two write endpoints (update quantity, validate) are not built.
- **Code meanings** come from the GWR docs. Warnings show blocking or not and what to do; statuses show their names; undocumented codes say "not described in the documentation".
- **Configurable GWR address:** one environment variable, `GWR_API_BASE_URL`.
- **Guardrails:** staff can't run commands, read or edit files, or use the web. There are no permission prompts. For anything not about GWR, the bot politely says it only answers GWR questions.
- **Tests:** 14 unit tests (run by the build, see Build). Phase 5 checks passed on 2026-11-22 (see `PLAN.md`).

## How to start
1. **GWR test environment:** copy `gwr-api.war` and `gwr-docs.war` into `<tomcat>\webapps\` (Tomcat 10.1, Java 17+), then run `<tomcat>\bin\startup.bat`. Check that `http://localhost:8080/gwr-api/` and `http://localhost:8080/gwr-docs/` open.
2. **Chatbot** (needs Java 17+ and Claude Code, logged in):
   - Windows: double-click `staff\start-gwr-assistant.cmd`.
   - Mac/Linux: `sh staff/start-gwr-assistant.sh`.
   - Both use `http://localhost:8080/gwr-api` unless `GWR_API_BASE_URL` is already set. For another GWR server, set it in the same window before starting:
     - Windows cmd: `set GWR_API_BASE_URL=http://<server>:<port>/gwr-api`
     - Windows PowerShell: `$env:GWR_API_BASE_URL="http://<server>:<port>/gwr-api"`
     - Mac/Linux: `export GWR_API_BASE_URL=http://<server>:<port>/gwr-api`
3. **For staff PCs:** copy only the `staff` folder. It holds everything the bot needs: jar, settings, style, skill and start scripts.

## Developer tools
- **Build:** `mvnw.cmd package` (Windows) or `./mvnw package` (Mac/Linux) runs the tests and writes the jar straight into `staff/`. It needs only Java 17+; the Maven wrapper downloads Maven 3.10.1 itself. Restart the bot afterwards: overwriting the jar while a bot session is running can make that session hang.
- **Codes:** `tools/build-codes.ps1` builds `src/main/resources/codes.md` from gwr-docs (it isn't part of the bot). Re-run it only when the docs change, then rebuild (see Build).
  1. Rebuild the code meanings:
     - Windows: `powershell -ExecutionPolicy Bypass -File tools\build-codes.ps1`
     - Mac/Linux: `pwsh tools/build-codes.ps1` (PowerShell 7).
     - For another docs server, add `-DocsUrl http://<server>:<port>/gwr-docs/`.
  2. Close the bot (`/exit`).
  3. Pack them into the jar: `.\mvnw.cmd package` (Windows) or `./mvnw package` (Mac/Linux).
- **Layout:**
  - `src/` – the MCP server: `GwrTools` (tool definitions), `GwrAnswer` (cleans answers and adds meanings), `Codes`, `GwrClient` (GET only).
  - `staff/` – the bot as staff use it.
  - `tools/` – developer scripts.

## What's missing
- The next ideas in `PLAN.md`, mainly a proposal list that already shows blocking warnings. Today "which proposals cannot be validated" needs one call per proposal with warnings.
- Untested: `start-gwr-assistant.sh` on a real Mac/Linux, and `build-codes.ps1` with `pwsh`.

## Known limits
- **Model:** use the default model. With haiku (`--model haiku`) the bot ignored the staff rules for non-GWR messages in tests (it talked about tools and gave off-topic tips). GWR answers still worked.
- **claude.ai connectors:** if the staff member's Claude account has claude.ai connectors (e.g. Claude Docs, Meetings), they still connect when Claude Code starts in `staff/`. Their tools are blocked (`mcp__claude_ai_*` in `staff/.claude/settings.json`), so the bot can't use them. Setting `ENABLE_CLAUDEAI_MCP_SERVERS=false` in the settings had no effect. To remove them completely, disable them in that account.
- **Test data is fixed in time:** dates are shown exactly as GWR gives them, never relative to today.
- **Not found vs. doesn't exist:** GWR answers an unknown site, article or supplier the same way as "no data", so the bot can only say "not found, please check".
