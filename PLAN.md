# GWR Chatbot – Plan

## Decisions (from customer answers, 2026-11-21)
- **Users:** staff start Claude Code themselves and just chat. They are not developers.
- **Restrictions:** staff must not be able to run commands, edit files or change GWR. They never see code or JSON.
- **Language:** answer in the language the user writes in; default English. Docs are English, so meanings come from the docs.
- **Server:** only the base URL is given. Check `openapi.yaml` for a login/security scheme; if none, assume no login.
- **Explain vs show:** show the data, and explain every code/warning in plain words using the docs. Never give a reason that isn't in the data or docs.
- **Show data as returned:** values exactly as GWR returns them. Never guess a meaning, unit or order that the docs don't state.
- **Sales:** show the total and the average of the 7 daily sales values. Don't claim which day is day 1, because the docs don't say.
- **Architecture (2026-11-21): option B.** Compared with A (rules plus web fetch only) and C (an allowlisted script):
  - **MCP server in Java:** one tool per read endpoint (GET only). The two write endpoints (`UpdateOrderedQuantity`, `Validate`) are not implemented. *Reason:* read-only is enforced by what exists, not by instructions, so the bot cannot change GWR even by mistake.
  - **Base URL from an environment variable.** *Reason:* switching to the customer's server on Friday is one setting with no code change.
  - **Settings deny all other tools** (commands, file edits, web fetch). *Reason:* staff cannot run commands or edit files, and no permission prompts appear.
  - **Skill with the answering rules** (language, show data as returned, explain codes from the docs, never guess, sales total and average, empty result means not found). *Reason:* one place for the behaviour, which we can review and test.
  - **Code file extracted once from gwr-docs** (warnings 1702/1745, statuses 1704/955, blocking or not, stock cover…), not fetched live. *Reason:* predictable, fast, testable, and no dependency on the docs server; meanings only come from the docs.
  - Rejected A: web fetch condenses pages before the bot sees them (data may not be shown as returned), the URL lives in two places, and it's weak to test. Rejected C: command allowlists are fragile, and staff would see commands and JSON.
- **Folder layout (2026-11-21): `staff/` subfolder.** It holds only the bot: its own CLAUDE.md, `.claude/settings.json`, `.mcp.json`, the skill and the code file. Staff start Claude Code inside `staff/`, and developers work in the repo root. *Reason:* `--settings` only adds a settings layer, so the developer's CLAUDE.md, skills and memory would still load for staff. A separate folder keeps the two apart, and only `staff/` is copied to staff machines. Rule: no developer notes in a root CLAUDE.md, because CLAUDE.md files from the folders above are loaded too. Check with `/context`, `/mcp` and `/permissions` inside `staff/`.
- **Code file coverage (2026-11-21):** all entries in gwr-docs for tables 1702 (line warnings), 1745 (header warnings), 1704 (order status) and 955 (article status), not only the codes seen in the test data. *Reason:* the customer's server on Friday may return codes the test data doesn't contain.
- **Build details (2026-11-21, confirmed):**
  - **Base URL:** the environment variable `GWR_API_BASE_URL`, e.g. `http://localhost:8080/gwr-api`.
  - **Build:** the MCP server reuses the existing `pom.xml` and uses Java 17 or newer.
  - **Unknown codes:** a code that isn't in the docs is shown as "unknown code X, not described in the documentation".
  - **Other codes:** the code file also covers the validation flag, warning flag, header alert type, profile link type, schedule origin and units, from the docs where a page describes them. Anything not documented is listed under Findings as "not documented".
- **Guardrails (2026-11-21):** `staff/.claude/settings.json` denies Bash, Edit, Write and web tools (WebFetch, WebSearch) and allows only the GWR MCP tools. Deny rules, not "ask", so staff never get a permission prompt they could approve.
- **Codes (2026-11-21):**
  - A script builds `codes.md` once from gwr-docs; it's re-run only when the docs change.
  - The MCP server loads `codes.md` at startup and adds the meaning next to each code in every tool answer, e.g. "12 – NPR: no price (blocking)". Unknown codes get "unknown code X, not described in the documentation".
  - Read is denied too.
  - *Reason:* the bot can't read any file, the meanings always come from the docs, and the code-to-meaning step is testable in the server.
