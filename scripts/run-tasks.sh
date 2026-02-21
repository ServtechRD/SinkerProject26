#!/usr/bin/env bash
set -euo pipefail

# ================== Config ==================
TASK_ROOT="${TASK_ROOT:-spec/task}"
BASE_BRANCH="${BASE_BRANCH:-claude/intergration}"
REMOTE="${REMOTE:-origin}"

# Branch naming
BRANCH_PREFIX="${BRANCH_PREFIX:-claude/feat/}"

# Claude Code
MODEL="${MODEL:-claude-sonnet-4-5-20250929}"
MAX_TURNS="${MAX_TURNS:-80}"

# PR / automerge label
AUTO_MERGE_LABEL="${AUTO_MERGE_LABEL:-automerge}"

# Behavior
WAIT_FOR_MERGE="${WAIT_FOR_MERGE:-true}"  # true=等 GitHub Actions merge 後才跑下一個
SLEEP_SECS="${SLEEP_SECS:-20}"

# Safety
NO_FORCE_PUSH="${NO_FORCE_PUSH:-true}"
# ===========================================

require() { command -v "$1" >/dev/null || { echo "❌ missing: $1"; exit 1; }; }
require git
require gh
require claude

if [[ ! -d "$TASK_ROOT" ]]; then
  echo "❌ TASK_ROOT not found: $TASK_ROOT"
  exit 1
fi

if ! gh auth status >/dev/null 2>&1; then
  echo "❌ gh is not authenticated. Run: gh auth login"
  exit 1
fi

# ---- helpers ----
find_next_todo() {
  find "$TASK_ROOT" -type f -name "status.todo" | sort | head -n 1 || true
}

task_id_from_dir() {
  basename "$1"
}

task_slug_from_id() {
  # task folder name is already slugged, e.g. T017-backend-forecast-query-version
  echo "$1"
}

ensure_clean_worktree() {
  if ! git diff --quiet || ! git diff --cached --quiet; then
    echo "❌ Working tree not clean. Please commit/stash first."
    exit 1
  fi
}

get_pr_url_by_branch() {
  local branch="$1"
  if gh pr view --head "$branch" >/dev/null 2>&1; then
    gh pr view --head "$branch" --json url -q .url
  else
    echo ""
  fi
}

get_pr_state() {
  local pr_url="$1"
  gh pr view "$pr_url" --json state -q .state
}

wait_for_pr_merged() {
  local pr_url="$1"
  echo "⏳ Waiting for PR to be MERGED by GitHub Actions: $pr_url"
  while true; do
    local state
    state="$(get_pr_state "$pr_url")"
    case "$state" in
      MERGED)
        echo "✅ PR merged: $pr_url"
        return 0
        ;;
      CLOSED)
        echo "❌ PR closed without merge: $pr_url"
        return 2
        ;;
      OPEN)
        # optional: show checks summary
        echo "⏳ PR still OPEN. Sleeping ${SLEEP_SECS}s..."
        sleep "$SLEEP_SECS"
        ;;
      *)
        echo "⚠️ Unknown PR state: $state. Sleeping ${SLEEP_SECS}s..."
        sleep "$SLEEP_SECS"
        ;;
    esac
  done
}

create_or_update_pr() {
  local task_id="$1"
  local branch="$2"

  local pr_url
  pr_url="$(get_pr_url_by_branch "$branch")"

  if [[ -z "$pr_url" ]]; then
    local title="[${task_id}] feat: $(task_slug_from_id "$task_id")"
    pr_url="$(gh pr create \
      --base "$BASE_BRANCH" \
      --head "$branch" \
      --title "$title" \
      --body "Automated PR for ${task_id}.

- CI: make test-compose (GitHub Actions)
- Auto-merge: label '${AUTO_MERGE_LABEL}'
" \
      --json url -q .url)"
    echo "✅ PR created: $pr_url"
  else
    echo "ℹ️ PR already exists: $pr_url"
  fi

  # add automerge label
  gh pr edit "$pr_url" --add-label "$AUTO_MERGE_LABEL" >/dev/null || true
  echo "🏷️ Added/ensured label: $AUTO_MERGE_LABEL"

  echo "$pr_url"
}

