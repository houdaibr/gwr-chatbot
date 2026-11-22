package com.houda.gwr;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Turns a GWR JSON answer into what the bot gets: fields are only dropped (never moved), and the documented
 * meaning is added next to each code as "{field}Meaning".
 */
public class GwrAnswer {

    static final String NOT_REACHABLE = "GWR is not reachable right now. Please try again later or contact IT.";
    static final String UNEXPECTED = "GWR answered something unexpected. Please contact IT.";

    private static final JsonMapper JSON = JsonMapper.shared();

    private final GwrClient gwr;
    private final Codes codes;

    public GwrAnswer(GwrClient gwr, Codes codes) {
        this.gwr = gwr;
        this.codes = codes;
    }

    /**
     * Calls GWR and returns the text the bot gets: the cleaned JSON, or a plain message when there is nothing to show.
     * Number inputs are typed as integers in the tool schema, so they are not checked here.
     */
    public String fetch(String path, Map<String, String> params, Map<String, Set<String>> drop) {
        GwrClient.Response response;
        try {
            response = gwr.get(path, params);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return NOT_REACHABLE;
        } catch (IOException e) {
            System.err.println("GWR call failed: " + path + " " + e);
            return NOT_REACHABLE;
        }
        JsonNode root;
        try {
            root = JSON.readTree(response.body());
        } catch (RuntimeException e) {
            System.err.println("GWR answer is not JSON: " + path + " HTTP " + response.status());
            return UNEXPECTED;
        }
        if (response.status() != 200) {
            String message = root.path("GoldWebServiceError").path("errorMessage").asString("");
            return message.isBlank()
                    ? "GWR answered with an error (HTTP " + response.status() + ")."
                    : "GWR could not answer: " + message;
        }
        if (!root.isObject()) {
            return UNEXPECTED;
        }
        if (isEmpty(root)) {
            JsonNode paging = root.path("pagination");
            return "Not found in GWR: nothing matches " + asked(params)
                    + (paging.isObject() ? " (total records: " + paging.path("totalRecords").asInt()
                    + ", total pages: " + paging.path("totalPages").asInt() + ")" : "") + ".";
        }
        return JSON.writeValueAsString(clean(root, drop));
    }

    /** True if every result list (groupRecord) in the answer is empty. */
    private static boolean isEmpty(JsonNode root) {
        for (JsonNode section : root.values()) {
            JsonNode records = section.path("groupRecord");
            if (records.isArray() && !records.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** The inputs of the call as "siteCode 12, articleCode 9999999", for the not-found message. */
    private static String asked(Map<String, String> params) {
        return params.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                .map(e -> e.getKey() + " " + e.getValue().trim())
                .collect(Collectors.joining(", "));
    }

    /**
     * @param root GWR JSON answer (an object)
     * @param drop fields to drop, per top-level section of the answer (e.g. "TOPOrderProposalDetailOrderLines");
     *             the key "*" applies to all sections. Empty fields ("" or null) are always dropped.
     */
    ObjectNode clean(JsonNode root, Map<String, Set<String>> drop) {
        ObjectNode out = JSON.createObjectNode();
        for (Map.Entry<String, JsonNode> section : root.properties()) {
            Set<String> dropHere = new HashSet<>(drop.getOrDefault("*", Set.of()));
            dropHere.addAll(drop.getOrDefault(section.getKey(), Set.of()));
            out.set(section.getKey(), cleanNode(section.getValue(), dropHere));
        }
        return out;
    }

    private JsonNode cleanNode(JsonNode node, Set<String> drop) {
        if (node.isArray()) {
            ArrayNode out = JSON.createArrayNode();
            node.values().forEach(v -> out.add(cleanNode(v, drop)));
            return out;
        }
        if (!node.isObject()) {
            return node;
        }
        ObjectNode out = JSON.createObjectNode();
        for (Map.Entry<String, JsonNode> p : node.properties()) {
            String field = p.getKey();
            JsonNode value = p.getValue();
            boolean skip = value.isNull() || (value.isString() && value.stringValue().isBlank()) || drop.contains(field);
            if (!skip) {
                out.set(field, cleanNode(value, drop));
                String meaning = value.isContainer() ? null : codes.describe(field, value.asString());
                if (meaning != null) {
                    out.put(field + "Meaning", meaning);
                }
            }
            // total and average at the place of day 7, also when the days themselves are dropped
            if (field.equals("dailySalesDay7")) {
                addSalesTotalAndAverage(node, out);
            }
        }
        return out;
    }

    /** Total and average of dailySalesDay1..7, right after day 7; only if all 7 days are numbers. */
    private static void addSalesTotalAndAverage(JsonNode row, ObjectNode out) {
        long total = 0;
        for (int day = 1; day <= 7; day++) {
            JsonNode sales = row.path("dailySalesDay" + day);
            if (!sales.isNumber()) {
                return;
            }
            total += sales.asLong();
        }
        out.put("salesTotal7Days", total);
        out.put("salesAverage7Days", Math.round(total * 10.0 / 7) / 10.0);
    }
}
