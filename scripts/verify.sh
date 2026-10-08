#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd -- "$project_dir"

export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$project_dir/.tooling/gradle-home}"
./gradlew build "$@"
