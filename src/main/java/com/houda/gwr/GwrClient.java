package com.houda.gwr;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Read-only access to the GWR API: this class can only send GET requests.
 * The write endpoints (POST) are deliberately not supported.
 */
public class GwrClient {

    /** HTTP status and body of one GWR call. */
    public record Response(int status, String body) {
    }

    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    /** @param baseUrl e.g. http://localhost:8080/gwr-api */
    public GwrClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /**
     * GET {baseUrl}/webservices/{path}?{params}. Parameters with a null or blank value are left out.
     *
     * @param path e.g. GWSOrder/GWSOrderProposalGetOrderProposal
     */
    public Response get(String path, Map<String, String> params) throws IOException, InterruptedException {
        String query = params.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue().trim(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        URI uri = URI.create(baseUrl + "/webservices/" + path + (query.isEmpty() ? "" : "?" + query));
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new Response(response.statusCode(), response.body());
    }
}
