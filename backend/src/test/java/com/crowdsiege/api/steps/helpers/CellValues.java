package com.crowdsiege.api.steps.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Gherkin cell conventions: empty cell / "null" = null, "(非空白)" = any non-blank value,
 * a double-quoted cell is a literal (e.g. "   "), ISO date-times compare by instant.
 */
public final class CellValues {

    public static final String NON_BLANK = "(非空白)";

    private CellValues() {
    }

    /**
     * Unwraps a literal cell: null stays null, "\"   \"" becomes three spaces.
     */
    public static String literal(String cell) {
        if (cell != null && cell.length() >= 2 && cell.startsWith("\"") && cell.endsWith("\"")) {
            return cell.substring(1, cell.length() - 1);
        }
        return cell;
    }

    public static List<String> csv(String cell) {
        if (cell == null || cell.isBlank()) {
            return List.of();
        }
        return Arrays.stream(cell.split(",")).map(String::trim).toList();
    }

    public static Integer integerOrNull(String cell) {
        return cell == null || cell.isBlank() ? null : Integer.valueOf(cell.trim());
    }

    public static Instant instant(String cell) {
        return OffsetDateTime.parse(cell.trim()).toInstant();
    }

    public static void assertMatches(String field, String expected, Object actual) {
        String actualText = actual == null ? null : String.valueOf(actual);
        if (expected == null || "null".equals(expected)) {
            assertThat(actualText).as("欄位 %s 預期為 null", field).isNull();
            return;
        }
        if (NON_BLANK.equals(expected)) {
            assertThat(actualText).as("欄位 %s 預期非空白", field).isNotBlank();
            return;
        }
        Instant expectedInstant = tryInstant(expected);
        Instant actualInstant = actualText == null ? null : tryInstant(actualText);
        if (expectedInstant != null && actualInstant != null) {
            assertThat(actualInstant).as("欄位 %s 時間", field).isEqualTo(expectedInstant);
            return;
        }
        assertThat(actualText).as("欄位 %s", field).isEqualTo(expected);
    }

    public static void assertRowMatches(Map<String, String> expectedRow, Map<String, Object> actualRow) {
        expectedRow.forEach((field, expected) -> {
            assertThat(actualRow).as("缺少欄位 %s", field).containsKey(field);
            assertMatches(field, expected, actualRow.get(field));
        });
    }

    public static void assertJsonMatches(Map<String, String> expectedRow, JsonNode node) {
        expectedRow.forEach((field, expected) -> assertMatches(field, expected, jsonValue(node.get(field))));
    }

    public static Object jsonValue(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        return node.isValueNode() ? node.asText() : node.toString();
    }

    private static Instant tryInstant(String text) {
        if (text.length() < 20 || text.charAt(4) != '-' || !text.contains("T")) {
            return null;
        }
        try {
            return OffsetDateTime.parse(text).toInstant();
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
