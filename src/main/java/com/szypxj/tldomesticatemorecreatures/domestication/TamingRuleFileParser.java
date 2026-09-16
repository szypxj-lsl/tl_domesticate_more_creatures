package com.szypxj.tldomesticatemorecreatures.domestication;

import java.util.ArrayList;
import java.util.List;

final class TamingRuleFileParser {
    private TamingRuleFileParser() {
    }

    static boolean hasActiveLegacySections(String content) {
        for (String raw : content.split("\\R")) {
            String line = raw.trim();
            if (line.startsWith("#")) {
                continue;
            }
            if (line.equals("[[knockout]]")
                    || line.equals("[[feeding]]")
                    || line.equals("[[knockout.foods]]")
                    || line.equals("[[feeding.foods]]")) {
                return true;
            }
        }
        return false;
    }

    static List<ParsedRule> parse(String content) {
        String clean = stripComments(content);
        String rulesArray = assignedArray(clean, "rules");
        if (rulesArray == null) {
            return List.of();
        }

        List<ParsedRule> result = new ArrayList<>();
        for (String entry : splitTopLevel(inner(rulesArray, '[', ']'))) {
            String table = entry.trim();
            if (!isWrapped(table, '{', '}')) {
                continue;
            }
            String body = inner(table, '{', '}');
            String entity = stringField(body, "entity");
            String method = stringField(body, "method");
            int requiredPlayerLevel = intField(body, "required_player_level", 1);
            List<ParsedFood> nativeFoods = parseFoods(field(body, "native_foods"));
            List<ParsedFood> extraFoods = parseFoods(field(body, "extra_foods"));
            List<ParsedFood> legacyFoods = parseFoods(field(body, "foods"));
            List<String> removedNativeFoods = parseStrings(field(body, "removed_native_foods"));
            result.add(new ParsedRule(entity, method, requiredPlayerLevel, nativeFoods, extraFoods, legacyFoods, removedNativeFoods));
        }
        return List.copyOf(result);
    }


