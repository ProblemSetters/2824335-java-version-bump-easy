package com.versions;

import com.versions.Models.CommandResult;
import com.versions.Models.State;

import java.io.PrintStream;

public final class Main {
    private static final String VERSIONS_PATH = "data/versions.json";

    private Main() {}

    /// Print the supported commands and their arguments.
    private static void printUsage(PrintStream out) {
        out.print("Usage:\n" +
                  "  versions change <major|minor|patch> <note>\n" +
                  "  versions release\n" +
                  "  versions yank <version>\n");
    }

    /// Load the Version Bump state, dispatch the requested command, and write any changes back.
    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage(System.err);
            System.exit(1);
        }

        String command = args[0];

        if ("change".equals(command)) {
            if (args.length != 3) {
                printUsage(System.err);
                System.exit(1);
            }
        } else if ("release".equals(command)) {
            if (args.length != 1) {
                printUsage(System.err);
                System.exit(1);
            }
        } else if ("yank".equals(command)) {
            if (args.length != 2) {
                printUsage(System.err);
                System.exit(1);
            }
        } else {
            System.err.println("Unknown command: " + command);
            printUsage(System.err);
            System.exit(1);
        }

        try {
            State state = Parser.parseVersionsJson(FileIo.readFileToString(VERSIONS_PATH));

            CommandResult result;
            if ("change".equals(command)) {
                result = Commands.change(state, args[1], args[2]);
            } else if ("release".equals(command)) {
                result = Commands.release(state);
            } else {
                result = Commands.yank(state, args[1]);
            }

            if (result.ok) {
                FileIo.writeVersionsJson(VERSIONS_PATH, state);
                System.out.println(result.message);
            } else {
                System.out.println("Error: " + result.reason);
            }
        } catch (Exception error) {
            System.err.println("Error: " + error.getMessage());
            System.exit(1);
        }
    }
}
