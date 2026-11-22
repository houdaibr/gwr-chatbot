package com.houda.gwr;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * The GWR tools: one read-only tool per GET endpoint of the GWR API (the write endpoints are not offered).
 * Claude picks a tool by its description, so each description says in plain words what it returns and when to use it.
 */
public class GwrTools {

    /** Registers all GWR tools on the server. */
    static void register(McpServer.SyncSpecification<?> server, GwrAnswer answer) {

        tool(server, answer, "get_order_proposal",
                "Get one order proposal by its number: the header (site, supplier, supply sector, status, order and "
                        + "delivery dates, totals), all its order lines (article, stock, quantity to be delivered, need, "
                        + "order minimum and multiple, ordered quantity, price) and its warnings (line warnings and header "
                        + "warnings, each with its meaning, whether it blocks validation, and what to do). Use it when the "
                        + "user asks about a specific order proposal, for example why it cannot be validated, what is "
                        + "ordered in it, or which warnings it has.",
                Map.of("orderProposalNumber", integer("The order proposal number, for example 500101")),
                List.of("orderProposalNumber"),
                "GWSOrder/GWSOrderProposalGetOrderProposal",
                Map.of("TOPOrderProposalDetailOrderLines", Set.of("orderProposalNumber", "siteCode", "supplierCode"),
                        "TOPOrderProposalLineListAlerts", Set.of("orderProposalNumber"),
                        "TOPOrderProposalHeaderListAlerts", Set.of("orderProposalNumber")));

        tool(server, answer, "get_site_articles",
                "Get the articles of a site with their current situation: article, supplier, supply sector, article "
                        + "status, supply warning (with its meaning, whether it blocks validation, and what to do), "
                        + "orderable dates, total and average sales of the last 7 days, stock, expired stock, quantity "
                        + "to be delivered, next delivery date and stock cover in days. Use it when the user asks how an "
                        + "article is doing at a site, or which articles of a site, supplier or supply sector have a "
                        + "warning or a given status. Always narrow the result with the filters. One page holds up to "
                        + "50 articles; the answer gives the total number of records and pages, so ask for a next page "
                        + "only if it is needed.",
                Map.of("siteCode", integer("The site code, for example 12"),
                        "articleCode", string("Only this article, for example 1000200"),
                        "supplierCode", string("Only articles of this supplier, for example 804006"),
                        "supplySectorCode", string("Only articles of this supply sector, for example BEV01"),
                        "supplyWarningCode", string("Only articles with this warning code, for example 12"),
                        "articleStatusCode", integer("Only articles with this status code, for example 1 (Active)"),
                        "pageNumber", integer("Page to get (starts at 1). Default 1")),
                List.of("siteCode"),
                "GWSArticle/GWSSiteArticleForecastGetList",
                Map.of("FORSiteArticleForecastList", Set.of("siteCode", "articleSUCode", "dailySalesDay1", "dailySalesDay2",
                        "dailySalesDay3", "dailySalesDay4", "dailySalesDay5", "dailySalesDay6", "dailySalesDay7")));

        tool(server, answer, "list_order_proposals",
                "List the order proposals of a site, one row per proposal: number, supplier, supply sector, status, "
                        + "order and delivery dates, totals (articles, pallets, packs, units, value, trucks), whether it "
                        + "has warnings and how many articles have warnings. It does not show the warnings themselves "
                        + "nor whether they block validation. Use it to find proposals of a site, supplier or supply "
                        + "sector. To find which proposals cannot be validated, list them here, then open with "
                        + "get_order_proposal each proposal whose warning flag (orderProposalWarningFlagCode) is 1 and "
                        + "look for blocking warnings. Use the warning flag, not the number of articles with warnings: "
                        + "the flag also covers header warnings, and the number of articles with warnings can be 0. "
                        + "Skip cancelled proposals (status 901).",
                Map.of("siteCode", integer("The site code, for example 12"),
                        "supplierCode", string("Only proposals of this supplier, for example 804006"),
                        "supplySectorCode", string("Only proposals of this supply sector, for example BEV01"),
                        "orderProposalStatusCode", integer("Only proposals with this status code, for example 1 (OK)"),
                        "orderProposalNumber", integer("Only this proposal number, for example 500101")),
                List.of("siteCode"),
                "GWSOrder/GWSOrderProposalGetListHeaders",
                Map.of("TOPOrderProposalListHeaders", Set.of("siteCode")));

        tool(server, answer, "get_supplier_delivery_schedule",
                "Get the delivery schedule of a supplier for a site: one row per order slot, with the order date, day "
                        + "and time and the delivery date, day and time. Use it when the user asks when the next order "
                        + "or delivery of a supplier is, or on which days a supplier can be ordered or delivers.",
                Map.of("siteCode", integer("The site code, for example 12"),
                        "supplierCode", string("The supplier code, for example 804006")),
                List.of("siteCode", "supplierCode"),
                "GWSThirdParty/GWSSupplierDeliveryScheduleGetList",
                Map.of("TOPSupplierDeliveryScheduleList", Set.of("siteCode", "supplierCode")));

        tool(server, answer, "get_truck_capacity",
                "Get the truck and order minimum of a supplier for a site: the quantity of a full truck and the "
                        + "minimum order quantity, each with its unit (for example pallets). Use it when the user asks "
                        + "how much fits in a truck, whether a proposal fills its truck, or what the supplier's order "
                        + "minimum is.",
                Map.of("siteCode", integer("The site code, for example 12"),
                        "supplierCode", string("The supplier code, for example 804006")),
                List.of("siteCode", "supplierCode"),
                "GWSOrder/GWSOrderProposalGetTruckCapacityDetail",
                Map.of("TopManualOrderAdditionHeader", Set.of("siteCode", "supplierCode",
                        "fullTruckQuantity1UnitCode", "orderOrSupplierMinimumQuantityUnitCode")));

        tool(server, answer, "list_supply_sectors",
                "List the supply sectors of a site (code and name, for example BEV01 Beverages). Use it when the user "
                        + "asks which supply sectors a site has, or names a sector and its code is needed.",
                Map.of("siteCode", integer("The site code, for example 12")),
                List.of("siteCode"),
                "GWSCommon/GWSCommonSupplySectorSelectionGetListSupplySectors",
                Map.of("TOPCOSupplySectorSelectionListHeaders", Set.of("siteCode")));

        tool(server, answer, "list_supply_sector_links",
                "List the people (profiles) linked to the supply sectors of a site: sector, link type code, profile "
                        + "code and name. Use it when the user asks who is linked to a supply sector. The meaning of "
                        + "the link type codes is not described in the documentation: show them only as \"link type "
                        + "1 / 2, meaning not documented\", never as a role such as responsible or deputy.",
                Map.of("siteCode", integer("The site code, for example 12"),
                        "supplySectorCode", string("Only this supply sector, for example BEV01"),
                        "profileLinkTypeCode", integer("Only this link type code, for example 1")),
                List.of("siteCode"),
                "GWSCommon/GWSCommonSupplySectorSelectionGetListProfileLinks",
                Map.of("TOPCOSupplySectorProfileLinkList", Set.of("siteCode")));
    }

