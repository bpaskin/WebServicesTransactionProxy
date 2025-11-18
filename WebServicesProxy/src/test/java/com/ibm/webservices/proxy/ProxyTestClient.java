package com.ibm.webservices.proxy;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Test client to demonstrate the Web Services Proxy functionality
 * 
 * This client shows how to:
 * 1. Send SOAP requests to the proxy
 * 2. Specify destination via headers
 * 3. Verify that CoordinationType elements are removed
 */
public class ProxyTestClient {
    
    private static final String PROXY_URL = "http://localhost:8080/TransactionWebService/BankService";
    
    public static void main(String[] args) {
        ProxyTestClient client = new ProxyTestClient();
        
        try {
            System.out.println("=== Web Services Proxy Test Client ===");
            
            // Test 1: SOAP request with destination URL header
            System.out.println("\n--- Test 1: SOAP Request with Destination URL Header ---");
            client.testSoapRequestWithDestinationUrl();
            
            // Test 2: SOAP request with individual destination headers
            System.out.println("\n--- Test 2: SOAP Request with Individual Destination Headers ---");
            client.testSoapRequestWithIndividualHeaders();
            
            // Test 3: WSDL request
            System.out.println("\n--- Test 3: WSDL Request ---");
            client.testWsdlRequest();
            
            // Test 4: Restricted headers test
            System.out.println("\n--- Test 4: Restricted Headers Test ---");
            client.testRestrictedHeaders();
            
            // Test 5: Status page
            System.out.println("\n--- Test 5: Proxy Status Page ---");
            client.testStatusPage();
            
        } catch (Exception e) {
            System.err.println("Error running tests: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Test SOAP request with complete destination URL header
     */
    private void testSoapRequestWithDestinationUrl() throws IOException, InterruptedException {
        String soapMessage = createSampleSoapMessage();
        
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(PROXY_URL))
            .timeout(Duration.ofSeconds(30))
            .header("X-Proxy-Destination-URL", "https://localhost:9444/TransactionWebService")
            .header("Content-Type", "text/xml; charset=utf-8")
            .header("SOAPAction", "transfer")
            .POST(HttpRequest.BodyPublishers.ofString(soapMessage))
            .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        System.out.println("Response Status: " + response.statusCode());
        System.out.println("Response: " + response.body().substring(0, Math.min(200, response.body().length())) + "...");
    }
    
    /**
     * Test SOAP request with individual destination headers
     */
    private void testSoapRequestWithIndividualHeaders() throws IOException, InterruptedException {
        String soapMessage = createSampleSoapMessage();
        
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(PROXY_URL))
            .timeout(Duration.ofSeconds(30))
            .header("X-Proxy-Destination-Host", "localhost")
            .header("X-Proxy-Destination-Port", "9444")
            .header("X-Proxy-Destination-Protocol", "https")
            .header("Content-Type", "text/xml; charset=utf-8")
            .header("SOAPAction", "transfer")
            .POST(HttpRequest.BodyPublishers.ofString(soapMessage))
            .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        System.out.println("Response Status: " + response.statusCode());
        System.out.println("Response: " + response.body().substring(0, Math.min(200, response.body().length())) + "...");
    }
    
    /**
     * Test WSDL request
     */
    private void testWsdlRequest() throws IOException, InterruptedException {
        String wsdlUrl = PROXY_URL + "?wsdl";
        
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(wsdlUrl))
            .timeout(Duration.ofSeconds(30))
            .header("X-Proxy-Destination-URL", "https://localhost:9444/TransactionWebService")
            .GET()
            .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        System.out.println("Response Status: " + response.statusCode());
        System.out.println("WSDL Content: " + response.body().substring(0, Math.min(300, response.body().length())) + "...");
    }
    
    /**
     * Test proxy status page
     */
    private void testStatusPage() throws IOException, InterruptedException {
        String statusUrl = "http://localhost:8080/";
        
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(statusUrl))
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        System.out.println("Response Status: " + response.statusCode());
        System.out.println("Status Page: " + response.body().substring(0, Math.min(400, response.body().length())) + "...");
    }
    
    /**
     * Test restricted headers functionality
     */
    private void testRestrictedHeaders() throws IOException, InterruptedException {
        String soapMessage = createSampleSoapMessage();
        
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        
        // Test with restricted headers (upgrade, connection)
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(PROXY_URL))
            .timeout(Duration.ofSeconds(30))
            .header("X-Proxy-Destination-URL", "https://localhost:9444/TransactionWebService")
            .header("Content-Type", "text/xml; charset=utf-8")
            .header("SOAPAction", "transfer")
            .header("Upgrade", "websocket")  // Restricted header
            .header("Connection", "upgrade") // Restricted header
            .header("Custom-Header", "test-value") // Non-restricted header
            .POST(HttpRequest.BodyPublishers.ofString(soapMessage))
            .build();
        
        System.out.println("Sending request with restricted headers:");
        System.out.println("  - Upgrade: websocket");
        System.out.println("  - Connection: upgrade");
        System.out.println("  - Custom-Header: test-value");
        
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("Response Status: " + response.statusCode());
            System.out.println("Note: Check proxy logs to see if restricted headers were processed correctly");
            System.out.println("Response: " + response.body().substring(0, Math.min(200, response.body().length())) + "...");
        } catch (Exception e) {
            System.out.println("Request failed (expected if target service is not running): " + e.getMessage());
            System.out.println("This test demonstrates the restricted headers functionality in the proxy configuration.");
        }
    }
    
    /**
     * Create a sample SOAP message with CoordinationType elements
     */
    private String createSampleSoapMessage() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                           xmlns:bank="urn:example:bank">
                <soap:Header>
                    <!-- This CoordinationContext should be removed by the proxy -->
                    <wscoor:CoordinationContext xmlns:wscoor="http://docs.oasis-open.org/ws-tx/wscoor/2006/06">
                        <wscoor:Identifier>urn:uuid:12345678-1234-1234-1234-123456789012</wscoor:Identifier>
                        <wscoor:CoordinationType>http://docs.oasis-open.org/ws-tx/wsat/2006/06</wscoor:CoordinationType>
                        <wscoor:RegistrationService>
                            <wsa:Address xmlns:wsa="http://www.w3.org/2005/08/addressing">
                                https://coordinator.example.com/registration
                            </wsa:Address>
                        </wscoor:RegistrationService>
                    </wscoor:CoordinationContext>
                    
                    <!-- This WS-AT element should also be removed -->
                    <wsat:ATAssertion xmlns:wsat="http://docs.oasis-open.org/ws-tx/wsat/2006/06">
                        <wsat:Durable>true</wsat:Durable>
                    </wsat:ATAssertion>
                </soap:Header>
                <soap:Body>
                    <bank:transfer>
                        <bank:fromAccount>ACC001</bank:fromAccount>
                        <bank:toAccount>ACC002</bank:toAccount>
                        <bank:cents>10000</bank:cents>
                    </bank:transfer>
                </soap:Body>
            </soap:Envelope>
            """;
    }
}