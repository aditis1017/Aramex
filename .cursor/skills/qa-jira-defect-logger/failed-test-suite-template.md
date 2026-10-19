# Failed test suite (Katalon)

Use when **Phase 4D** of **qa-jira-defect-logger** creates or updates a retry suite under `Test Suites/`.

## Naming

| Piece | Rule |
|-------|------|
| Source suite | `testsuite/@name` from `JUnit_Report.xml`, or folder name under `Reports/<ts>/` |
| New suite name | `<SOURCE_SUITE>_FAILED_<yyyyMMdd>` |
| Collision | If `Test Suites/<name>.ts` already exists for the same calendar day, **replace** `testCaseLink` entries with the latest failed set from this report (do not duplicate links). |
| Files | `Test Suites/<name>.ts` (required), `Test Suites/<name>.groovy` (create only if missing — copy stub from `Test Suites/CCI_APM_CCIPH-634_SUITE.groovy`) |

## Which test cases to include

1. From the parsed report, collect every `<testcase>` with `status="FAILED"` or a `<failure>` child.
2. Use `testcase/@name` or `testcase/@classname` as `testCaseId` (must start with `Test Cases/`).
3. Verify each path exists under the project (`Test Cases/.../*.tc` or folder). Drop entries that do not exist; note dropped IDs in Phase 5 delivery.
4. Preserve **source suite order** when `Test Suites/<SOURCE_SUITE>.ts` exists: include only failed IDs, in the same order as the source `.ts` `testCaseLink` list.
5. If the source `.ts` is missing, order failed cases alphabetically by `testCaseId`.

## XML template

Replace placeholders. Generate new `testSuiteGuid` and per-link `guid` values (UUID v4 or project-style unique strings). Do not reuse GUIDs from the source suite.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<TestSuiteEntity>
   <description>Failed cases from <SOURCE_SUITE> — report <REPORT_RELATIVE_PATH> — executed <EXECUTION_TIMESTAMP>. Created by qa-jira-defect-logger. Bugs: <BUG_KEYS_OR_NONE>.</description>
   <name><SUITE_NAME></name>
   <tag>failed-retry,qa-jira-defect-logger,<STORY_KEY>,<SOURCE_SUITE></tag>
   <isRerun>false</isRerun>
   <mailRecipient></mailRecipient>
   <numberOfRerun>0</numberOfRerun>
   <pageLoadTimeout>30</pageLoadTimeout>
   <pageLoadTimeoutDefault>true</pageLoadTimeoutDefault>
   <rerunFailedTestCasesOnly>false</rerunFailedTestCasesOnly>
   <rerunImmediately>true</rerunImmediately>
   <testSuiteGuid><NEW_SUITE_GUID></testSuiteGuid>
   <!-- Repeat per failed TC -->
   <testCaseLink>
      <guid><LINK_GUID></guid>
      <isReuseDriver>false</isReuseDriver>
      <isRun>true</isRun>
      <testCaseId><TEST_CASE_PATH></testCaseId>
      <usingDataBindingAtTestSuiteLevel>false</usingDataBindingAtTestSuiteLevel>
   </testCaseLink>
</TestSuiteEntity>
```

Copy `isReuseDriver` / `usingDataBindingAtTestSuiteLevel` from the matching `testCaseLink` in the source suite when available.

## After write

1. Do **not** add the suite to CI by default; tell the user they can run it locally or add it to a workflow if needed.
2. Mention the suite path in the **story** log comment (Phase 4C): `Retry suite: Test Suites/<name>.ts`.
