package com.versions;

import com.versions.Models.CommandResult;
import com.versions.Models.Reason;
import com.versions.Models.State;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandsTest {

    /// Build a State from a data/versions.json document.
    private static State makeState(String versionsDoc) {
        return Parser.parseVersionsJson(versionsDoc);
    }

    private static void assertOk(CommandResult result, String expectedMessage) {
        assertTrue(result.ok, () -> "expected ok with \"" + expectedMessage + "\", got ok=" + result.ok + " reason=" + result.reason);
        assertEquals(expectedMessage, result.message);
    }

    private static void assertError(CommandResult result, Reason expectedReason) {
        assertFalse(result.ok, () -> "expected error " + expectedReason + ", got ok=" + result.ok + " message=\"" + result.message + "\"");
        assertEquals(expectedReason, result.reason);
    }

    private static void assertVersionsJson(State state, String expectedJson) {
        String got = FileIo.versionsToJson(state);
        assertEquals(expectedJson, got);
    }

    /// change notes a major, minor or patch change and reports the running pending count, refuses
    /// an unknown type and a blank note, checks the type before the note, and writes nothing when
    /// it refuses.
    @Test
    void test_change_notes_each_type_and_checks_rules_in_order() {
        State state = makeState("{\n" +
                "  \"version\": \"1.4.2\",\n" +
                "  \"pending\": [],\n" +
                "  \"releases\": [\n" +
                "    {\"version\":\"1.4.2\",\"notes\":[\"fix the login crash\"],\"yanked\":false},\n" +
                "    {\"version\":\"1.4.0\",\"notes\":[\"add dark mode\",\"remember the sidebar width\"],\"yanked\":false}\n" +
                "  ]\n" +
                "}");

        CommandResult smallest = Commands.change(state, "patch", "fix the invite email");
        CommandResult middle = Commands.change(state, "minor", "export the audit log");
        CommandResult biggest = Commands.change(state, "major", "drop the v1 api");
        CommandResult unknownType = Commands.change(state, "hotfix", "reindex search");
        CommandResult blankNote = Commands.change(state, "patch", "");
        CommandResult bothWrong = Commands.change(state, "hotfix", "");

        assertOk(smallest, "Noted patch: fix the invite email (1 pending)");
        assertOk(middle, "Noted minor: export the audit log (2 pending)");
        assertOk(biggest, "Noted major: drop the v1 api (3 pending)");
        assertError(unknownType, Reason.INVALID_TYPE);
        assertError(blankNote, Reason.EMPTY_NOTE);
        assertError(bothWrong, Reason.INVALID_TYPE);

        assertVersionsJson(state, "{\n" +
                "  \"version\": \"1.4.2\",\n" +
                "  \"pending\": [\n" +
                "    {\"type\":\"patch\",\"note\":\"fix the invite email\"},\n" +
                "    {\"type\":\"minor\",\"note\":\"export the audit log\"},\n" +
                "    {\"type\":\"major\",\"note\":\"drop the v1 api\"}\n" +
                "  ],\n" +
                "  \"releases\": [\n" +
                "    {\"version\":\"1.4.2\",\"notes\":[\"fix the login crash\"],\"yanked\":false},\n" +
                "    {\"version\":\"1.4.0\",\"notes\":[\"add dark mode\",\"remember the sidebar width\"],\"yanked\":false}\n" +
                "  ]\n" +
                "}");
    }

    /// A release bumps the current version by the biggest pending type, ships the notes in the
    /// order they arrived, empties the queue, and lands at the front of the list.
    @Test
    void test_release_bumps_by_the_biggest_pending_type() {
        State state = makeState("{\n" +
                "  \"version\": \"1.4.2\",\n" +
                "  \"pending\": [\n" +
                "    {\"type\":\"patch\",\"note\":\"fix the duplicate invite email\"},\n" +
                "    {\"type\":\"minor\",\"note\":\"export the audit log\"},\n" +
                "    {\"type\":\"patch\",\"note\":\"trim the audit log header\"}\n" +
                "  ],\n" +
                "  \"releases\": [\n" +
                "    {\"version\":\"1.4.2\",\"notes\":[\"fix the login crash\"],\"yanked\":false}\n" +
                "  ]\n" +
                "}");

        CommandResult shipped = Commands.release(state);

        assertOk(shipped, "Released 1.5.0 (3 changes)");

        assertVersionsJson(state, "{\n" +
                "  \"version\": \"1.5.0\",\n" +
                "  \"pending\": [],\n" +
                "  \"releases\": [\n" +
                "    {\"version\":\"1.5.0\",\"notes\":[\"fix the duplicate invite email\",\"export the audit log\",\"trim the audit log header\"],\"yanked\":false},\n" +
                "    {\"version\":\"1.4.2\",\"notes\":[\"fix the login crash\"],\"yanked\":false}\n" +
                "  ]\n" +
                "}");
    }

    /// A release with a major change pending adds one to the first number and resets both of the
    /// others.
    @Test
    void test_release_with_a_major_change_resets_the_lower_parts() {
        State state = makeState("{\n" +
                "  \"version\": \"1.4.2\",\n" +
                "  \"pending\": [\n" +
                "    {\"type\":\"minor\",\"note\":\"add bulk invite links\"},\n" +
                "    {\"type\":\"major\",\"note\":\"drop the v1 api\"},\n" +
                "    {\"type\":\"minor\",\"note\":\"add invite link expiry\"}\n" +
                "  ],\n" +
                "  \"releases\": [\n" +
                "    {\"version\":\"1.4.2\",\"notes\":[\"fix the login crash\"],\"yanked\":false}\n" +
                "  ]\n" +
                "}");

        CommandResult shipped = Commands.release(state);

        assertOk(shipped, "Released 2.0.0 (3 changes)");

        assertVersionsJson(state, "{\n" +
                "  \"version\": \"2.0.0\",\n" +
                "  \"pending\": [],\n" +
                "  \"releases\": [\n" +
                "    {\"version\":\"2.0.0\",\"notes\":[\"add bulk invite links\",\"drop the v1 api\",\"add invite link expiry\"],\"yanked\":false},\n" +
                "    {\"version\":\"1.4.2\",\"notes\":[\"fix the login crash\"],\"yanked\":false}\n" +
                "  ]\n" +
                "}");
    }

    /// A queue of patches adds one to the third number and the new release sits ahead of the older
    /// ones, and a second release with the queue empty is refused and changes nothing.
    @Test
    void test_release_with_only_patches_bumps_the_patch() {
        State state = makeState("{\n" +
                "  \"version\": \"1.4.2\",\n" +
                "  \"pending\": [\n" +
                "    {\"type\":\"patch\",\"note\":\"fix the duplicate invite email\"},\n" +
                "    {\"type\":\"patch\",\"note\":\"handle the expired invite link\"}\n" +
                "  ],\n" +
                "  \"releases\": [\n" +
                "    {\"version\":\"1.5.0\",\"notes\":[\"export the audit log\"],\"yanked\":true},\n" +
                "    {\"version\":\"1.4.2\",\"notes\":[\"fix the login crash\"],\"yanked\":false},\n" +
                "    {\"version\":\"1.4.0\",\"notes\":[\"add dark mode\",\"remember the sidebar width\"],\"yanked\":false}\n" +
                "  ]\n" +
                "}");

        CommandResult shipped = Commands.release(state);
        CommandResult nothingLeft = Commands.release(state);

        assertOk(shipped, "Released 1.4.3 (2 changes)");
        assertError(nothingLeft, Reason.NOTHING_TO_RELEASE);

        assertVersionsJson(state, "{\n" +
                "  \"version\": \"1.4.3\",\n" +
                "  \"pending\": [],\n" +
                "  \"releases\": [\n" +
                "    {\"version\":\"1.4.3\",\"notes\":[\"fix the duplicate invite email\",\"handle the expired invite link\"],\"yanked\":false},\n" +
                "    {\"version\":\"1.5.0\",\"notes\":[\"export the audit log\"],\"yanked\":true},\n" +
                "    {\"version\":\"1.4.2\",\"notes\":[\"fix the login crash\"],\"yanked\":false},\n" +
                "    {\"version\":\"1.4.0\",\"notes\":[\"add dark mode\",\"remember the sidebar width\"],\"yanked\":false}\n" +
                "  ]\n" +
                "}");
    }

    /// yank pulls a release that is not the current one and leaves the current version alone,
    /// reports a number no release carries, answers a release that was already pulled before it
    /// looks for a survivor, and writes nothing when it refuses.
    @Test
    void test_yank_pulls_a_live_release_and_checks_rules_in_order() {
        State state = makeState("{\n" +
                "  \"version\": \"1.5.1\",\n" +
                "  \"pending\": [\n" +
                "    {\"type\":\"minor\",\"note\":\"add a per-team webhook\"}\n" +
                "  ],\n" +
                "  \"releases\": [\n" +
                "    {\"version\":\"1.5.1\",\"notes\":[\"fix the csv export header\"],\"yanked\":true},\n" +
                "    {\"version\":\"1.4.10\",\"notes\":[\"speed up the dashboard query\",\"cache the team list\"],\"yanked\":false},\n" +
                "    {\"version\":\"1.4.2\",\"notes\":[\"fix the login crash\"],\"yanked\":false}\n" +
                "  ]\n" +
                "}");

        CommandResult pulled = Commands.yank(state, "1.4.10");
        CommandResult unknown = Commands.yank(state, "2.0.1");
        CommandResult lastLive = Commands.yank(state, "1.4.2");
        CommandResult pulledAgain = Commands.yank(state, "1.5.1");

        assertOk(pulled, "Yanked 1.4.10; current version 1.5.1");
        assertError(unknown, Reason.VERSION_NOT_FOUND);
        assertOk(lastLive, "Yanked 1.4.2; current version 1.5.1");
        assertError(pulledAgain, Reason.ALREADY_YANKED);

        assertVersionsJson(state, "{\n" +
                "  \"version\": \"1.5.1\",\n" +
                "  \"pending\": [\n" +
                "    {\"type\":\"minor\",\"note\":\"add a per-team webhook\"}\n" +
                "  ],\n" +
                "  \"releases\": [\n" +
                "    {\"version\":\"1.5.1\",\"notes\":[\"fix the csv export header\"],\"yanked\":true},\n" +
                "    {\"version\":\"1.4.10\",\"notes\":[\"speed up the dashboard query\",\"cache the team list\"],\"yanked\":true},\n" +
                "    {\"version\":\"1.4.2\",\"notes\":[\"fix the login crash\"],\"yanked\":true}\n" +
                "  ]\n" +
                "}");
    }

    /// Pulling the current version moves it to the newest release that is not yanked, repeated
    /// pulls walk down the list one at a time, and the last release still standing cannot be
    /// pulled.
    @Test
    void test_yank_the_current_version_falls_back_to_the_newest_survivor() {
        State state = makeState("{\n" +
                "  \"version\": \"1.5.2\",\n" +
                "  \"pending\": [\n" +
                "    {\"type\":\"patch\",\"note\":\"stop the retry loop on a 404\"}\n" +
                "  ],\n" +
                "  \"releases\": [\n" +
                "    {\"version\":\"1.5.2\",\"notes\":[\"retry failed uploads\"],\"yanked\":false},\n" +
                "    {\"version\":\"1.5.1\",\"notes\":[\"fix the csv export header\"],\"yanked\":true},\n" +
                "    {\"version\":\"1.4.10\",\"notes\":[\"cache the team list\"],\"yanked\":false},\n" +
                "    {\"version\":\"1.4.2\",\"notes\":[\"fix the login crash\"],\"yanked\":false},\n" +
                "    {\"version\":\"1.4.1\",\"notes\":[\"restore the reset password link\"],\"yanked\":true}\n" +
                "  ]\n" +
                "}");

        CommandResult firstPull = Commands.yank(state, "1.5.2");
        CommandResult secondPull = Commands.yank(state, "1.4.10");
        CommandResult lastStanding = Commands.yank(state, "1.4.2");

        assertOk(firstPull, "Yanked 1.5.2; current version 1.4.10");
        assertOk(secondPull, "Yanked 1.4.10; current version 1.4.2");
        assertError(lastStanding, Reason.NO_OTHER_VERSION);

        assertVersionsJson(state, "{\n" +
                "  \"version\": \"1.4.2\",\n" +
                "  \"pending\": [\n" +
                "    {\"type\":\"patch\",\"note\":\"stop the retry loop on a 404\"}\n" +
                "  ],\n" +
                "  \"releases\": [\n" +
                "    {\"version\":\"1.5.2\",\"notes\":[\"retry failed uploads\"],\"yanked\":true},\n" +
                "    {\"version\":\"1.5.1\",\"notes\":[\"fix the csv export header\"],\"yanked\":true},\n" +
                "    {\"version\":\"1.4.10\",\"notes\":[\"cache the team list\"],\"yanked\":true},\n" +
                "    {\"version\":\"1.4.2\",\"notes\":[\"fix the login crash\"],\"yanked\":false},\n" +
                "    {\"version\":\"1.4.1\",\"notes\":[\"restore the reset password link\"],\"yanked\":true}\n" +
                "  ]\n" +
                "}");
    }
}