# ============= main loop =============
echo "============================================================"
echo "Runner config:"
echo "- TASK_ROOT=$TASK_ROOT"
echo "- BASE_BRANCH=$BASE_BRANCH"
echo "- BRANCH_PREFIX=$BRANCH_PREFIX"
echo "- MODEL=$MODEL MAX_TURNS=$MAX_TURNS"
echo "- AUTO_MERGE_LABEL=$AUTO_MERGE_LABEL"
echo "- WAIT_FOR_MERGE=$WAIT_FOR_MERGE"
echo "============================================================"

while true; do
  todo_file="$(find_next_todo)"
  if [[ -z "${todo_file:-}" ]]; then
    echo "🎉 No pending tasks (no status.todo found)."
    exit 0
  fi

  task_dir="$(dirname "$todo_file")"
  task_id="$(task_id_from_dir "$task_dir")"

  desc="$task_dir/description.md"
  acc="$task_dir/acceptance.md"

  if [[ ! -f "$desc" || ! -f "$acc" ]]; then
    echo "❌ Missing description.md or acceptance.md in: $task_dir"
    exit 1
  fi

  echo "============================================================"
  echo "🚀 Starting task: $task_id"
  echo "📁 $task_dir"
  echo "============================================================"

  ensure_clean_worktree

  # Sync base branch
  git checkout "$BASE_BRANCH"
  git pull --rebase "$REMOTE" "$BASE_BRANCH"

  # Move status.todo -> status.doing on base branch first (so PR includes it)
  mv "$task_dir/status.todo" "$task_dir/status.doing"
  git add "$task_dir/status.doing"
  git commit -m "chore(${task_id}): mark doing"

  # Create branch
  branch="${BRANCH_PREFIX}${task_id}"
  git checkout -b "$branch"

  # Docker env up (best-effort; no tests here)
  docker compose ps >/dev/null 2>&1 || true
  make dev-up >/dev/null 2>&1 || true

  # Run Claude Code to implement (no local tests; CI will run make test-compose)
  prompt=$(cat <<EOF
You are Claude Code in this repo.

Task folder: ${task_dir}
Read:
- ${desc}
- ${acc}
- agent.md
- skill.md

Hard rules:
- Docker-first (never run gradle/npm on host)
- Do NOT merge PR (GitHub Actions auto-merge)
- Do NOT run tests locally (CI runs make test-compose)
- Implement only this task, minimal diff
- Add/adjust tests as required
- Use Conventional Commits (feat/fix/chore/test/refactor)

When done:
- Ensure changes are committed on this branch.

Start now.
EOF
)

  claude -p \
    --model "$MODEL" \
    --max-turns "$MAX_TURNS" \
    --append-system-prompt "$prompt"

  # Safety: never force push
  if [[ "$NO_FORCE_PUSH" == "true" ]]; then
    # guard: if remote branch exists and is not fast-forward, fail
    if git ls-remote --exit-code --heads "$REMOTE" "$branch" >/dev/null 2>&1; then
      # branch exists on remote; ensure we can fast-forward
      git fetch "$REMOTE" "$branch":"refs/remotes/$REMOTE/$branch" || true
      if ! git merge-base --is-ancestor "refs/remotes/$REMOTE/$branch" HEAD; then
        echo "❌ Remote branch exists and would require non-fast-forward push. Aborting (NO_FORCE_PUSH=true)."
        exit 1
      fi
    fi
  fi

  # If Claude didn't commit, create a fallback commit
  if ! git diff --quiet || ! git diff --cached --quiet; then
    git add -A
    if ! git diff --cached --quiet; then
      git commit -m "feat(${task_id}): implement ${task_id}"
    fi
  fi

  # Mark done inside the PR (done means "implemented & PR ready"; merge handled by Actions)
  if [[ -f "$task_dir/status.doing" ]]; then
    mv "$task_dir/status.doing" "$task_dir/status.done"
    git add "$task_dir/status.done"
    git commit -m "chore(${task_id}): mark done"
  fi

  # Push branch
  git push -u "$REMOTE" "$branch"

  # Create PR + label automerge
  pr_url="$(create_or_update_pr "$task_id" "$branch")"

  if [[ "$WAIT_FOR_MERGE" == "true" ]]; then
    wait_for_pr_merged "$pr_url"
    # After merge, sync base and proceed
    git checkout "$BASE_BRANCH"
    git pull --rebase "$REMOTE" "$BASE_BRANCH"
  else
    echo "✅ PR opened (not waiting for merge): $pr_url"
    echo "➡️ Exiting because WAIT_FOR_MERGE=false"
    exit 0
  fi
done
