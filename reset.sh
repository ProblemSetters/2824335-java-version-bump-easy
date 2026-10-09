#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cat > "$SCRIPT_DIR/data/versions.json" <<'EOF'
{
  "version": "1.5.2",
  "pending": [
    {"type":"patch","note":"fix the duplicate invite email"},
    {"type":"minor","note":"export the audit log"},
    {"type":"major","note":"drop the v1 api"},
    {"type":"patch","note":"stop the retry loop on a 404"},
    {"type":"minor","note":"add a per-team webhook"},
    {"type":"patch","note":"handle the expired invite link"}
  ],
  "releases": [
    {"version":"1.5.2","notes":["retry failed uploads","tighten the session timeout"],"yanked":false},
    {"version":"1.5.1","notes":["fix the csv export header"],"yanked":true},
    {"version":"1.4.10","notes":["speed up the dashboard query","cache the team list","log slow requests"],"yanked":false},
    {"version":"1.4.2","notes":["fix the login crash"],"yanked":false},
    {"version":"1.4.1","notes":["restore the reset password link"],"yanked":true},
    {"version":"1.4.0","notes":["add dark mode","remember the sidebar width"],"yanked":false},
    {"version":"1.3.2","notes":["fix the avatar upload"],"yanked":false},
    {"version":"1.3.1","notes":["show release notes in the app"],"yanked":false},
    {"version":"1.2.3","notes":["fix the invite email link"],"yanked":true},
    {"version":"1.2.1","notes":[],"yanked":false}
  ]
}
EOF

echo "Restored data/versions.json to its initial state"
