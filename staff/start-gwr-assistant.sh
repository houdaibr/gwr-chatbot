#!/bin/sh
# Starts the GWR assistant (Claude Code in this folder).
# GWR_API_BASE_URL: set it before starting to use another GWR server; otherwise the local test server is used.
if [ -z "$GWR_API_BASE_URL" ]; then
  GWR_API_BASE_URL="http://localhost:8080/gwr-api"
fi
export GWR_API_BASE_URL
cd "$(dirname "$0")" || exit 1
exec claude
