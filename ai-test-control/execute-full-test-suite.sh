#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

./run-unit-tests.sh
./run-integration-tests.sh
mvn -B -ntp -Dtest='*ContractTest' -Dsurefire.failIfNoSpecifiedTests=false test
./run-pipeline-tests.sh
./run-chaos-tests.sh
