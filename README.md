# HPS ADD 3.0 — Multi-Agent Architecture Design (Option 3)

Hotel Pricing System (HPS) architecture design using **ADD 3.0**, implemented with **Spring AI Alibaba** in a **hybrid multi-agent** topology:

- **Supervisor** — routes work across four ADD iterations and keeps conversation state.
- **Iteration pipelines** — each iteration runs a fixed four-stage pipeline with collaborative verification.

| Item | Value |
|------|--------|
| Course option | **Option 3 — Multi-agent** (distributed reasoning + collaborative verification) |
| LLM | **gpt-5.4** (OpenAI-compatible API via Spring AI) |
| Framework | Spring Boot 3.5 + Spring AI Alibaba Agent Framework |

---

## Architecture topology

```
User / SupervisorRunner
         │
         ▼
┌────────────────────────────────────────────────────────────┐
│  ADD Supervisor (add_supervisor)                           │
│  ReactAgent + MemorySaver — multi-turn orchestration       │
│                                                            │
│  Tools (function calling):                                 │
│    run_iteration_1 … run_iteration_4                     │
└────────────┬───────────┬───────────┬───────────┬──────────┘
             │           │           │           │
             ▼           ▼           ▼           ▼
      Pipeline-1   Pipeline-2   Pipeline-3   Pipeline-4
      (Iter 1)     (Iter 2)     (Iter 3)     (Iter 4)

Each pipeline — fixed four stages (sequential + one revision loop):

  Stage 1  driver_analyst        ADD Steps 1–2  Select architectural drivers
      │
      ▼
  Stage 2  structure_designer    ADD Steps 3–5  Elements, concepts, instantiation
      │                              ▲
      │         (if REVISION_NEEDED) │
      ▼                              │
  Stage 3  view_recorder           ADD Step 6     Mermaid diagram + design decisions
      │
      ▼
  Stage 4  quality_validator     ADD Step 7     QA/CON/CRN validation → APPROVED / REVISION_NEEDED
```

**Why hybrid?** The Supervisor provides a single entry point and ordered iteration routing (good for logs and Studio UI). Each `run_iteration_N` tool runs an internal **pipeline + peer review**, which matches Option 3’s *distributed reasoning* and *collaborative verification* requirements better than a pure Supervisor delegating opaque sub-tasks.

---

## Project layout

```
project_swsd/
├── README.md
├── pom.xml
├── mvnw
└── src/main/
    ├── java/.../supervisor/
    │   ├── SupervisorApplication.java       # Spring Boot entry
    │   ├── SupervisorConfig.java            # ADD Supervisor bean + IterationTools
    │   ├── SupervisorRunner.java            # Optional: run all 4 iterations on startup
    │   ├── AgentStaticLoader.java           # Registers add_supervisor for Studio
    │   └── add/
    │       ├── prior/
    │       │   └── HpsPriorKnowledge.java     # Single source of prior knowledge
    │       ├── agents/
    │       │   └── AddAgentsConfig.java       # 4 pipeline role agents
    │       ├── pipeline/
    │       │   ├── IterationPipeline.java     # One iteration, 4 stages + revision
    │       │   ├── IterationPipelineService.java
    │       │   └── IterationResult.java
    │       └── tools/
    │           └── IterationTools.java        # Exposes pipelines as Supervisor tools
    └── resources/
        └── application.yml
```

---

## Core files

