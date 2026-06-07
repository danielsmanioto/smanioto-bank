#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"

exec "$ROOT/services/data-lake/run_job.sh" "$@"