    private static List<String> parseStrings(String value) {
        if (!isWrapped(value, '[', ']')) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String entry : splitTopLevel(inner(value, '[', ']'))) {
            String item = unquote(entry);
            if (!item.isBlank()) {
                result.add(item);
            }
        }
        return List.copyOf(result);
    }

    private static List<ParsedFood> parseFoods(String value) {
        if (!isWrapped(value, '[', ']')) {
            return List.of();
        }
        List<ParsedFood> result = new ArrayList<>();
        for (String entry : splitTopLevel(inner(value, '[', ']'))) {
            String table = entry.trim();
            if (!isWrapped(table, '{', '}')) {
                continue;
            }
            String body = inner(table, '{', '}');
            String item = stringField(body, "item");
            int amount = intField(body, "amount", -1);
            result.add(new ParsedFood(item, amount));
        }
        return List.copyOf(result);
    }

    private static String assignedArray(String content, String key) {
        int searchFrom = 0;
        while (searchFrom < content.length()) {
            int keyIndex = content.indexOf(key, searchFrom);
            if (keyIndex < 0) {
                return null;
            }
            if (!wordBoundary(content, keyIndex - 1) || !wordBoundary(content, keyIndex + key.length())) {
                searchFrom = keyIndex + key.length();
                continue;
            }
            int equals = skipWhitespace(content, keyIndex + key.length());
            if (equals >= content.length() || content.charAt(equals) != '=') {
                searchFrom = keyIndex + key.length();
                continue;
            }
            int start = skipWhitespace(content, equals + 1);
            if (start >= content.length() || content.charAt(start) != '[') {
                return null;
            }
            int end = matching(content, start, '[', ']');
            return end < 0 ? null : content.substring(start, end + 1);
        }
        return null;
    }

    private static String field(String body, String key) {
        for (String part : splitTopLevel(body)) {
            int equals = topLevelEquals(part);
            if (equals < 0) {
                continue;
            }
            String candidate = part.substring(0, equals).trim();
            if (key.equals(candidate)) {
                return part.substring(equals + 1).trim();
            }
        }
        return "";
    }

    private static String stringField(String body, String key) {
        return unquote(field(body, key));
    }

    private static int intField(String body, String key, int fallback) {
        try {
            return Integer.parseInt(field(body, key));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static List<String> splitTopLevel(String value) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        int braces = 0;
        int brackets = 0;
        boolean quoted = false;
        boolean escaped = false;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (quoted) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    quoted = false;
                }
                continue;
            }
            if (c == '"') {
                quoted = true;
            } else if (c == '{') {
                braces++;
            } else if (c == '}') {
                braces--;
            } else if (c == '[') {
                brackets++;
            } else if (c == ']') {
                brackets--;
            } else if (c == ',' && braces == 0 && brackets == 0) {
                String part = value.substring(start, i).trim();
                if (!part.isEmpty()) {
                    parts.add(part);
                }
                start = i + 1;
            }
        }

        String tail = value.substring(start).trim();
        if (!tail.isEmpty()) {
            parts.add(tail);
        }
        return parts;
    }

    private static int topLevelEquals(String value) {
        int braces = 0;
        int brackets = 0;
        boolean quoted = false;
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (quoted) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    quoted = false;
                }
                continue;
            }
            if (c == '"') {
                quoted = true;
            } else if (c == '{') {
                braces++;
            } else if (c == '}') {
                braces--;
            } else if (c == '[') {
                brackets++;
            } else if (c == ']') {
                brackets--;
            } else if (c == '=' && braces == 0 && brackets == 0) {
                return i;
            }
        }
        return -1;
    }

    private static int matching(String value, int start, char open, char close) {
        int depth = 0;
        boolean quoted = false;
        boolean escaped = false;
        for (int i = start; i < value.length(); i++) {
            char c = value.charAt(i);
            if (quoted) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    quoted = false;
                }
                continue;
            }
            if (c == '"') {
                quoted = true;
            } else if (c == open) {
                depth++;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String stripComments(String value) {
        StringBuilder out = new StringBuilder(value.length());
        boolean quoted = false;
        boolean escaped = false;
        boolean comment = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (comment) {
                if (c == '\n' || c == '\r') {
                    comment = false;
                    out.append(c);
                }
                continue;
            }
            if (quoted) {
                out.append(c);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    quoted = false;
                }
                continue;
            }
            if (c == '"') {
                quoted = true;
                out.append(c);
            } else if (c == '#') {
                comment = true;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static boolean isWrapped(String value, char open, char close) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.length() >= 2 && trimmed.charAt(0) == open && trimmed.charAt(trimmed.length() - 1) == close;
    }

    private static String inner(String value, char open, char close) {
        String trimmed = value.trim();
        if (!isWrapped(trimmed, open, close)) {
            return "";
        }
        return trimmed.substring(1, trimmed.length() - 1);
    }

    private static String unquote(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.length() >= 2 && trimmed.charAt(0) == '"' && trimmed.charAt(trimmed.length() - 1) == '"') {
            return trimmed.substring(1, trimmed.length() - 1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        }
        return trimmed;
    }

    private static int skipWhitespace(String value, int index) {
        int i = index;
        while (i < value.length() && Character.isWhitespace(value.charAt(i))) {
            i++;
        }
        return i;
    }

    private static boolean wordBoundary(String value, int index) {
        return index < 0 || index >= value.length() || !Character.isJavaIdentifierPart(value.charAt(index));
    }

    record ParsedRule(
            String entity,
            String method,
            int requiredPlayerLevel,
            List<ParsedFood> nativeFoods,
            List<ParsedFood> extraFoods,
            List<ParsedFood> legacyFoods,
            List<String> removedNativeFoods
    ) {
        ParsedRule {
            nativeFoods = List.copyOf(nativeFoods);
            extraFoods = List.copyOf(extraFoods);
            legacyFoods = List.copyOf(legacyFoods);
            removedNativeFoods = List.copyOf(removedNativeFoods);
        }
    }

    record ParsedFood(String item, int amount) {
    }
}
