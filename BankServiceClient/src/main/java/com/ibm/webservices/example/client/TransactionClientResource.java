package com.ibm.webservices.example.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * REST endpoint for the WS-AT Transaction Client
 * 
 * This resource provides HTTP endpoints to interact with the WS-AT client
 * running in Liberty, allowing web-based testing and monitoring.
 */
@ApplicationScoped
@Path("/transaction")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TransactionClientResource {
    
    private static final Logger logger = Logger.getLogger(TransactionClientResource.class.getName());
    
    @Inject
    private WSATTransactionClient wsatClient;
    
    /**
     * Health check endpoint
     */
    @GET
    @Path("/health")
    public Response health() {
        try {
            boolean initialized = wsatClient.isInitialized();
            String status = wsatClient.getTransactionStatus();
            
            String healthInfo = String.format(
                "{\n" +
                "  \"status\": \"UP\",\n" +
                "  \"client_initialized\": %s,\n" +
                "  \"transaction_status\": \"%s\",\n" +
                "  \"timestamp\": \"%s\"\n" +
                "}",
                initialized,
                status,
                java.time.Instant.now().toString()
            );
            
            return Response.ok(healthInfo).build();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Health check failed", e);
            String errorInfo = String.format(
                "{\n" +
                "  \"status\": \"DOWN\",\n" +
                "  \"error\": \"%s\",\n" +
                "  \"timestamp\": \"%s\"\n" +
                "}",
                e.getMessage().replace("\"", "\\\""),
                java.time.Instant.now().toString()
            );
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                          .entity(errorInfo)
                          .build();
        }
    }
    
    /**
     * Initialize the WS-AT client
     */
    @POST
    @Path("/initialize")
    public Response initialize() {
        try {
            wsatClient.initialize();
            
            String response = String.format(
                "{\n" +
                "  \"message\": \"WS-AT Client initialized successfully\",\n" +
                "  \"status\": \"SUCCESS\",\n" +
                "  \"timestamp\": \"%s\"\n" +
                "}",
                java.time.Instant.now().toString()
            );
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize client", e);
            String errorResponse = String.format(
                "{\n" +
                "  \"message\": \"Failed to initialize WS-AT Client\",\n" +
                "  \"error\": \"%s\",\n" +
                "  \"status\": \"ERROR\",\n" +
                "  \"timestamp\": \"%s\"\n" +
                "}",
                e.getMessage().replace("\"", "\\\""),
                java.time.Instant.now().toString()
            );
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(errorResponse)
                          .build();
        }
    }
    
    /**
     * Run the complete transaction demonstration
     */
    @POST
    @Path("/demo")
    public Response runDemo() {
        try {
            String demoResult = wsatClient.runTransactionDemo();
            
            String response = String.format(
                "{\n" +
                "  \"message\": \"Transaction demonstration completed\",\n" +
                "  \"status\": \"SUCCESS\",\n" +
                "  \"result\": \"%s\",\n" +
                "  \"timestamp\": \"%s\"\n" +
                "}",
                demoResult.replace("\"", "\\\"").replace("\n", "\\n"),
                java.time.Instant.now().toString()
            );
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to run transaction demo", e);
            String errorResponse = String.format(
                "{\n" +
                "  \"message\": \"Failed to run transaction demonstration\",\n" +
                "  \"error\": \"%s\",\n" +
                "  \"status\": \"ERROR\",\n" +
                "  \"timestamp\": \"%s\"\n" +
                "}",
                e.getMessage().replace("\"", "\\\""),
                java.time.Instant.now().toString()
            );
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(errorResponse)
                          .build();
        }
    }
    
    /**
     * Execute a single transfer
     */
    @POST
    @Path("/transfer")
    public Response executeTransfer(
            @QueryParam("from") String fromAccount,
            @QueryParam("to") String toAccount,
            @QueryParam("amount") Integer amountCents) {
        
        if (fromAccount == null || toAccount == null || amountCents == null) {
            String errorResponse = String.format(
                "{\n" +
                "  \"message\": \"Missing required parameters\",\n" +
                "  \"error\": \"Parameters 'from', 'to', and 'amount' are required\",\n" +
                "  \"status\": \"ERROR\",\n" +
                "  \"timestamp\": \"%s\"\n" +
                "}",
                java.time.Instant.now().toString()
            );
            return Response.status(Response.Status.BAD_REQUEST)
                          .entity(errorResponse)
                          .build();
        }
        
        try {
            String transferResult = wsatClient.executeTransfer(fromAccount, toAccount, amountCents);
            
            String response = String.format(
                "{\n" +
                "  \"message\": \"Transfer operation completed\",\n" +
                "  \"status\": \"SUCCESS\",\n" +
                "  \"from_account\": \"%s\",\n" +
                "  \"to_account\": \"%s\",\n" +
                "  \"amount_cents\": %d,\n" +
                "  \"amount_dollars\": %.2f,\n" +
                "  \"result\": \"%s\",\n" +
                "  \"timestamp\": \"%s\"\n" +
                "}",
                fromAccount,
                toAccount,
                amountCents,
                amountCents / 100.0,
                transferResult.replace("\"", "\\\"").replace("\n", "\\n"),
                java.time.Instant.now().toString()
            );
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to execute transfer", e);
            String errorResponse = String.format(
                "{\n" +
                "  \"message\": \"Failed to execute transfer\",\n" +
                "  \"error\": \"%s\",\n" +
                "  \"status\": \"ERROR\",\n" +
                "  \"timestamp\": \"%s\"\n" +
                "}",
                e.getMessage().replace("\"", "\\\""),
                java.time.Instant.now().toString()
            );
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(errorResponse)
                          .build();
        }
    }
    
    /**
     * Get current transaction status
     */
    @GET
    @Path("/status")
    public Response getStatus() {
        try {
            String status = wsatClient.getTransactionStatus();
            boolean initialized = wsatClient.isInitialized();
            
            String response = String.format(
                "{\n" +
                "  \"transaction_status\": \"%s\",\n" +
                "  \"client_initialized\": %s,\n" +
                "  \"timestamp\": \"%s\"\n" +
                "}",
                status,
                initialized,
                java.time.Instant.now().toString()
            );
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to get status", e);
            String errorResponse = String.format(
                "{\n" +
                "  \"message\": \"Failed to get transaction status\",\n" +
                "  \"error\": \"%s\",\n" +
                "  \"status\": \"ERROR\",\n" +
                "  \"timestamp\": \"%s\"\n" +
                "}",
                e.getMessage().replace("\"", "\\\""),
                java.time.Instant.now().toString()
            );
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity(errorResponse)
                          .build();
        }
    }
    
    /**
     * Get API documentation
     */
    @GET
    @Path("/help")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getHelp() {
        String help = 
            "WS-AT Transaction Client API\n" +
            "============================\n\n" +
            "Available endpoints:\n\n" +
            "GET  /transaction/health     - Health check\n" +
            "POST /transaction/initialize - Initialize the client\n" +
            "POST /transaction/demo       - Run transaction demonstration\n" +
            "POST /transaction/transfer   - Execute single transfer\n" +
            "                               Parameters: from, to, amount (in cents)\n" +
            "GET  /transaction/status     - Get transaction status\n" +
            "GET  /transaction/help       - This help message\n\n" +
            "Examples:\n" +
            "curl -X POST http://localhost:9080/client/api/transaction/initialize\n" +
            "curl -X POST http://localhost:9080/client/api/transaction/demo\n" +
            "curl -X POST \"http://localhost:9080/client/api/transaction/transfer?from=ACC001&to=ACC002&amount=5000\"\n" +
            "curl -X GET http://localhost:9080/client/api/transaction/status\n";
        
        return Response.ok(help).build();
    }
}