package com.houda.gwr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Code meanings from codes.md (generated once from gwr-docs by tools/build-codes.ps1, packed in the jar).
 */
public class Codes {

    /** API field -> docs table that explains its codes. Other code fields are not documented. */
    static final Map<String, String> FIELD_TABLE = Map.of(
            "supplyWarningCode", "1702",
            "orderProposalDetailAlertCode", "1702",
            "orderProposalHeaderAlertCode", "1745",
            "orderProposalStatusCode", "1704",
            "articleStatusCode", "955");

    /** Code fields whose codes the docs do not explain (see PLAN.md Findings): the server says so itself. */
    static final Set<String> UNDOCUMENTED_FIELDS = Set.of(
            "orderProposalTypeCode", "profileLinkTypeCode",
            "orderProposalValidationFlagCode", "orderProposalWarningFlagCode", "orderProposalNoteFlagCode",
            "scheduleOrigin", "articleSUCode", "fixedDeliveryTime");

    static final String NOT_DOCUMENTED = "not described in the documentation";

    /** "table:code" -> meanings (a code can have more than one: 1745 entry 7 is CMin and CCMin). */
    private final Map<String, List<String>> meanings = new HashMap<>();

    public Codes() {
        try (InputStream in = Codes.class.getResourceAsStream("/codes.md")) {
            if (in == null) {
                throw new IllegalStateException("codes.md not found in the jar");
            }
            load(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void load(BufferedReader reader) throws IOException {
        String table = null;
        for (String line; (line = reader.readLine()) != null; ) {
            if (line.startsWith("## Table ")) {
                table = line.substring("## Table ".length()).split(" ")[0];
                continue;
            }
            if (table == null || !line.startsWith("| ")) {
                continue;
            }
            String[] c = line.substring(2, line.length() - 2).split(" \\| ", -1);
            if (!c[0].matches("\\d+")) {
                continue; // header or separator row
            }
            String text = c.length == 3
                    ? c[1] // status tables: Code | Name | Source
                    : c[1] + " - " + c[2] + " (" + c[4] + "): " + c[3] + " What to do: " + c[5]; // warning tables
            meanings.computeIfAbsent(table + ":" + c[0], k -> new ArrayList<>()).add(text);
        }
    }

    /** Meaning of a code of the given API field, or null if the field is not a code field. */
    public String describe(String field, String code) {
        if (UNDOCUMENTED_FIELDS.contains(field)) {
            return NOT_DOCUMENTED;
        }
        String table = FIELD_TABLE.get(field);
        if (table == null) {
            return null;
        }
        List<String> m = meanings.get(table + ":" + code.trim());
        return m == null
                ? "unknown code " + code.trim() + ", " + NOT_DOCUMENTED
                : String.join(" OR ", m);
    }
}
