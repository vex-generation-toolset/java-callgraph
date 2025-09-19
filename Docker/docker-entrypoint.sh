#!/bin/sh
#
# Copyright (C) 2025-present OpenRefactory, Inc.
#
# SPDX-License-Identifier: Apache-2.0
#
set -e

# Switch to workspace
cd /workspace

# Start the app under Xvfb
/usr/bin/xvfb-run \
    --auto-servernum \
    --server-args="-screen 0 1024x768x24" -- \
    /usr/local/callgraph/callgraph \
    -data /workspace \
    "$@"
