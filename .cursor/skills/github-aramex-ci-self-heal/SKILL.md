---
name: github-aramex-ci-self-heal
description: >-
  Stages all changes, pulls from https://github.com/aditis1017/Aramex.git,
  resolves merge conflicts, pushes the current branch, and starts GitHub
  Actions. A failed Aramex CI run is re-executed through CI/CD. The self-heal
  loop is 3 attempts total, not 4. Invoke with /github-aramex-ci-self-heal and
  a commit message.
---

# Stage, pull, push, then self-heal CI up to 3 times

Publish the current branch to remote `github` (`https://github.com/aditis1017/Aramex.git`), then watch **Aramex CI**. If that run fails, re-execute it through GitHub Actions.

`SELF_HEAL_ATTEMPTS` is **3**. Count the push-triggered run as attempt 1. Re-execute at most twice more. Do not start a 4th run.

Invoking this skill approves the commit, pull, conflict resolution, and push. Do not ask for a second confirmation. A commit message is required when there is anything to commit. If it is missing, stop and ask. Do not invent one.

Also follow the safety rules in [github-aramex-push](../github-aramex-push/SKILL.md): identity must already be set, remote `github` only, no force push, no `git config`, no `--no-verify`. On PowerShell, do not chain commands with `&&`.

## Order

1. **Prerequisites.** Inside a work tree. Current branch is not `main`, `master`, or `release/*`. Not in the middle of a merge, rebase, or cherry-pick. `git config user.name` and `user.email` are set. Remote `github` URL is `https://github.com/aditis1017/Aramex.git`.
2. **Stage all changes.**

```powershell
git add -A
```

   Then unstage credential files and leave them unstaged:

   - `.cursor/mcp.json`
   - `Profiles/default.glbl` when it contains a password
   - `.env`, `credentials.json`, `*.pem`, `*.key`

   Search the index for a literal Katalon `apiKey` value. If one is staged, unstage that file. If it is `.github/workflows/master.yml` or `.github/workflows/aramex-ci.yml`, stop. Do not print the secret. `.github/workflows/aramex-ci.yml` must use `${{ secrets.KATALON_API_KEY }}`.
3. **Commit** when the index has changes, using the user's message:

```powershell
git commit -m @"
Commit message here.

"@
```

   If the index is empty, skip the commit.
4. **Pull.** Fetch and merge the same branch from `github`. Do not rebase.

```powershell
git pull github HEAD
```

   If the remote branch does not exist yet, continue to the push. If local edits would be overwritten, commit first, then pull again.
5. **Resolve conflicts.** If the pull reports conflicts, stop the push until they are resolved:

   - List every conflicted file.
   - Remove conflict markers. Keep both sides when both changes are needed.
   - In workflow files, keep `${{ secrets.KATALON_API_KEY }}`. Never keep a literal API key.
   - `git add` each resolved file. Commit the merge. Do not use `--no-verify`.
   - If a conflict cannot be resolved without guessing, stop and name the files. Do not push.
6. **Push.**

```powershell
git push -u github HEAD
```

   If authentication fails, stop. Tell the user to sign in to GitHub with Git Credential Manager. Do not ask for a token or password.
7. **CI attempt 1 of 3.** Record the Actions run for the branch that was pushed. Poll until it completes:

```powershell
gh run watch <run-id> --repo aditis1017/Aramex --exit-status
```

   If `gh` is missing, poll `https://api.github.com/repos/aditis1017/Aramex/actions/runs/<run-id>` every 20 seconds. Do not send a token. Do not print secrets. If `conclusion` is `success`, stop.
8. **Self-heal attempts 2 and 3.** If `conclusion` is `failure` or `cancelled` and fewer than 3 runs have been used, re-execute the failed run:

```powershell
gh run rerun <run-id> --repo aditis1017/Aramex --failed
```

   Watch the new run. This may happen only for attempt 2 and attempt 3.
9. **Stop at 3.** If attempt 3 fails, do not rerun. Report the three run URLs and the failing step names. Do not change test code in this loop unless the user asked for a fix in the same request.

| Attempt | Action |
|---|---|
| 1 | Run started by the push |
| 2 | First CI/CD re-execution after failure |
| 3 | Second CI/CD re-execution after failure |
| 4 | Do not run |

## Report

- Branch, commit, and remote URL.
- Whether the pull was clean, skipped because the remote branch was new, or merged after conflict resolution. Name files that conflicted.
- Paths left unstaged because they contain credentials.
- For each attempt (1 to 3): run URL and conclusion.
- Whether CI passed on attempt 1 or after a self-heal re-run.
- If all 3 attempts failed, say the loop stopped at 3.
