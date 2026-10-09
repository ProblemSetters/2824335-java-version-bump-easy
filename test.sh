#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Compile first to ensure tests run against latest source.
mvn -q -DskipTests compile 2>&1 || true

# Remove stale surefire report so a failed build cannot reuse old results.
surefire_xml="target/surefire-reports/TEST-com.versions.CommandsTest.xml"
rm -f "$surefire_xml"

# Run Maven tests (allow failure so we can parse results).
mvn -q test 2>&1 || true

if [[ -f "$surefire_xml" ]]; then
    cp "$surefire_xml" junit.xml
else
    # Fallback: generate a minimal junit.xml indicating all tests failed.
    cat > junit.xml <<'XMLEOF'
<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="com.versions.CommandsTest" tests="6" failures="6">
  <testcase name="test_change_notes_each_type_and_checks_rules_in_order"><failure message="build failed" /></testcase>
  <testcase name="test_release_bumps_by_the_biggest_pending_type"><failure message="build failed" /></testcase>
  <testcase name="test_release_with_a_major_change_resets_the_lower_parts"><failure message="build failed" /></testcase>
  <testcase name="test_release_with_only_patches_bumps_the_patch"><failure message="build failed" /></testcase>
  <testcase name="test_yank_pulls_a_live_release_and_checks_rules_in_order"><failure message="build failed" /></testcase>
  <testcase name="test_yank_the_current_version_falls_back_to_the_newest_survivor"><failure message="build failed" /></testcase>
</testsuite>
XMLEOF
fi

# Print colored terminal output from junit.xml.
python3 - <<'PY' 2>/dev/null || true
import xml.etree.ElementTree as ET, sys

try:
    tree = ET.parse("junit.xml")
except Exception:
    print("\033[1;31mCould not parse junit.xml\033[0m", file=sys.stderr)
    sys.exit(0)

root = tree.getroot()
suites = [root] if root.tag == "testsuite" else root.findall(".//testsuite")

total = 0
failures = 0

for suite in suites:
    for tc in suite.findall("testcase"):
        name = tc.get("name", "unknown")
        total += 1
        fail = tc.find("failure")
        err = tc.find("error")
        problem = fail if fail is not None else err
        if problem is not None:
            failures += 1
            print(f"\033[1;31mFAIL: {name}\033[0m", file=sys.stderr)
            msg = problem.get("message", "") or problem.text or ""
            for line in msg.splitlines():
                stripped = line.strip()
                if stripped:
                    print(f"       {stripped}", file=sys.stderr)
        else:
            print(f"\033[32mPASS: {name}\033[0m", file=sys.stderr)

if failures > 0:
    print(f"\n\033[1;31m{failures} test(s) failed.\033[0m", file=sys.stderr)
else:
    print(f"\n\033[32mAll {total} tests passed!\033[0m", file=sys.stderr)
PY