- **`codes.md` build (2026-11-21, confirmed):**
  - **Script:** `tools/build-codes.ps1` (PowerShell, run by a developer, not part of the bot) reads the docs from `http://localhost:8080/gwr-docs/`.
  - **Location:** the script writes `src/main/resources/codes.md`, so it's packed inside the server jar. No environment variable; when the docs change, re-run the script and rebuild the jar.
  - **Format:** each entry has the table, code, short name, a one-line meaning, blocking or information only, the possible actions (what to do, from the docs) and the source doc page.
  - **Language:** English, as in the docs. The bot translates when the user writes in another language.
- **Runtime and start (2026-11-21, confirmed):**
  - **Java:** the server targets Java 17 (`pom.xml`), so it runs on more laptops.
  - **Jar:** the jar is copied into `staff/`, so the folder works on its own.
  - **Start script:** it sets `GWR_API_BASE_URL` to the default `http://localhost:8080/gwr-api` only if the variable isn't already set, so a value set beforehand (e.g. the customer's server on Friday) wins.
- **Finding blocked proposals (2026-11-22, customer rule):** open with `get_order_proposal` every proposal whose `orderProposalWarningFlagCode` is 1. The flag also covers header warnings, while the number of articles with warnings can be 0. Skip cancelled proposals (status 901). This rule is in the `list_order_proposals` description.
- **Dates and times (2026-11-22):** dates are shown exactly as GWR gives them, never relative to today. *Reason:* the test data is fixed in time, so everything would look past. Times are always shown as hh:mm (1100 → 11:00, 600 → 06:00), with no "most likely".
- **Warning codes:** the article `supplyWarningCode` uses the same table-1702 codes as the order line warnings (confirmed by customer, 2026-11-21).

## Findings
- 2026-11-21: Both WARs deployed on Tomcat 10.1.62 (Java 25), port 8080. `/gwr-api/`, `/gwr-api/openapi.yaml`, `/gwr-api/api-docs.html` and `/gwr-docs/` all return HTTP 200.
- 2026-11-21: `openapi.yaml` has no `security`/`securitySchemes`, so per the decision we assume no login.
- 2026-11-21: The API has 9 endpoints. Two of them change data and must never be called: `GWSOrderProposalUpdateOrderedQuantity` and `GWSOrderProposalValidate`.
- 2026-11-21: Line alert codes (`orderProposalDetailAlertCode`) = docs "Order line warnings" (table 1702 entries), e.g. 1=RDG, 5=STK, 12=NPR (blocking), 30=OUT. Each warning in the docs is marked as blocking (prevents validation) or for information only.
- 2026-11-21: Article `supplyWarningCode` matches the same table-1702 codes. For supplier 804006, codes 1/12/30/5 are on the same articles as the line alerts of proposal 500101. Inferred from the data and confirmed by the customer (see Decisions).
- 2026-11-21: Stock cover is in days ("Stock cover" doc page): the number of days the stock covers, based on consumption forecasts.
- 2026-11-21: Edge cases. Not found (unknown article, site, proposal or supplier) → HTTP 200 with an empty list, not an error. A missing required parameter → HTTP 400 `GWS-400 "Missing mandatory parameter: <name>"`. A non-numeric siteCode (`abc`) → 200 with an empty list. A page past the end → 200 with an empty list, and `totalPages` is still shown. So the bot must treat an empty result as "not found" and say so plainly.
- 2026-11-21: Header alerts = table 1745 ("Header warnings"). Proposal status = table 1704 (1=OK, 901=Cancelled). Article status = table 955 (1=Active).
- 2026-11-21: Forecast list `pageSize` max 100.
- 2026-11-21: Searched all 100 gwr-docs pages for the tables behind our API code fields. Only 1702, 1745, 1704 and 955 have their entries listed.
  - Table 1703 (Order type, for `orderProposalTypeCode`) and table 3033 (Schedule origin, for `scheduleOrigin`) are only named; their entries aren't in the docs.
  - Not documented: `profileLinkTypeCode` (the "Supply sector links screen" page is about supplier links, not profile link types), the validation, warning and note flags, `articleSUCode`, `fixedDeliveryTime`.
  - Codes from these fields are shown as "not described in the documentation".
