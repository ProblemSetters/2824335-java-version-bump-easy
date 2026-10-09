package com.versions;

import com.versions.Models.Change;
import com.versions.Models.Release;
import com.versions.Models.State;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

public final class FileIo {
    private FileIo() {}

    /// Escape a string for safe inclusion inside JSON double quotes.
    public static String escapeJsonString(String value) {
        if (value == null) return "";
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"':  escaped.append("\\\""); break;
                case '\\': escaped.append("\\\\"); break;
                case '\n': escaped.append("\\n");  break;
                case '\r': escaped.append("\\r");  break;
                case '\t': escaped.append("\\t");  break;
                default:   escaped.append(ch);
            }
        }
        return escaped.toString();
    }

    /// Format the notes of a release as a single-line JSON array of strings.
    public static String notesToJson(List<String> notes) {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        for (int i = 0; i < notes.size(); i++) {
            if (i > 0) builder.append(',');
            builder.append('"').append(escapeJsonString(notes.get(i))).append('"');
        }
        builder.append(']');
        return builder.toString();
    }

    /// Format a single pending change as a single-line JSON object.
    public static String changeToJson(Change change) {
        return "{\"type\":\"" + change.type + "\",\"note\":\"" + escapeJsonString(change.note) + "\"}";
    }

    /// Format a single release as a single-line JSON object.
    public static String releaseToJson(Release release) {
        return "{\"version\":\"" + release.version + "\",\"notes\":" + notesToJson(release.notes) +
               ",\"yanked\":" + (release.yanked ? "true" : "false") + "}";
    }

    /// Format a JSON array of objects, one object per indented line.
    private static <T> void appendArray(StringBuilder builder, List<T> items, Function<T, String> toJson) {
        builder.append('[');
        for (int i = 0; i < items.size(); i++) {
            builder.append(i == 0 ? "\n    " : ",\n    ");
            builder.append(toJson.apply(items.get(i)));
        }
        if (!items.isEmpty()) {
            builder.append("\n  ");
        }
        builder.append(']');
    }

    /// Read a text file as a single string.
    public static String readFileToString(String path) {
        try {
            return Files.readString(Path.of(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Unable to open file: " + path, e);
        }
    }

    /// Format the Version Bump state as JSON for data/versions.json.
    public static String versionsToJson(State state) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n  \"version\": \"").append(state.version).append('"');
        builder.append(",\n  \"pending\": ");
        appendArray(builder, state.pending, FileIo::changeToJson);
        builder.append(",\n  \"releases\": ");
        appendArray(builder, state.releases, FileIo::releaseToJson);
        builder.append("\n}");
        return builder.toString();
    }

    /// Write the Version Bump state JSON to the given file.
    public static void writeVersionsJson(String path, State state) {
        try {
            Files.writeString(Path.of(path), versionsToJson(state) + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Unable to open output file: " + path, e);
        }
    }
}
