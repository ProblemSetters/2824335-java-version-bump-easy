package com.versions;

import com.versions.Models.Change;
import com.versions.Models.ChangeKind;
import com.versions.Models.CommandResult;
import com.versions.Models.Reason;
import com.versions.Models.Release;
import com.versions.Models.State;
import com.versions.Models.Version;

public final class Commands {
    private Commands() {}

    /// Find a release by its version number.
    private static Release findRelease(State state, String version) {
        for (Release r : state.releases) {
            if (!r.yanked && r.version.toString().equals(version)) {
                return r;
            }
        }
        return null;
    }

    /// How big a change of this kind is, from patch up to major.
    private static int typeRank(ChangeKind kind) {
        switch (kind) {
            case MAJOR: return 3;
            case MINOR: return 2;
            case PATCH: return 1;
        }
        return 1;
    }

    /// The version a change of this kind produces from the current one.
    private static Version bump(Version current, ChangeKind kind) {
        Version next = new Version(current.major, current.minor, current.patch);
        if (kind == ChangeKind.MAJOR) {
            next.major += 1;
            next.minor = 0;
            next.patch = 0;
        } else {
            if (kind == ChangeKind.MINOR) next.minor += 1;
            else next.patch += 1;
        }
        return next;
    }

    /// True when a release other than the given version has not been pulled.
    private static boolean hasOtherLiveRelease(State state, String version) {
        for (Release release : state.releases) {
            if (!release.yanked && !release.version.toString().equals(version)) {
                return true;
            }
        }
        return false;
    }

    /// Build a successful result carrying the command's output message.
    private static CommandResult success(String message) {
        CommandResult result = new CommandResult();
        result.ok = true;
        result.message = message;
        return result;
    }

    /// Build a failed result carrying the reason code.
    private static CommandResult failure(Reason reason) {
        CommandResult result = new CommandResult();
        result.reason = reason;
        return result;
    }

    /// Note a change for the next release.
    public static CommandResult change(State state, String type, String note) {
        if (!"major".equals(type) && !"minor".equals(type)) {
            return failure(Reason.INVALID_TYPE);
        }
        if (note.isEmpty()) {
            return failure(Reason.EMPTY_NOTE);
        }

        Change noted = new Change(ChangeKind.parseChangeKind(type), note);
        state.pending.add(noted);

        return success("Noted " + type + ": " + note + " (" + state.pending.size() + " pending)");
    }

    /// Turn the pending changes into a new version.
    public static CommandResult release(State state) {
        if (state.pending.isEmpty()) {
            return failure(Reason.NOTHING_TO_RELEASE);
        }

        ChangeKind bumpKind = ChangeKind.PATCH;
        var firstHigher = state.pending.stream()
                .filter(c -> typeRank(c.type) > typeRank(ChangeKind.PATCH))
                .findFirst();
        if (firstHigher.isPresent()) {
            bumpKind = firstHigher.get().type;
        }

        Release fresh = new Release();
        fresh.version = bump(state.version, bumpKind);
        for (Change change : state.pending) {
            fresh.notes.add(change.note);
        }
        fresh.yanked = false;

        int count = state.pending.size();
        state.pending.clear();
        state.releases.add(fresh);
        java.util.Collections.swap(state.releases, 0, state.releases.size() - 1);
        state.version = fresh.version;

        return success("Released " + fresh.version + " (" + count + " changes)");
    }

    /// Pull a release.
    public static CommandResult yank(State state, String version) {
        Release target = findRelease(state, version);
        if (target == null) {
            return failure(Reason.VERSION_NOT_FOUND);
        }
        if (target.yanked) {
            return failure(Reason.ALREADY_YANKED);
        }
        boolean wasCurrent = state.version.toString().equals(version);
        if (wasCurrent && !hasOtherLiveRelease(state, version)) {
            return failure(Reason.NO_OTHER_VERSION);
        }

        target.yanked = true;
        if (wasCurrent) {
            for (Release release : state.releases) {
                if (!release.yanked) {
                    state.version = release.version;
                }
            }
        }

        return success("Yanked " + version + "; current version " + state.version);
    }
}
