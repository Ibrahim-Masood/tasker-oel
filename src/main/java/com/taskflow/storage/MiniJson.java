package com.taskflow.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A tiny JSON reader/writer, just big enough for the task file: an array of flat objects whose
 * values are strings or {@code null}. Writing it by hand keeps the project free of external
 * libraries, so it builds with nothing but the JDK in any IDE.
 *
 * @author Ibrahim Masood
 * @version 1.0
 */
final class MiniJson {

    private MiniJson() {
    }

    /**
     * Converts a list of flat string maps to pretty-printed JSON text.
     *
     * @param objects the objects to write; a {@code null} value is written as JSON {@code null}
     * @return the JSON text
     */
    static String write(List<Map<String, String>> objects) {
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < objects.size(); i++) {
            sb.append("  {\n");
            int n = 0;
            int size = objects.get(i).size();
            for (Map.Entry<String, String> e : objects.get(i).entrySet()) {
                sb.append("    ").append(quote(e.getKey())).append(": ")
                        .append(e.getValue() == null ? "null" : quote(e.getValue()))
                        .append(++n < size ? ",\n" : "\n");
            }
            sb.append("  }").append(i < objects.size() - 1 ? ",\n" : "\n");
        }
        return sb.append("]\n").toString();
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    /**
     * Parses JSON text produced by {@link #write(List)}.
     *
     * @param text the JSON text
     * @return the list of objects, each a map from key to string (or {@code null})
     * @throws IOException if the text is not valid JSON of the expected shape
     */
    static List<Map<String, String>> read(String text) throws IOException {
        Parser p = new Parser(text);
        List<Map<String, String>> result = new ArrayList<>();
        p.skipSpace();
        if (p.atEnd()) {
            return result;
        }
        p.expect('[');
        p.skipSpace();
        if (p.peek() == ']') {
            p.next();
            return result;
        }
        while (true) {
            result.add(p.readObject());
            p.skipSpace();
            char c = p.next();
            if (c == ']') {
                break;
            }
            if (c != ',') {
                throw p.error("expected ',' or ']'");
            }
        }
        return result;
    }

    /** Recursive-descent style parser over a character buffer. */
    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s) {
            this.s = s;
        }

        boolean atEnd() {
            return pos >= s.length();
        }

        char peek() throws IOException {
            if (atEnd()) {
                throw error("unexpected end of file");
            }
            return s.charAt(pos);
        }

        char next() throws IOException {
            char c = peek();
            pos++;
            return c;
        }

        void skipSpace() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
                pos++;
            }
        }

        void expect(char c) throws IOException {
            skipSpace();
            if (next() != c) {
                throw error("expected '" + c + "'");
            }
        }

        IOException error(String msg) {
            return new IOException("Invalid task file at position " + pos + ": " + msg);
        }

        Map<String, String> readObject() throws IOException {
            Map<String, String> map = new LinkedHashMap<>();
            expect('{');
            skipSpace();
            if (peek() == '}') {
                next();
                return map;
            }
            while (true) {
                skipSpace();
                String key = readString();
                expect(':');
                skipSpace();
                String value;
                if (peek() == 'n') {
                    if (!s.startsWith("null", pos)) {
                        throw error("expected null");
                    }
                    pos += 4;
                    value = null;
                } else {
                    value = readString();
                }
                map.put(key, value);
                skipSpace();
                char c = next();
                if (c == '}') {
                    return map;
                }
                if (c != ',') {
                    throw error("expected ',' or '}'");
                }
            }
        }

        String readString() throws IOException {
            if (next() != '"') {
                pos--;
                throw error("expected a string");
            }
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = next();
                if (c == '"') {
                    return sb.toString();
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                char e = next();
                switch (e) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case '"', '\\', '/' -> sb.append(e);
                    case 'u' -> {
                        if (pos + 4 > s.length()) {
                            throw error("bad unicode escape");
                        }
                        try {
                            sb.append((char) Integer.parseInt(s.substring(pos, pos + 4), 16));
                        } catch (NumberFormatException ex) {
                            throw error("bad unicode escape");
                        }
                        pos += 4;
                    }
                    default -> throw error("bad escape \\" + e);
                }
            }
        }
    }
}