- 2026-11-21: `codes.md` has 72 line warnings (1702), 29 header warnings (1745), 6 order statuses (1704) and 5 article statuses (955).
  - Header warnings CMin and CCMin are both documented as table 1745 entry 7. The server must show both meanings for code 7.
  - CCMax, CCMul and CCMin (1745 entries 5, 6 and 7) are "blocking or not depending on" table 1095 entry 4 NV2/NV3/NV1. They're marked "depends on configuration (…)".
  - 9 warnings don't say whether they block: EOL, UDW, OVS, MSV, MSR, RDI, CMin, SMin, RND++. They're marked "not stated in the documentation".
- 2026-11-22: Staff session check (`claude -p` in `staff/`): the mode is `dontAsk`, so there are no prompts and anything not allowed is refused. The only tools are Skill, ToolSearch and the 7 GWR tools.
  - The claude.ai connectors still connect, but their tools are blocked (`mcp__claude_ai_*`). `ENABLE_CLAUDEAI_MCP_SERVERS=false` in settings had no effect. Open point: whether their server instructions still reach the model.
  - Asked to run `dir` and create a file, the bot refuses, but answers in developer terms ("re-enable Bash and Write"). The skill (4.3) must give a plain staff answer instead.
- 2026-11-22: Skill test (`claude -p`, haiku, in `staff/`):
  - GWR questions follow the skill. "Validate 500101" → the blocking warning plus where to send it ([Send], Order proposal detail screen). German "Wie läuft Artikel 1000200?" → asks back for the site in German.
  - Non-GWR messages don't load the skill. "Run dir" and "weather in Berlin" are answered with tool talk and outside tips. Step 4.4 (`staff/CLAUDE.md`, always loaded) must carry the "only GWR, never mention tools" rules.
- 2026-11-21: `claude mcp list` also shows the user's claude.ai connectors (Claude Data, Docs, Meetings) as connected. Staff settings (step 4.2) must block them too.

## Open (decide later)
- (folder layout: decided, `staff/` subfolder; see Decisions)
- (code file access: decided, the server loads `codes.md`; see Decisions)
- (`codes.md` build: decided; see Decisions)

## Plan

Status per step: [ ] todo · [~] doing · [x] done. One step = one small change, checked before the next one.

### Phase 0 – MCP server skeleton
- [x] 0.1 `pom.xml`: add the MCP Java SDK and JUnit.
- [x] 0.2 `pom.xml`: a plugin that builds one runnable jar with all dependencies and the main class set.
- [x] 0.3 `main` that starts an MCP server over stdio with no tools. Logs go to stderr only, because stdout carries the protocol.
- [x] 0.4 Check: `mvn package` builds the jar, and `claude mcp` (from a test folder) connects to it and lists 0 tools.

### Phase 1 – `codes.md` from the docs
- [x] 1.1 `tools/build-codes.ps1`: read the "Order line warnings" page (table 1702) and write those entries to `src/main/resources/codes.md`.
- [x] 1.2 Add "Header warnings" (table 1745).
- [x] 1.3 Add the status tables 1704 and 955.
- [x] 1.4 Look up the other codes (validation flag, warning flag, header alert type, profile link type, schedule origin, units). Add the ones the docs describe; list the rest under Findings as "not documented".
- [x] 1.5 Review `codes.md`: spot-check entries 1, 5, 12 and 30 against the docs.