| File | Role |
|------|------|
| `HpsPriorKnowledge.java` | **Prior knowledge** only: ADD 3.0 steps, HPS functionality (HPS-1…6), quality attributes (QA-1…9), constraints (CON-1…6), concerns (CRN-1…5), four-iteration plan. All agents must cite IDs from here only. |
| `AddAgentsConfig.java` | Defines four **role prompts**: Driver Analyst, Structure Designer, View Recorder, Quality Validator. Each embeds `ALL_PRIOR_KNOWLEDGE` and explicit **RULES** (no external domain knowledge). |
| `IterationPipeline.java` | Executes one iteration: Stages 1→2→3→4; on `REVISION_NEEDED`, repeats stages 2–4 once with validator feedback. |
| `IterationPipelineService.java` | Owns four pipelines (Iteration 1–4 goals) and exposes `runIteration(n)`. |
| `IterationResult.java` | Immutable record of drivers, design, Mermaid view, validation; `toSummary()` for tool return value. |
| `IterationTools.java` | `@Tool` methods `run_iteration_1` … `run_iteration_4` — Supervisor invokes these via function calling. |
| `SupervisorConfig.java` | **Supervisor** system prompt (workflow + prior knowledge) + `methodTools(iterationTools)` + `MemorySaver`. |
| `SupervisorRunner.java` | When `supervisor.run-examples=true`, sends one request to run all four iterations and logs full output (for deliverable logs). |
| `AgentStaticLoader.java` | Exposes `add_supervisor` to Spring AI Alibaba Studio (`/chatui/index.html`). |
| `application.yml` | OpenAI model config, `spring.ai.dashscope.enabled=false`, `supervisor.run-examples`. |

### Four ADD iterations (mapped to tools)

| Tool | Iteration goal | Primary drivers (from prior knowledge) |
|------|----------------|----------------------------------------|
| `run_iteration_1` | Establishing an Overall System Structure | CRN-1, CRN-2, CRN-3, CON-1, CON-2, CON-6 |
| `run_iteration_2` | Structures for Primary Functionality | HPS-1…HPS-6, QA-5 |
| `run_iteration_3` | Reliability and Availability | QA-2, QA-3, QA-1, QA-4 |
| `run_iteration_4` | Development and Operations | QA-7, QA-8, QA-9, QA-6, CRN-4, CRN-5 |

---

## How to run

### Prerequisites

- **JDK 17+**
- **Maven 3.6+** (or use `./mvnw` in the project root)
- **OpenAI API key** (model: `gpt-5.4` as configured in `application.yml`)

Set the API key (recommended — do not commit keys to Git):

```bash
export OPENAI_API_KEY=your-openai-api-key
```

DashScope is disabled in this project (`spring.ai.dashscope.enabled: false`).

### Build

```bash
cd /path/to/project_swsd
./mvnw -B package -DskipTests
```

If `./mvnw` fails (e.g. wrapper permissions), use a local Maven install:

```bash
mvn -B package -DskipTests
```

### Start the application

**Mode A — Server only** (no automatic design run):

```bash
# In application.yml set: supervisor.run-examples: false
./mvnw spring-boot:run
```

Or run the JAR:

```bash
java -jar target/hps-add-option3-0.0.1-SNAPSHOT.jar
```

**Mode B — Run full four-iteration design on startup** (for assignment logs):

```bash
export OPENAI_API_KEY=your-openai-api-key
export supervisor.run-examples=true
# or set supervisor.run-examples: true in application.yml

./mvnw spring-boot:run
```

The console prints timestamped logs for each pipeline stage (Driver Analyst → … → Validator). Redirect logs for the **interaction log** deliverable:

```bash
./mvnw spring-boot:run 2>&1 | tee logs/add-design-$(date +%Y%m%d-%H%M%S).log
```

### Studio UI (interactive)

After startup, open:

```text
http://localhost:8080/chatui/index.html
```

Select agent **`add_supervisor`** (not the old `personal_assistant` unless using the built-in alias).

**Troubleshooting — no reply in Chat UI**

