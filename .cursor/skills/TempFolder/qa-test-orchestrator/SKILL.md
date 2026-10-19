---
name: qa-test-orchestrator
description: >-
  End-to-end QA pipeline. Takes a feature or story prompt, retrieves Jira and
  Confluence requirements, then writes structured test cases. Use when the user
  asks to generate test cases from Jira, Confluence, or a feature name.
---

# QA Test Orchestrator (Agent-3)

You coordinate Agent-2 (knowledge retrieval) and Agent-1 (test case writing).
Execute all phases in order. Do not skip phases.

## Trigger examples

- "Generate test cases for PROJ-123"
- "Write regression tests for Login feature"
- "Using qa-test-orchestrator: Checkout flow, scope full"

## Input you accept

| Parameter | Required | Default |
|-----------|----------|---------|
| Feature name and/or Jira key | Yes (at least one) | — |
| Scope | No | `regression` |
| Confluence URL | No | Resolved via doc-index |
| Extra instructions | No | — |

---

## Phase 1 — Gather Requirements (Agent-2)

Follow every step in [qa-knowledge-retriever/SKILL.md](../qa-knowledge-retriever/SKILL.md):

1. Read [doc-index.md](../qa-knowledge-retriever/doc-index.md)
2. Fetch Jira + Confluence content via Atlassian MCP
3. Produce the **strict output format** from Agent-2 (Jira Summary, Confluence Requirements, Gaps)

**Checkpoint:** Do not proceed until you have acceptance criteria or documented gaps.

---

## Phase 2 — Generate Test Cases (Agent-1)

Follow every step in [qa-test-case-writer/SKILL.md](../qa-test-case-writer/SKILL.md):

1. Input = full requirements bundle from Phase 1
2. Apply user scope (`smoke` | `regression` | `full`)
3. Apply user extra instructions (e.g. "include security cases")
4. Write test cases using [test-case-template.md](../qa-test-case-writer/test-case-template.md)

**Checkpoint:** Run Agent-1 quality gate before saving.

---

## Phase 3 — Coverage Validation

Before delivering, verify:

| Check | Rule |
|-------|------|
| AC coverage | Every acceptance criterion has ≥ 1 test case |
| Negative cases | At least 1 per feature (if applicable) |
| Boundary cases | At least 1 per feature (if applicable) |
| Traceability | Every `Requirement_Ref` column filled |
| Uniqueness | All `TC_ID` values unique |
| Gaps | Assumptions from Phase 1 reflected in file header |

If a check fails, add missing cases and re-validate.

---

## Phase 4 — Deliver

1. Save to `TestCases/<feature-slug>-test-cases.md`
2. Reply to the user with this summary:

```markdown
## Pipeline Complete

**Feature:** [name]
**Source:** [Jira keys] | [Confluence pages]
**Output file:** TestCases/<feature-slug>-test-cases.md

### Coverage
| Type | Count |
|------|-------|
| Positive | N |
| Negative | N |
| Boundary | N |
| **Total** | N |

### Acceptance Criteria Coverage
- AC-1: TC-XXX-001, TC-XXX-002
- AC-2: TC-XXX-003

### Gaps / Assumptions
- [from Phase 1, if any]
```

---

## Error handling

| Situation | Action |
|-----------|--------|
| Atlassian MCP not authenticated | Stop; instruct user to connect MCP |
| Jira key not found | Search by feature name; if still missing, ask user for correct key |
| No Confluence spec | Proceed with Jira only; flag gaps in output |
| Requirements too vague | Generate cases for known items; list assumptions prominently |

## Do not

- Skip Phase 1 and write test cases from memory
- Change the test case table format
- Save files outside `TestCases/`
