# Bug Done retry suite (Katalon)

Use when **qa-suite-failed-bug-done-retry** writes a filtered retry suite under `Test Suites/`.

## Naming

| Piece | Rule |
|-------|------|
| Source suite | `SOURCE_SUITE` from report or `Test Suites/<name>.ts` |
| New suite name | `<SOURCE_SUITE>_BUG_DONE_RETRY_<yyyyMMdd>` |
| Collision | Same calendar day: replace `testCaseLink` entries with the latest eligible failed set |
| Files | `Test Suites/<name>.ts` (required), `Test Suites/<name>.groovy` (create only if missing) |

## Which test cases to include

Include a test case **only if all** are true:

1. It **failed** in the source suite report (`JUnit_Report.xml`).
2. It maps to a Jira Test Case (or valid direct Bug link per skill Phase 3b).
3. At least one linked Bug satisfies: **Bugs ticket != New && Bug Ticket == Done**.

Do **not** add passing cases. Do **not** add failures whose bugs are not **Done**.

Preserve source suite order from `Test Suites/<SOURCE_SUITE>.ts` when that file exists.

## XML template

Replace placeholders. Generate new `testSuiteGuid` and per-link `guid` values.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<TestSuiteEntity>
   <description>Failed cases with Done bugs from <SOURCE_SUITE> — report <REPORT_RELATIVE_PATH> — executed <EXECUTION_TIMESTAMP>. Filter: Bugs ticket != New &amp;&amp; Bug Ticket == Done. Bugs: <BUG_KEYS>.</description>
   <name><SUITE_NAME></name>
   <tag>failed-retry,bug-done,<STORY_KEY>,<SOURCE_SUITE></tag>
   <isRerun>false</isRerun>
   <mailRecipient></mailRecipient>
   <numberOfRerun>0</numberOfRerun>
   <pageLoadTimeout>30</pageLoadTimeout>
   <pageLoadTimeoutDefault>true</pageLoadTimeoutDefault>
   <rerunFailedTestCasesOnly>false</rerunFailedTestCasesOnly>
   <rerunImmediately>true</rerunImmediately>
   <testSuiteGuid><NEW_SUITE_GUID></testSuiteGuid>
   <testCaseLink>
      <guid><LINK_GUID></guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId><TEST_CASE_PATH></testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
</TestSuiteEntity>
```

Copy `isReuseDriver` / `usingDataBindingAtTestSuiteLevel` from the source suite link when available.

## After write

1. Tell the user the suite path and how to run (`run: yes` / CI / Katalon Studio).
2. Optional Jira comment on each **Done** Bug: `Automation retry suite: Test Suites/<name>.ts` — only if the user asks.