| Symptom | Cause | Fix |
|---------|--------|-----|
| Send message, nothing appears | Browser thread still uses **`personal_assistant`** after rename | Restart app (includes alias), or create a **new thread** under **`add_supervisor`**, or clear site data for `localhost:8080` |
| Spinner runs 10+ minutes | Supervisor called `run_iteration_N`; one iteration = **4–8 LLM calls** | Wait, or ask only: `Summarize iteration 1 drivers without running tools` |
| Immediate error in server log | `OPENAI_API_KEY` empty | `export OPENAI_API_KEY=...` in the same terminal before `spring-boot:run` |
| Simple message, UI empty, log shows **`401 Unauthorized`** | Invalid key, wrong API host, or model not allowed | See [Fix 401](#fix-401-unauthorized) below |
| Only **「Create new thread」** visible | No active thread, or previous run failed | Click **Create new thread** → choose **`add_supervisor`** → send again |

Tools **are** registered (`IterationTools`: `run_iteration_1` … `run_iteration_4`); a missing framework is not the issue.

#### Fix 401 Unauthorized

Server log example:

```text
401 Unauthorized from POST https://api.openai.com/v1/chat/completions
```

1. **Same terminal**: `export OPENAI_API_KEY=...` then `./mvnw spring-boot:run` (IDE runs need the env var in Run Configuration too).
2. **Official OpenAI key**: must work against `https://api.openai.com` and support model `gpt-5.4` (or change model in `application.yml`).
3. **Third-party / school proxy**: set `spring.ai.openai.base-url` to that provider’s OpenAI-compatible endpoint (see commented line in `application.yml`).
4. **Verify** (replace key):

```bash
curl -s -o /dev/null -w "%{http_code}" https://api.openai.com/v1/models \
  -H "Authorization: Bearer $OPENAI_API_KEY"
```

`200` = key accepted by OpenAI; `401` = key rejected (UI will stay blank).

---

## Configuration

| Property | Description |
|----------|-------------|
| `spring.ai.openai.api-key` | OpenAI API key; use env var `OPENAI_API_KEY` |
| `spring.ai.openai.chat.options.model` | `gpt-5.4` |
| `spring.ai.dashscope.enabled` | `false` — avoids DashScope bean conflicts |
| `supervisor.run-examples` | `true` — run full HPS ADD design via `SupervisorRunner` on startup |

---

## Mapping to assignment (Option 3)

| Requirement | How this project satisfies it |
|-------------|-------------------------------|
| **Multi-agent system** | 1 Supervisor + 4 role agents × 4 iteration pipelines |
| **ADD 3.0 + HPS case + 4 iteration plan** | Encoded in `HpsPriorKnowledge.java`; injected into every system prompt |
| **Prior knowledge only** | Agent RULES forbid knowledge outside `HpsPriorKnowledge` |
| **Multiple role prompts** | `AddAgentsConfig` — four distinct roles |
| **Dialogue / workflow rules** | Supervisor prompt (iteration order, tool usage); per-agent RULES |
| **Distributed reasoning** | Each stage is a separate `ReactAgent` with its own prompt and output |
| **Collaborative verification** | Stage 4 Quality Validator; `REVISION_NEEDED` triggers redesign round |
| **Mermaid views** | View Recorder must output a ` ```mermaid ` block (Step 6) |
| **Source code (15 pts)** | This repository — pipeline + Supervisor + prior knowledge module |
| **Full interaction log (15 pts)** | Spring Boot logs with timestamps; use `tee` or IDE/console capture for all 4 iterations |
| **Report (20 pts)** | English report separate from this repo; copy Mermaid/decisions from logs |

**Deliverables checklist**

1. **Source code** — push this repo (exclude `target/`; never commit API keys).
2. **LLM interaction log** — capture startup run with `supervisor.run-examples=true` or Studio sessions; must include all four iterations with timestamps.
3. **Report** — English, ≤30 pages A4; align views and rationale with log outputs.

---

## Programmatic usage

```java
@Qualifier("supervisorAgent")
@Autowired
ReactAgent supervisorAgent;

AssistantMessage response = supervisorAgent.call(new UserMessage(
    "Run ADD iteration 1 for the Hotel Pricing System."));
String text = response.getText();
```

To run a single pipeline without the Supervisor:

```java
@Autowired
IterationPipelineService pipelineService;

IterationResult result = pipelineService.runIteration(1);
System.out.println(result.toSummary());
```

---

## Security note (GitHub)

- Do **not** commit real API keys in `application.yml`.
- Use `export OPENAI_API_KEY=...` or a local `application-local.yml` (listed in `.gitignore`).
- If a key was ever committed, rotate it in the provider console before pushing.

---

## Technology stack

- Java 17, Spring Boot 3.5.7
- Spring AI 1.1.2, Spring AI Alibaba Agent Framework 1.1.2.2
- Spring AI Alibaba Studio (Chat UI)
- OpenAI chat model (`spring-ai-starter-model-openai`)

---

## License

Apache License 2.0 (see file headers in source).