### Phase 2 – Server core (no tools yet)
- [x] 2.1 Read `GWR_API_BASE_URL`; if it's missing, stop with a clear message.
- [x] 2.2 GWR client that can only send GET requests to the base URL.
- [x] 2.3 Load `codes.md` from the jar and add the meaning next to each code (unknown → "unknown code X, not described in the documentation").
- [x] 2.4 Trim answers: empty fields, repeated input fields and unit codes (see Response trimming).
- [x] 2.5 Plain results: an empty list → "not found: …", a 400 → its message, server down → "GWR is not reachable". Number inputs are checked before the call.
- [x] 2.6 Unit tests for 2.3–2.5: `CodesTest` and `GwrAnswerTest` (with a local HTTP stub, so Tomcat isn't needed).

### Phase 3 – Tools (one step per tool, each with a test against the test server)
- [x] 3.0 Dev testing: `gwr` is registered for the developer only (`claude mcp add --scope local`, `GWR_API_BASE_URL=http://localhost:8080/gwr-api`); restart Claude Code to see the tools. Removed on 2026-11-22: local scope is stored per git repo, so it also loaded in `staff/` next to `staff/.mcp.json` (gwr twice). Test the bot through the staff start script instead.
- [x] 3.1 `get_order_proposal` (example Q3)
- [x] 3.2 `get_site_articles` with paging and the sales total and average (examples Q1 and Q2)
- [x] 3.3 `list_order_proposals`
- [x] 3.4 `get_supplier_delivery_schedule`
- [x] 3.5 `get_truck_capacity`
- [x] 3.6 `list_supply_sectors`
- [x] 3.7 `list_supply_sector_links`

### Phase 4 – `staff/` folder
- [x] 4.1 `staff/.mcp.json`: start the server jar and pass `GWR_API_BASE_URL`.
- [x] 4.2 `staff/.claude/settings.json`: deny Bash, Edit, Write, Read, web tools and all other tools; allow only the GWR tools.
- [x] 4.3 Answering skill with the rules from Decisions. Notes to include:
  - **"Blocking" questions (2026-11-22):** `get_site_articles` has no blocking filter, so call it once per line-warning code:
    - the blocking codes 11 LDA, 12 NPR, 24 FRQ, 70 MinA, 71 AMul;
    - the codes whose blocking isn't documented, 13 EOL, 79 UDW, 95 OVS, 98 MSV, 99 MSR.
    - Report the second group separately as "may block validation; the documentation doesn't say".
    - For a proposal, the header warnings CCMax/CCMul/CCMin are "blocking depending on configuration", and CMin, SMin, RDI and RND++ are "not stated in the documentation".
  - **Link types (2026-11-22):** show them only as "link type 1 / 2, meaning not documented". Never give a role example such as "responsible" or "deputy".
- [x] 4.4 Short `staff/CLAUDE.md`: what the bot is, and to use the skill.
- [x] 4.5 Start script for staff: set the default `GWR_API_BASE_URL` only if it isn't set already, then start Claude Code in `staff/`.

### Phase 5 – Verification in Claude Code (from `staff/`) – passed 2026-11-22 (all checks done by the developer via the start script)
- [x] 5.1 `/context`, `/mcp` and `/permissions` show only the bot setup: none of the developer's CLAUDE.md, skills or memory.
- [x] 5.2 The three TASK.md questions give the answers found on 2026-11-21.
- [x] 5.3 Edge cases: unknown article, site and proposal, missing input, text instead of a number, a German question, and Tomcat stopped (the bot says GWR is not reachable, with no technical error).
- [x] 5.4 Guardrails: ask it to run a command, edit a file, validate proposal 500101 or change a quantity. Each is refused, with no permission prompt.
- [x] 5.5 A different base URL works (simulate Friday).

### Phase 6 – Hand-over
- [x] 6.1 README: what works, how to start, what's missing.
- [x] 6.2 PLAN.md: statuses and the next ideas.

### Next ideas (not planned yet)
- **"Has a warning" filter for `get_site_articles` (2026-11-22):** the API can only filter by one warning code. For "which articles of supplier X have a warning", the bot fetches all the supplier's articles (29 rows, about 15 KB for 804006) and picks them itself, which can take several pages for a large supplier. Idea: an optional `onlyWithWarning` input that keeps only rows with a warning code (the server filters each page).
- **Proposal list with blocking warnings (2026-11-22):** the proposal list shows only a warning flag and the number of articles with warnings. So "which proposals of a site cannot be validated" needs one `get_order_proposal` call per proposal with warnings. Idea: an optional `withBlockingWarnings` input on `list_order_proposals`. The server opens each proposal with warnings itself and adds its blocking warnings to the row, judged by `codes.md`; undocumented ones are marked "may block".
- **Who validated a proposal (2026-11-22):** `orderProposalValidationProfileCode` (e.g. 105346 on 500108) is the same kind of profile code as in `list_supply_sector_links` (105346 = Jonas Wickert, linked to BEV01 and DAI01). The bot could name who validated a proposal by combining `get_order_proposal` with `list_supply_sector_links`. A profile that isn't linked to any sector of the site stays a bare code.
- **"Blocking only" filter for `get_site_articles` (2026-11-22):** today "which articles have a blocking warning" takes one call per code (5 blocking codes plus 5 where blocking isn't documented). Idea: an optional `blockingOnly` input where the server walks through all pages itself and returns only rows whose warning is blocking (or not documented, marked "may block"), using `codes.md`.

### MCP tools (approved 2026-11-21)
One tool per GET endpoint, under `<GWR_API_BASE_URL>/webservices/`. Every answer has the meaning from `codes.md` next to each code.

| Tool                             | Endpoint                                                       | Required                   | Optional                                                                                                  |
|----------------------------------|----------------------------------------------------------------|----------------------------|-----------------------------------------------------------------------------------------------------------|
| `get_site_articles`              | `GWSArticle/GWSSiteArticleForecastGetList`                     | `siteCode`                 | `articleCode`, `supplierCode`, `supplySectorCode`, `supplyWarningCode`, `articleStatusCode`, `pageNumber` |
| `list_order_proposals`           | `GWSOrder/GWSOrderProposalGetListHeaders`                      | `siteCode`                 | `supplierCode`, `supplySectorCode`, `orderProposalStatusCode`, `orderProposalNumber`                      |
| `get_order_proposal`             | `GWSOrder/GWSOrderProposalGetOrderProposal`                    | `orderProposalNumber`      | none                                                                                                      |
| `get_supplier_delivery_schedule` | `GWSThirdParty/GWSSupplierDeliveryScheduleGetList`             | `siteCode`, `supplierCode` | none                                                                                                      |
| `get_truck_capacity`             | `GWSOrder/GWSOrderProposalGetTruckCapacityDetail`              | `siteCode`, `supplierCode` | none                                                                                                      |
| `list_supply_sectors`            | `GWSCommon/GWSCommonSupplySectorSelectionGetListSupplySectors` | `siteCode`                 | none                                                                                                      |
| `list_supply_sector_links`       | `GWSCommon/GWSCommonSupplySectorSelectionGetListProfileLinks`  | `siteCode`                 | `supplySectorCode`, `profileLinkTypeCode`                                                                 |

Not built: `GWSOrderProposalUpdateOrderedQuantity` (POST), `GWSOrderProposalValidate` (POST).

Tool rules (confirmed 2026-11-21):
- **Paging:** `get_site_articles` returns one page plus the counts (total records, total pages, current page), and has an optional `pageNumber` input. The bot uses the filters first and fetches more pages only if needed. *Reason:* fetching all pages would be 9 calls and a huge answer for site 12. The page size stays at the API default (50) and isn't a tool input.
- **Inputs:** number inputs are declared as integers in the tool schema, so the SDK rejects text before the API is called (changed 2026-11-22: no extra check in the server).
- **Empty result:** returned as "not found: <what was asked>". A 400 from the API is returned as its plain error message.
- **Sales:** `get_site_articles` also returns the 7-day sales total and average.

Response trimming (2026-11-21), to save tokens. Fields are only dropped, never moved or restructured:
- **All tools:** drop empty fields (`""`, null). Zero values are kept.
- **Input fields:** drop fields that repeat a tool input in every row. Claude already knows them from the question.
  - `siteCode` in all site-based tools.
  - `siteCode` and `supplierCode` in `get_supplier_delivery_schedule` and `get_truck_capacity`.
  - In `get_order_proposal`, drop `orderProposalNumber` in lines and alerts, and `siteCode` and `supplierCode` in lines, because they're already in the header.
- **`get_site_articles` (2026-11-22):** drop `dailySalesDay1`–`7`. The server adds `salesTotal7Days` and `salesAverage7Days` (1 decimal) in their place. *Reason:* we never talk about single days (the docs don't say which day is day 1). Also drop `articleSUCode`: it isn't documented, so "not described" would be repeated in every row with no use for staff.
- **`get_truck_capacity`:** drop the unit codes (`fullTruckQuantity1UnitCode`, `orderOrSupplierMinimumQuantityUnitCode`), because the unit name is next to them.
- **Keep:** `articleCode` in line alerts, all codes (documented or not), `articleEANCode`, `profileCode`, dates plus day names, and times as returned.
- **Also kept (confirmed):** `siteDescription` stays in each row, as the only place the site name appears.
- **Also kept (confirmed):** `supplierAddressChainCode` and `supplierCommercialContractCode`, because they tell rows apart if a supplier has two contracts.

Verification: call each tool with site 12, article 1000200, supplier 804006 and proposal 500101, and with an unknown ID, a missing required input and a non-number input.