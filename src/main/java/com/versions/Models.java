package com.versions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Models {
    private Models() {}

    public static class Version {
        public int major = 0;
        public int minor = 0;
        public int patch = 0;

        public Version() {}

        public Version(int major, int minor, int patch) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
        }

        @Override
        public String toString() {
            return major + "." + minor + "." + patch;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Version version = (Version) o;
            return major == version.major && minor == version.minor && patch == version.patch;
        }

        @Override
        public int hashCode() {
            return Objects.hash(major, minor, patch);
        }
    }

    public enum ChangeKind {
        MAJOR,
        MINOR,
        PATCH;

        @Override
        public String toString() {
            return name().toLowerCase();
        }

        public static ChangeKind parseChangeKind(String text) {
            if ("major".equals(text)) return MAJOR;
            if ("minor".equals(text)) return MINOR;
            if ("patch".equals(text)) return PATCH;
            throw new RuntimeException("unknown change type: " + text);
        }
    }

    public static class Change {
        public ChangeKind type = ChangeKind.PATCH;
        public String note = "";

        public Change() {}

        public Change(ChangeKind type, String note) {
            this.type = type;
            this.note = note;
        }
    }

    public static class Release {
        public Version version = new Version();
        public List<String> notes = new ArrayList<>();
        public boolean yanked = false;

        public Release() {}

        public Release(Version version, List<String> notes, boolean yanked) {
            this.version = version;
            this.notes = notes != null ? notes : new ArrayList<>();
            this.yanked = yanked;
        }
    }

    public static class State {
        public Version version = new Version();
        public List<Change> pending = new ArrayList<>();
        public List<Release> releases = new ArrayList<>();

        public State() {}
    }

    public enum Reason {
        NONE,
        INVALID_TYPE,
        EMPTY_NOTE,
        NOTHING_TO_RELEASE,
        VERSION_NOT_FOUND,
        ALREADY_YANKED,
        NO_OTHER_VERSION;

        @Override
        public String toString() {
            return this == NONE ? "" : name();
        }
    }

    public static class CommandResult {
        public boolean ok = false;
        public String message = "";
        public Reason reason = Reason.NONE;

        public CommandResult() {}
    }
}
