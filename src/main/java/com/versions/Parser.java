package com.versions;

import com.versions.Models.Change;
import com.versions.Models.ChangeKind;
import com.versions.Models.Release;
import com.versions.Models.State;
import com.versions.Models.Version;

public final class Parser {
    private Parser() {}

    /// Read a version number from its text form.
    public static Version parseVersionText(String text) {
        int first = text.indexOf('.');
        int second = text.indexOf('.', first + 1);
        Version version = new Version();
        version.major = Integer.parseInt(text.substring(0, first));
        version.minor = Integer.parseInt(text.substring(first + 1, second));
        version.patch = Integer.parseInt(text.substring(second + 1, second + 2));
        return version;
    }

    private static class JsonParser {
        private final String json;
        private int pos;

        public JsonParser(String json) {
            this.json = json != null ? json : "";
            this.pos = 0;
        }

        /// Parse the Version Bump state document into the in-memory State.
        public State parseVersionsDoc() {
            State state = new State();
            skipWs();
            expect('{');
            while (true) {
                skipWs();
                if (peek() == '}') {
                    pos++;
                    break;
                }
                String key = parseString();
                skipWs();
                expect(':');
                skipWs();
                if ("version".equals(key)) {
                    state.version = parseVersionText(parseString());
                } else if ("pending".equals(key)) {
                    parseArray(() -> state.pending.add(parseChangeObject()));
                } else if ("releases".equals(key)) {
                    parseArray(() -> state.releases.add(parseReleaseObject()));
                }
                skipWs();
                if (peek() == ',') {
                    pos++;
                }
            }
            return state;
        }

        /// Run parseElement once per item in a JSON array.
        private void parseArray(Runnable parseElement) {
            expect('[');
            skipWs();
            if (peek() == ']') {
                pos++;
                return;
            }
            while (true) {
                parseElement.run();
                skipWs();
                if (peek() == ',') {
                    pos++;
                    skipWs();
                    continue;
                }
                expect(']');
                break;
            }
        }

        /// Parse a single pending change object.
        private Change parseChangeObject() {
            Change change = new Change();
            skipWs();
            expect('{');
            while (true) {
                skipWs();
                if (peek() == '}') {
                    pos++;
                    break;
                }
                String key = parseString();
                skipWs();
                expect(':');
                skipWs();
                if ("type".equals(key)) {
                    change.type = ChangeKind.parseChangeKind(parseString());
                } else if ("note".equals(key)) {
                    change.note = parseString();
                }
                skipWs();
                if (peek() == ',') {
                    pos++;
                }
            }
            return change;
        }

        /// Parse a single release object.
        private Release parseReleaseObject() {
            Release release = new Release();
            skipWs();
            expect('{');
            while (true) {
                skipWs();
                if (peek() == '}') {
                    pos++;
                    break;
                }
                String key = parseString();
                skipWs();
                expect(':');
                skipWs();
                if ("version".equals(key)) {
                    release.version = parseVersionText(parseString());
                } else if ("notes".equals(key)) {
                    parseArray(() -> release.notes.add(parseString()));
                } else if ("yanked".equals(key)) {
                    release.yanked = parseBool();
                }
                skipWs();
                if (peek() == ',') {
                    pos++;
                }
            }
            return release;
        }

        private char peek() {
            return pos < json.length() ? json.charAt(pos) : '\0';
        }

        private void expect(char c) {
            if (peek() != c) {
                throw new RuntimeException("JSON parse error: expected '" + c + "' at position " + pos);
            }
            pos++;
        }

        private void skipWs() {
            while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
                pos++;
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (pos < json.length() && json.charAt(pos) != '"') {
                if (json.charAt(pos) == '\\' && pos + 1 < json.length()) {
                    pos++;
                    char escaped = json.charAt(pos++);
                    switch (escaped) {
                        case '"':  value.append('"');  break;
                        case '\\': value.append('\\'); break;
                        case '/':  value.append('/');  break;
                        case 'n':  value.append('\n'); break;
                        case 'r':  value.append('\r'); break;
                        case 't':  value.append('\t'); break;
                        default:   value.append(escaped);
                    }
                } else {
                    value.append(json.charAt(pos++));
                }
            }
            expect('"');
            return value.toString();
        }

        private boolean parseBool() {
            if (json.startsWith("true", pos)) {
                pos += 4;
                return true;
            }
            if (json.startsWith("false", pos)) {
                pos += 5;
                return false;
            }
            throw new RuntimeException("JSON parse error: expected boolean at position " + pos);
        }
    }

    /// Parse the Version Bump state JSON into the in-memory State.
    public static State parseVersionsJson(String doc) {
        JsonParser parser = new JsonParser(doc);
        return parser.parseVersionsDoc();
    }
}
