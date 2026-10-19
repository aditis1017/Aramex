---
name: github-aramex-push
description: >-
  Commits Katalon automation on the current branch and pushes it to
  https://github.com/aditis1017/Aramex.git so GitHub Actions CI starts.
  Invoke with /github-aramex-push and a commit message, or when the user asks
  to push this project to aditis1017/Aramex and trigger CI/CD.
---

# Push to aditis1017/Aramex and trigger CI

Push the **current branch** to remote `github` (`https://github.com/aditis1017/Aramex.git`). A push of `.github/workflows/aramex-ci.yml` starts the **Aramex CI** workflow on every branch.

Invoking this skill is approval to commit (when a message is supplied) and to push **only** to remote `github`. Do not ask for a second confirmation.

## Input

- **Required when the worktree has changes to publish:** commit message (one or two sentences). If it is missing, stop and ask. Do not invent a message.
- **Branch:** the branch already checked out. Do not create or switch branches unless the user names one in the same request.
- **Remote:** `github` only. Never push to `origin`.

## Prerequisites (stop on failure)

1. `git rev-parse --is-inside-work-tree` is `true`.
2. Current branch is not `main`, `master`, or `release/*`.
3. Worktree is not in the middle of a merge, rebase, or cherry-pick.
4. `git config user.name` and `git config user.email` are already set. If either is empty, stop. Do not run `git config`.
5. Remote `github` exists and its URL is `https://github.com/aditis1017/Aramex.git`. If the remote is missing, add it. If the URL differs, run `git remote set-url github https://github.com/aditis1017/Aramex.git`. Do not change `origin` or any other remote.
6. **Secret scan.** Before staging, search the paths you will add for API keys, passwords, tokens, and private keys. Also check `.github/workflows/master.yml`. If a literal Katalon `apiKey` value is still in that file or in any file you would stage, stop. Name the file. Do not print the secret. Tell the user to remove it and rotate the key. The key is already in git history of this clone; say that before the first push to `github`, because a push publishes that history.

## Safety

- Never `git push --force`, `--force-with-lease`, `reset --hard`, `clean`, or `--no-verify`.
- Never update git config. Never skip hooks. Never amend unless the user explicitly asked and the amend rules are all met.
- Never stage `.env`, `credentials.json`, `*.pem`, `*.key`, `Profiles` values that contain passwords, or `.cursor/mcp.json`.
- Never stage Katalon generated output: `Reports/`, `bin/`, `Libs/`, `build/`, `.cache/`, `.classpath`, `.project`, `output/` (except `output/.gitkeep`), `Downloads/*` (except `Downloads/.gitkeep`).
- Do not stage `BE-ApplicationCode/` or `FE-ApplicationCode/` unless the user names that path in the same request.
- Do not use `git add -A` or `git add .`.
- On PowerShell, do not chain commands with `&&`.

## Order

Run these in parallel first:

```text
git status
git diff
git diff --staged
git log -8 --oneline
git remote get-url github
```

1. **Ensure CI workflow is present.** `.github/workflows/aramex-ci.yml` must be in the commit you push. It triggers on every `push`. Do not put a Katalon API key in the file. Runtime Engine reads `secrets.KATALON_API_KEY`.
2. **Stage Katalon source and the workflow.** `git add` only paths that exist among: `Test Cases`, `Test Suites`, `Scripts`, `Keywords`, `Object Repository`, `Profiles`, `Data Files`, `Test Listeners`, `Include`, `Checkpoints`, `settings`, `*.prj`, `.gitignore`, `.github/workflows/aramex-ci.yml`, `.github/workflows/master.yml`. Add any other path the user named. Skip a path that fails the secret scan.
3. **Commit** when something is staged:

```powershell
git commit -m @"
Commit message here.

"@
```

   If nothing is staged, skip the commit and push existing commits. If a hook rejects the commit, fix the reported issue and create a new commit. Do not amend and do not use `--no-verify`.
4. **Push.**

```powershell
git push -u github HEAD
```

   If authentication fails, stop. Tell the user to sign in to GitHub for `https://github.com/aditis1017/Aramex.git` with Git Credential Manager. Do not ask for a token or password.
5. **Confirm CI started.** Query [aditis1017/Aramex](https://github.com/aditis1017/Aramex) Actions for this branch. Prefer `gh` when it is installed:

```powershell
gh run list --repo aditis1017/Aramex --branch <current-branch> --limit 5
```

   If `gh` is missing, call the public API (no token):

```powershell
Invoke-RestMethod -Uri "https://api.github.com/repos/aditis1017/Aramex/actions/runs?per_page=5" -Headers @{ "User-Agent" = "github-aramex-push"; Accept = "application/vnd.github+json" }
```

   Keep the run whose `headBranch` is the current branch. If no run is visible yet, wait 15 seconds and query once more. Actions are queued only after the workflow file exists on the remote. Report the run URL `https://github.com/aditis1017/Aramex/actions/runs/<id>`, or the Actions page `https://github.com/aditis1017/Aramex/actions` when the API has no run yet.
6. **Verify.** `git status -sb`. The branch should track `github`, not `origin`.

## Report

- Branch name and remote URL pushed.
- Commit hash and message, or that there was nothing new to commit.
- Push result.
- CI run URL and status (`queued`, `in_progress`, or `completed`). If Katalon did not execute, say whether `KATALON_API_KEY` still needs to be added under the repository secrets.
- Paths committed, and paths left unstaged.
