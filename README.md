# Java: Version Bump

Your task is to fix the version bump tool so that every command updates the data correctly and prints the correct success message or error reason. It reads the state from `data/versions.json`, processes the `change`, `release` and `yank` commands, and writes the updated state back.

## Project Structure

```text
├── data/
│   └── versions.json           # Persistent release state (version, pending changes, releases)
├── src/
│   ├── main/java/com/versions/
│   │   ├── Commands.java       # Command handlers
│   │   ├── FileIo.java         # State file load and save
│   │   ├── Main.java           # CLI entry point
│   │   ├── Models.java         # Data models
│   │   └── Parser.java         # JSON parsing
│   └── test/java/com/versions/
│       └── CommandsTest.java   # Unit tests
├── versions                    # CLI launcher script
├── pom.xml                     # Maven project configuration
├── run.sh                      # Setup and run script
├── reset.sh                    # Restore data/versions.json to its initial state
├── test.sh                     # Test script (writes junit.xml)
└── README.md
```

## Commands

### Install

```bash
mvn -B -ntp -DskipTests package
```

### Run

```bash
bash run.sh
```

### Test

```bash
bash test.sh
```

### Reset

Restore `data/versions.json` to its initial state.

```bash
bash reset.sh
```

## Examples

Each example below runs independently against the initial `data/versions.json` and shows only the records it changes.

### Example 1: A change is noted for the next release

Command:

```bash
./versions change minor "add saml login"
```

Output:

```text
Noted minor: add saml login (7 pending)
```

Changed records in `data/versions.json`:

```json
{"type":"minor","note":"add saml login"}
```

### Example 2: A type the tool does not record is rejected

Command:

```bash
./versions change hotfix "urgent fix"
```

Output:

```text
Error: INVALID_TYPE
```

`data/versions.json`: unchanged.

### Example 3: A change with no note is rejected

Command:

```bash
./versions change minor ""
```

Output:

```text
Error: EMPTY_NOTE
```

`data/versions.json`: unchanged.

### Example 4: The biggest pending change decides the next version

Command:

```bash
./versions release
```

Output:

```text
Released 2.0.0 (6 changes)
```

Changed records in `data/versions.json`:

```json
"version": "2.0.0"
"pending": []
{"version":"2.0.0","notes":["fix the duplicate invite email","export the audit log","drop the v1 api","stop the retry loop on a 404","add a per-team webhook","handle the expired invite link"],"yanked":false}
```

### Example 5: Pulling a release that is not the current one leaves the version alone

Command:

```bash
./versions yank 1.4.10
```

Output:

```text
Yanked 1.4.10; current version 1.5.2
```

Changed records in `data/versions.json`:

```json
{"version":"1.4.10","notes":["speed up the dashboard query","cache the team list","log slow requests"],"yanked":true}
```
