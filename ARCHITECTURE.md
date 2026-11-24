# Architecture

## Chosen architecture

```
Staff member
   │  double-clicks staff/start-gwr-assistant.cmd (or .sh)
   ▼
Claude Code, started in staff/
   ├─ .claude/settings.json          allow only the 7 GWR tools, deny everything else, no prompts
   ├─ .claude/output-styles/         staff rules replace the coding prompt
   ├─ .claude/skills/gwr-assistant/  answering rules
   └─ .mcp.json                      starts the MCP server
         ▼
gwr-mcp-server.jar (Java 17, MCP over stdio)
   ├─ GwrTools    7 tools, one per GET endpoint
   ├─ GwrClient   GET only, base URL from GWR_API_BASE_URL
   ├─ GwrAnswer   trims the JSON, adds code meanings, plain error messages
   └─ Codes       code meanings from codes.md (packed in the jar)
         ▼
GWR API  (GWR_API_BASE_URL, e.g. http://localhost:8080/gwr-api)
```

- **MCP server in Java:** exposes the 7 read endpoints of the GWR API as tools. It sends only GET requests; the two write endpoints (`UpdateOrderedQuantity`, `Validate`) aren't built.
- **Code meanings:** the server adds the documented meaning next to every code, taken from `codes.md`. A developer script (`tools/build-codes.ps1`) extracts that file once from gwr-docs.
- **Staff folder:** staff double-click a start script, which opens Claude Code in `staff/`. That folder holds everything the bot needs:
  - **Settings** allow only the 7 GWR tools and deny everything else, with no permission prompts.
  - **Output style** replaces Claude Code's coding prompt with staff rules: only GWR, plain tone, never talk about tools.
  - **Skill** holds the answering rules: data only from GWR, never guess, how to explain codes, and what to say when something can't be done.
- **API address:** one environment variable, so switching servers needs no code change.

## How it meets each requirement

| Requirement (TASK.md)                  | How                                                                                                                                                                                            |
|----------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Built with and runs in Claude Code     | An MCP server plus the `staff/` folder, started with `claude`. Built entirely in Claude Code sessions.                                                                                         |
| Only reads, never changes GWR          | `GwrClient` can only send GET (`src/main/java/com/houda/gwr/GwrClient.java`), and no write tool is registered. Read-only is enforced by code, not by a prompt.                                 |
| Configurable API address               | `GWR_API_BASE_URL`. The start script uses localhost only if the variable isn't already set.                                                                                                    |
| Used by staff, not developers          | See the rows below.                                                                                                                                                                            |
| · No commands, file access or web      | Settings deny Bash, PowerShell, Read, Edit, Write, Glob, Grep, web and agents. `dontAsk` mode refuses them silently, with no prompt a staff member could approve.                              |
| · No code or JSON                      | The skill and output style forbid code, JSON and field names. The server already trims the JSON: it drops empty and repeated fields, and replaces the daily sales with a total and an average. |
| · Plain answers in the user's language | The answer comes first in one line, with at most 10 list items. The default language is English.                                                                                               |
| · Correct, never guessed               | The meanings come from the docs, added by the server, so the model doesn't look them up. Undocumented codes say "not described in the documentation". Dates are shown as GWR gives them.       |
| · Clear when something fails           | "GWR is not reachable…", "Not found in GWR: …", and missing inputs are asked back with no defaults.                                                                                            |
| · Write requests                       | The bot says it can't, and names the GWR screen where it can be done.                                                                                                                          |
| · Off-topic requests                   | Politely refused.                                                                                                                                                                              |
| · Easy to install                      | Copy only `staff/`. It needs Java 17+ and Claude Code.                                                                                                                                         |
| State understandable for outsiders     | `README.md` covers what works, how to start, what's missing and the known limits. `PLAN.md` holds the decisions, findings and plan.                                                            |
| Session history kept                   | All work was done in Claude Code. The `.jsonl` files are in `%USERPROFILE%\.claude\projects\`.                                                                                                 |

## Options not chosen

| Option                                                                           | Why not                                                                                                                                                                                                     |
|----------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **A: rules + web fetch only** (no server; the model calls the API with WebFetch) | WebFetch condenses pages before the model sees them, so data may not be shown as returned. The URL would live in two places. Read-only would depend on instructions only, and the approach is hard to test. |
| **C: allowlisted script** (Bash allowed for one curl/script command)             | Command allowlists are fragile, and staff would see commands and JSON.                                                                                                                                      |