    /**
     * Registers one read-only tool: its inputs are the schema properties, passed as query parameters of a GET
     * on the given GWR path; the answer is cleaned with the given drop list.
     */
    private static void tool(McpServer.SyncSpecification<?> server, GwrAnswer answer, String name,
                             String description, Map<String, Object> properties, List<String> required, String path,
                             Map<String, Set<String>> drop) {
        var tool = McpSchema.Tool.builder(name)
                .description(description)
                .inputSchema(Map.of("type", "object", "properties", properties, "required", required,
                        "additionalProperties", false))
                .build();
        String[] inputs = properties.keySet().toArray(String[]::new);
        server.toolCall(tool, (exchange, request) -> McpSchema.CallToolResult.builder()
                .addTextContent(answer.fetch(path, params(request.arguments(), inputs), drop))
                .isError(false)
                .build());
    }

    private static Map<String, Object> integer(String description) {
        return Map.of("type", "integer", "description", description);
    }

    private static Map<String, Object> string(String description) {
        return Map.of("type", "string", "description", description);
    }

    /** The given tool arguments as GWR query parameters (numbers without decimals, missing ones left out). */
    static Map<String, String> params(Map<String, Object> arguments, String... names) {
        Map<String, String> params = new HashMap<>();
        for (String name : names) {
            Object v = arguments == null ? null : arguments.get(name);
            if (v instanceof Number n) {
                params.put(name, String.valueOf(n.longValue()));
            } else if (v != null) {
                params.put(name, v.toString());
            }
        }
        return params;
    }
}
