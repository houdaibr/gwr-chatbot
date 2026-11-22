package com.houda.gwr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

/** GwrAnswer against a small local HTTP stub that returns a fixed status and body. */
class GwrAnswerTest {

    private static final String PROPOSAL = """
            {"TOPOrderProposalHeader":{"groupRecord":[{"orderProposalNumber":500101,"orderProposalTypeCode":1,
              "siteCode":12,"orderProposalStatusCode":1,"orderProposalNote":""}]},
             "TOPOrderProposalDetailOrderLines":{"groupRecord":[{"orderProposalNumber":500101,"orderProposalLineNumber":"3",
              "articleCode":"1000222","siteCode":12,"supplierCode":"804006","orderProposalOrderedQuantity":0,
              "netUnitPurchasePrice":0}]},
             "TOPOrderProposalLineListAlerts":{"groupRecord":[{"orderProposalNumber":500101,"orderProposalLineNumber":"3",
              "articleCode":"1000222","orderProposalDetailAlertCode":12}]},
             "TOPOrderProposalHeaderListAlerts":{"groupRecord":[]}}""";

    private static final Map<String, Set<String>> PROPOSAL_DROP = Map.of(
            "TOPOrderProposalDetailOrderLines", Set.of("orderProposalNumber", "siteCode", "supplierCode"),
            "TOPOrderProposalLineListAlerts", Set.of("orderProposalNumber"));

    private HttpServer server;
    private int status;
    private String body;
    private GwrAnswer answer;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        answer = new GwrAnswer(new GwrClient("http://localhost:" + server.getAddress().getPort() + "/gwr-api"), new Codes());
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private String fetch(int status, String body, Map<String, String> params, Map<String, Set<String>> drop) {
        this.status = status;
        this.body = body;
        return answer.fetch("GWSOrder/Test", params, drop);
    }

    @Test
    void dropsEmptyAndListedFieldsAndAddsMeanings() {
        String out = fetch(200, PROPOSAL, Map.of("orderProposalNumber", "500101"), PROPOSAL_DROP);

        // header keeps its fields, drops the empty note, gets the status meaning and the undocumented type meaning
        assertTrue(out.contains("{\"orderProposalNumber\":500101,\"orderProposalTypeCode\":1,"
                + "\"orderProposalTypeCodeMeaning\":\"not described in the documentation\",\"siteCode\":12,"
                + "\"orderProposalStatusCode\":1,\"orderProposalStatusCodeMeaning\":\"OK\"}"), out);
        assertFalse(out.contains("orderProposalNote"), out);
        // line: repeated inputs dropped, zero values kept
        assertTrue(out.contains("{\"orderProposalLineNumber\":\"3\",\"articleCode\":\"1000222\","
                + "\"orderProposalOrderedQuantity\":0,\"netUnitPurchasePrice\":0}"), out);
        // alert: meaning right after the code
        assertTrue(out.contains("\"orderProposalDetailAlertCode\":12,\"orderProposalDetailAlertCodeMeaning\":\"NPR - no price (blocking):"), out);
    }

    @Test
    void emptyResultIsNotFound() {
        String out = fetch(200, """
                {"pagination":{"pageNumber":1,"pageSize":50,"totalRecords":0,"totalPages":0},
                 "FORSiteArticleForecastList":{"groupRecord":[]}}""",
                Map.of("siteCode", "12"), Map.of());
        assertEquals("Not found in GWR: nothing matches siteCode 12 (total records: 0, total pages: 0).", out);
    }

    @Test
    void error400GivesTheGwrMessage() {
        String out = fetch(400, "{\"GoldWebServiceError\":{\"errorCode\":\"GWS-400\",\"errorMessage\":\"Missing mandatory parameter: siteCode\"}}",
                Map.of(), Map.of());
        assertEquals("GWR could not answer: Missing mandatory parameter: siteCode", out);
    }

    @Test
    void errorWithoutMessage() {
        assertEquals("GWR answered with an error (HTTP 500).", fetch(500, "{}", Map.of(), Map.of()));
    }

    @Test
    void bodyThatIsNotJsonDoesNotCrash() {
        assertEquals(GwrAnswer.UNEXPECTED, fetch(200, "<html>Service unavailable</html>", Map.of(), Map.of()));
        assertEquals(GwrAnswer.UNEXPECTED, fetch(502, "Bad gateway", Map.of(), Map.of()));
    }

    @Test
    void serverDownIsNotReachable() {
        server.stop(0);
        assertEquals(GwrAnswer.NOT_REACHABLE, answer.fetch("GWSOrder/Test", Map.of("siteCode", "12"), Map.of()));
    }
}
