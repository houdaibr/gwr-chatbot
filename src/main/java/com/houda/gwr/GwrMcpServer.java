package com.houda.gwr;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * Read-only MCP server for GWR, over stdio (the tools are in GwrTools).
 * stdout carries the MCP protocol, so all logging must go to stderr.
 */
public class GwrMcpServer {

    public static void main(String[] args) {
        String baseUrl = System.getenv("GWR_API_BASE_URL");
        if (baseUrl == null || baseUrl.isBlank()) {
            System.err.println("GWR_API_BASE_URL is not set (example: http://localhost:8080/gwr-api). Server not started.");
            System.exit(1);
        }
        var answer = new GwrAnswer(new GwrClient(baseUrl.trim()), new Codes());

        McpServer.SyncSpecification<?> server = McpServer.sync(new StdioServerTransportProvider(McpJsonDefaults.getMapper()))
                .serverInfo("gwr", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder().tools(false).build());
        GwrTools.register(server, answer);
        server.build();
        System.err.println("GWR MCP server started (stdio), GWR API: " + baseUrl.trim());
    }
}
