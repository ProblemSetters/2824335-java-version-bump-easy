# C++: Version Bump

Every change waiting for the next release is recorded as major, minor, or patch. When a release is made, the biggest pending change decides which part of the version number goes up, so the number tells users how risky the upgrade is. A release can also be withdrawn after it is made. If the wrong part of the number goes up, a release leaves its changes pending, or a withdrawn version stays the current one, users install a version that is riskier than its number says or that should no longer be used.

Your task is to fix the version bump tool so that every command updates the data correctly and prints the correct success message or error reason.

## State File

**File: `data/versions.json`**

```json
{
  "version": "1.5.2",
  "pending": [
    {"type":"patch","note":"fix the duplicate invite email"},
    {"type":"minor","note":"export the audit log"}
  ],
  "releases": [
    {
      "version": "1.5.2",
      "notes": ["retry failed uploads","tighten the session timeout"],
      "yanked": false
    },
    {
      "version": "1.5.1",
      "notes": ["fix the csv export header"],
      "yanked": true
    }
  ]
}
```

Fields:

- `version` (string): the current version, `major.minor.patch`.
- `pending` (list): changes waiting for the next release, in the order noted.
  - `type` (string): `major`, `minor`, or `patch`.
  - `note` (string): what changed.
- `releases` (list): every release, newest first.
  - `version` (string): the version number.
  - `notes` (list of strings): the notes it shipped with.
  - `yanked` (boolean): whether the release was pulled.

A note with spaces is quoted on the command line. When writing `data/versions.json` back, preserve the field order shown above, and keep pending changes and releases in the order they appear in the file.

## Command Output

Each command prints a one-line result on success, or `Error: <REASON>` on failure. Its rules are checked in the order listed, stopping at the first failure, and on failure `data/versions.json` is not modified.

## Commands

### `change`

Records a change for the next release.

**Usage**

```bash
./versions change <major|minor|patch> <note>
```

**Output**

```text
Noted <type>: <note> (<n> pending)
```

**Rules**

Check these rules in order and stop at the first failure:

- If the type is not `major`, `minor`, or `patch`, fail with `INVALID_TYPE`.
- If the note is empty, fail with `EMPTY_NOTE`.

On success, append the change.

### `release`

Creates a new version from the pending changes.

**Usage**

```bash
./versions release
```

**Output**

```text
Released <version> (<n> changes)
```

**Rules**

- If nothing is pending, fail with `NOTHING_TO_RELEASE`.

On success:

- Bump the current version by the biggest pending type: `major` adds one to the first number and resets the other two to 0, `minor` adds one to the second and resets the third, `patch` adds one to the third.
- Insert the release at the front with the pending notes.
- Clear `pending` and set `version`.

### `yank`

Withdraws a release.

**Usage**

```bash
./versions yank <version>
```

**Output**

```text
Yanked <version>; current version <version>
```

**Rules**

Check these rules in order and stop at the first failure:

- If no release has that number, fail with `VERSION_NOT_FOUND`.
- If it is already yanked, fail with `ALREADY_YANKED`.
- If it is the current version and no other release is unyanked, fail with `NO_OTHER_VERSION`.

On success:

- Mark it yanked.
- When it was the current version, the current version becomes the newest release that is not yanked.

> **Note:** Refer to `README.md` for more details and examples.
