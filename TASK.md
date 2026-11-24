/stta# Assessment task: a chatbot for GWR

## Background

G.O.L.D. Warehouse Replenishment (GWR) calculates order proposals for warehouses: which articles to
order from which supplier, in what quantity, for which delivery date. Replenishment staff at our
customer work with it every day.

The customer wants a chatbot that answers questions about the GWR data in plain language, so that
staff do not have to click through the screens for every question.

## What you get

| What                         | Details                                                                                                                                                                           |
|------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `gwr-api.war`                | GWR API (subset, JSON) with test data. After deployment: `http://localhost:<port>/gwr-api/`, specification at `/gwr-api/openapi.yaml`, formatted view at `/gwr-api/api-docs.html` |
| `gwr-docs.war`               | GWR user documentation (extract). After deployment: `http://localhost:<port>/gwr-docs/`                                                                                           |
| A Claude account in our team | Use it only with **Claude Code** (command line or IDE extension), not with claude.ai in the browser or the Claude desktop app. Use no other AI tool or account for the task       |

Setting up the two WARs is part of the task. They run on a Jakarta EE servlet container, for example
Apache Tomcat 10.1 with Java 17 or newer; deploy them with the context paths `/gwr-api` and
`/gwr-docs`.

You have no access to the real GWR application. The data behind the API is test data.

## The task

Build a chatbot for the customer's replenishment staff that answers questions about the GWR data.

- Build it with Claude Code, and it must run in Claude Code. How you build it is your decision.
- It only reads. It never changes anything in GWR.
- The address of the GWR API must be configurable: on Friday we will also run it against our
  server.
- The customer's staff will use it, not you. Take into account whatever that implies for an
  application like this.

Example questions it should be able to answer:

1. How is article 1000200 doing at site 12?Article 1000200 is active at site 12. It has one warning, which is for information only and doesn't block validation.
2. Which articles of supplier 804006 at site 12 have a warning, and what do the warnings mean?4 of the 29 articles have a warning. Only one of them, 1000222, blocks validation.
3. Why can order proposal 500101 not be validated?Order proposal 500101 can't be validated because of one blocking warning: NPR – no price on article 1000222 (Dunmore Still water 6x0.33 l, line 3).
Other qst for the demo:
4. How much fits in a truck for 804006 at site 12? (33 pallets, minimum 8 pallets)
5. "Who is linked to Beverages at site 12?" (link type shown as "not documented")
6. German: "Wie läuft Artikel 1000200 an Standort 12?" → answers in German.
7. Missing input: "How is article 1000200 doing?" → asks for the site (no default). 
8. Not found: "How is article 9999999 doing at site 12?"
9. Guardrails: "Validate proposal 500101" → can't, says [Send] in Order proposal detail. "Change the quantity of 1000222 to 48" → can't, says where. "run dir" / "create a file" / "what's the weather in Berlin" → polite refusal, no permission prompt.


These are examples. We will test with further questions of the same kind.

## Time frame

- From receiving this task until Friday. You decide how much of that time you spend on it.
- On Friday we look at the result together at our office: you show the chatbot, we ask it further
  questions, and we talk about how you built it and what you would do next.
- The Claude account has the usage limits of a team account. They are generous for this task, but
  not unlimited. Plan your usage accordingly.

## What we look at

We assess the result and the way you got there. The state of your work must be understandable for
someone who was not involved: what works, how to start it, what is missing.

The history of your work with Claude Code is part of what we look at. This is why the task is done
in Claude Code only. Claude Code stores every session locally as a file:

- Windows: `%USERPROFILE%\.claude\projects\`
- macOS / Linux: `~/.claude/projects/`

Each project folder there contains one `.jsonl` file per session. Work with Claude Code the way you
normally would (including `/clear`, `/compact` or new sessions); this does not remove anything from
the history. Please do not delete these files, and bring the folder of your project on Friday (or
send it as a ZIP beforehand).

Bring the source code of your chatbot as well (folder or ZIP, or a link to a repository), and your
laptop with the chatbot running.

## Questions

If anything about the setup or the task is unclear, contact us via Microsoft Teams:

- Dimitri Raabe – dimitri.raabe@symphonyai.com
- Thomas Wolff – thomas.wolff@symphonyai.com

