#!/usr/bin/env bash
# 바뀐 파일(HEAD 대비 변경 + untracked)이 속한 모듈의 단위 테스트만 실행합니다.
#   JVM 모듈(nextbill.jvm.library)    -> :x:test
#   Android 모듈                       -> :x:testDebugUnitTest
#   build-logic/                       -> ./gradlew -p build-logic test
# 종료 코드 (Claude Code Stop hook 규약):
#   0  바뀐 모듈이 없거나 테스트가 통과함, 또는 이미 Stop hook으로 이어진 턴(stop_hook_active=true)
#   2  테스트 실패. stderr가 Claude에게 전달되고 Claude는 멈추지 않고 실패를 다룹니다.
# stop_hook_active=true이면 테스트를 다시 돌리지 않고 0으로 끝내, 실패가 계속될 때 무한 반복을 막습니다.
set -uo pipefail

# Stop hook은 stdin으로 JSON을 넘깁니다. 터미널에서 직접 실행하면 stdin을 읽지 않습니다.
if [ ! -t 0 ]; then
  hook_input=$(cat)
  if printf '%s' "$hook_input" | grep -Eq '"stop_hook_active"[[:space:]]*:[[:space:]]*true'; then
    exit 0
  fi
fi

cd "$(git rev-parse --show-toplevel)" || exit 0

changed_files() {
  if git rev-parse --verify --quiet HEAD >/dev/null; then
    git diff --name-only HEAD
  else
    git diff --name-only --cached
  fi
  git ls-files --others --exclude-standard
}

# 파일 경로에서 가장 가까운 build.gradle.kts가 있는 디렉터리를 찾아 Gradle 경로로 바꿉니다.
module_task() {
  local dir
  dir=$(dirname "$1")
  while [ "$dir" != "." ] && [ "$dir" != "/" ]; do
    if [ -f "$dir/build.gradle.kts" ]; then
      local path=":${dir//\//:}"
      if grep -q 'nextbill\.jvm\.library' "$dir/build.gradle.kts"; then
        echo "$path:test"
      else
        echo "$path:testDebugUnitTest"
      fi
      return
    fi
    dir=$(dirname "$dir")
  done
}

build_logic_changed=false
tasks=""
while IFS= read -r file; do
  [ -z "$file" ] && continue
  case "$file" in
    build-logic/*) build_logic_changed=true ;;
    *) tasks="$tasks$(module_task "$file")"$'\n' ;;
  esac
done < <(changed_files | sort -u)

tasks=$(printf '%s' "$tasks" | sed '/^$/d' | sort -u | tr '\n' ' ')

status=0
if [ "$build_logic_changed" = true ]; then
  ./gradlew -q -p build-logic test || status=$?
fi
if [ -n "$tasks" ]; then
  # shellcheck disable=SC2086
  ./gradlew -q $tasks || status=$?
fi

if [ "$status" -ne 0 ]; then
  echo "test-changed.sh: tests failed (${tasks}build-logic=${build_logic_changed})" >&2
  exit 2
fi
exit 0
