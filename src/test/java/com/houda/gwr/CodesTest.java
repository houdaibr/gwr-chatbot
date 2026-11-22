package com.houda.gwr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CodesTest {

    private final Codes codes = new Codes();

    @Test
    void lineWarningHasNameBlockingMeaningAndAction() {
        String m = codes.describe("orderProposalDetailAlertCode", "12");
        assertTrue(m.startsWith("NPR - no price (blocking):"), m);
        assertTrue(m.contains("What to do: Enter the purchase prices in G.O.L.D. Central."), m);
    }

    @Test
    void articleWarningUsesTheSameTableAsLineWarnings() {
        assertEquals(codes.describe("orderProposalDetailAlertCode", "1"), codes.describe("supplyWarningCode", "1"));
        assertTrue(codes.describe("supplyWarningCode", "1").startsWith("RDG - important rounding (information only):"));
    }

    @Test
    void headerWarningWithConfigurableBlocking() {
        assertTrue(codes.describe("orderProposalHeaderAlertCode", "5")
                .startsWith("CCMax - commercial contract maximum exceeded (depends on configuration (table 1095 entry 4 NV2)):"));
    }

    @Test
    void duplicateHeaderCodeGivesBothMeanings() {
        String m = codes.describe("orderProposalHeaderAlertCode", "7");
        assertTrue(m.startsWith("CMin - ") && m.contains(" OR CCMin - "), m);
    }

    @Test
    void statuses() {
        assertEquals("OK", codes.describe("orderProposalStatusCode", "1"));
        assertEquals("Cancelled", codes.describe("orderProposalStatusCode", "901"));
        assertEquals("Active", codes.describe("articleStatusCode", "1"));
    }

    @Test
    void unknownCodeOfADocumentedTable() {
        assertEquals("unknown code 9, not described in the documentation", codes.describe("articleStatusCode", "9"));
        assertEquals("unknown code 999, not described in the documentation",
                codes.describe("orderProposalDetailAlertCode", " 999 "));
    }

    @Test
    void undocumentedCodeFields() {
        assertEquals("not described in the documentation", codes.describe("orderProposalTypeCode", "1"));
        assertEquals("not described in the documentation", codes.describe("profileLinkTypeCode", "2"));
        assertEquals("not described in the documentation", codes.describe("orderProposalValidationFlagCode", "0"));
        assertEquals("not described in the documentation", codes.describe("orderProposalWarningFlagCode", "1"));
        assertEquals("not described in the documentation", codes.describe("orderProposalNoteFlagCode", "0"));
        assertEquals("not described in the documentation", codes.describe("scheduleOrigin", "1"));
        assertEquals("not described in the documentation", codes.describe("articleSUCode", "1"));
        assertEquals("not described in the documentation", codes.describe("fixedDeliveryTime", "0"));
    }

    @Test
    void otherFieldsAreNotCodes() {
        assertNull(codes.describe("articleCode", "1000200"));
        assertNull(codes.describe("stockQuantity", "480"));
    }
}
