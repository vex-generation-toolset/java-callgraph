#!/usr/bin/env bash
set -euo pipefail

# Switch to workspace
cd /workspace

# Start the app under Xvfb
/usr/bin/xvfb-run \
    --auto-servernum \
    --server-args="-screen 0 1024x768x24" -- \
    /usr/local/callgraph/callgraph \
    -data /workspace \
    "$@"
