#!/usr/bin/env bash
set -euo pipefail

TASK_ROOT="${TASK_ROOT:-spec/task}"
BRANCH_PREFIX="${BRANCH_PREFIX:-claude/feat/}"
SHOW_CHECKS="${SHOW_CHECKS:-true}"

require() { command -v "$1" >/dev/null || { echo "❌ missing: $1"; exit 1; }; }
require gh

if [[ ! -d "$TASK_ROOT" ]]; then
  echo "❌ TASK_ROOT not found: $TASK_ROOT"
  exit 1
fi

if ! gh auth status >/dev/null 2>&1; then
  echo "❌ gh is not authenticated. Run: gh auth login"
  exit 1
fi

status_of_task() {
  local dir="$1"
  if [[ -f "$dir/status.todo" ]]; then echo "todo"; return; fi
  if [[ -f "$dir/status.doing" ]]; then echo "doing"; return; fi
  if [[ -f "$dir/status.done" ]]; then echo "done"; return; fi
  echo "unknown"
}

pr_url_for_task() {
  local task_id="$1"
  local branch="${BRANCH_PREFIX}${task_id}"
  if gh pr view --head "$branch" >/dev/null 2>&1; then
    gh pr view --head "$branch" --json url -q .url
  else
    echo ""
  fi
}

pr_state() {
  local pr_url="$1"
  gh pr view "$pr_url" --json state -q .state
}

checks_summary() {
  local pr_url="$1"
  # prints concise checks list; may exit non-zero if none
  gh pr checks "$pr_url" 2>/dev/null || true
}

echo "============================================================"
echo "Task Status Report"
echo "- TASK_ROOT=$TASK_ROOT"
echo "- BRANCH_PREFIX=$BRANCH_PREFIX"
echo "============================================================"

mapfile -t TASK_DIRS < <(find "$TASK_ROOT" -mindepth 1 -maxdepth 1 -type d | sort)

if [[ ${#TASK_DIRS[@]} -eq 0 ]]; then
  echo "No task folders found."
  exit 0
fi

todo_count=0
doing_count=0
done_count=0

for dir in "${TASK_DIRS[@]}"; do
  task_id="$(basename "$dir")"
  st="$(status_of_task "$dir")"

  case "$st" in
    todo) ((todo_count+=1)) ;;
    doing) ((doing_count+=1)) ;;
    done) ((done_count+=1)) ;;
  esac

  pr_url="$(pr_url_for_task "$task_id")"
  if [[ -n "$pr_url" ]]; then
    state="$(pr_state "$pr_url")"
    echo "- ${task_id}  [${st}]  PR=${state}  ${pr_url}"
    if [[ "$SHOW_CHECKS" == "true" && "$state" == "OPEN" ]]; then
      echo "  checks:"
      checks_summary "$pr_url" | sed 's/^/    /'
    fi
  else
    echo "- ${task_id}  [${st}]  PR=none"
  fi
done

echo "------------------------------------------------------------"
echo "Summary: todo=${todo_count}, doing=${doing_count}, done=${done_count}"
echo "Tip: set SHOW_CHECKS=false to hide checks."
