package com.ibm.webservices.proxy;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Test client to demonstrate the Web Services Proxy functionality
 *
 * This client shows how to:
 * 1. Send SOAP requests to the proxy with WS-Addressing To field
 * 2. Specify destination via HTTP headers (fallback method)
 * 3. Verify that CoordinationType elements are removed
 *
 * Note: The proxy now requires destination to be specified either in:
 * - SOAP message WS-Addressing To field (primary method)
 * - HTTP headers (fallback method)
 * Configuration-based destinations have been removed.
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
            
            // Test 4: Status page
            System.out.println("\n--- Test 4: Proxy Status Page ---");
            client.testStatusPage();
            
        } catch (Exception e) {
            System.err.println("Error running tests: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Test SOAP request with complete destination URL header
     */
    private void testSoapRequestWithDestinationUrl() throws IOException {
        String soapMessage = createSampleSoapMessage();
        
        HttpURLConnection connection = createConnection(PROXY_URL, "POST");
        
        // Set destination URL header
        connection.setRequestProperty("X-Proxy-Destination-URL", "https://localhost:9444");
        connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        connection.setRequestProperty("SOAPAction", "urn:example:bank/BankService/transferRequest");
        
        // Send SOAP message
        sendRequest(connection, soapMessage);
        
        // Read response
        String response = readResponse(connection);
        System.out.println("Response Status: " + connection.getResponseCode());
        System.out.println("Response: " + response.substring(0, Math.min(200, response.length())) + "...");
        
        connection.disconnect();
    }
    
    /**
     * Test SOAP request with individual destination headers
     * Note: All three headers (host, port, protocol) must be provided when using header-based routing
     */
    private void testSoapRequestWithIndividualHeaders() throws IOException {
        String soapMessage = createSampleSoapMessage();
        
        HttpURLConnection connection = createConnection(PROXY_URL, "POST");
        
        // Set individual destination headers
        connection.setRequestProperty("X-Proxy-Destination-Host", "localhost");
        connection.setRequestProperty("X-Proxy-Destination-Port", "9444");
        connection.setRequestProperty("X-Proxy-Destination-Protocol", "https");
        connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        connection.setRequestProperty("SOAPAction", "urn:example:bank/BankService/transferRequest");
        
        // Send SOAP message
        sendRequest(connection, soapMessage);
        
        // Read response
        String response = readResponse(connection);
        System.out.println("Response Status: " + connection.getResponseCode());
        System.out.println("Response: " + response.substring(0, Math.min(200, response.length())) + "...");
        
        connection.disconnect();
    }
    
    /**
     * Test WSDL request
     */
    private void testWsdlRequest() throws IOException {
        String wsdlUrl = PROXY_URL + "?wsdl";
        
        HttpURLConnection connection = createConnection(wsdlUrl, "GET");
        
        // Set destination headers for WSDL
        connection.setRequestProperty("X-Proxy-Destination-URL", "https://localhost:9444");
        
        // Read response
        String response = readResponse(connection);
        System.out.println("Response Status: " + connection.getResponseCode());
        System.out.println("WSDL Content: " + response.substring(0, Math.min(300, response.length())) + "...");
        
        connection.disconnect();
    }
    
    /**
     * Test proxy status page
     */
    private void testStatusPage() throws IOException {
        String statusUrl = "http://localhost:8080/";
        
        HttpURLConnection connection = createConnection(statusUrl, "GET");
        
        // Read response
        String response = readResponse(connection);
        System.out.println("Response Status: " + connection.getResponseCode());
        System.out.println("Status Page: " + response.substring(0, Math.min(400, response.length())) + "...");
        
        connection.disconnect();
    }
    
    /**
     * Create a sample SOAP message with CoordinationType elements
     */
    private String createSampleSoapMessage() {
        return """
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
             
             <soap:Header>
             <Action xmlns="http://www.w3.org/2005/08/addressing" soap:mustUnderstand="1">urn:example:bank/BankService/transferRequest</Action>
             <MessageID xmlns="http://www.w3.org/2005/08/addressing" soap:mustUnderstand="1">urn:uuid:716a7d6e-5564-4f2c-8cf4-3ebc910731be</MessageID>
             <To xmlns="http://www.w3.org/2005/08/addressing" soap:mustUnderstand="1">https://localhost:9444/TransactionWebService/BankService</To>
             <ReplyTo xmlns="http://www.w3.org/2005/08/addressing" soap:mustUnderstand="1">
             <Address>http://www.w3.org/2005/08/addressing/anonymous</Address>
             </ReplyTo>
             <CoordinationContext xmlns="http://docs.oasis-open.org/ws-tx/wscoor/2006/06" xmlns:ns2="http://www.w3.org/2005/08/addressing" soap:mustUnderstand="1">
             <Identifier>0000019a829022d3000000010a408b543fdb9d98fb1e0693187e0363c55c0c9005f56133</Identifier>
             <Expires>119889</Expires>
             <CoordinationType>http://docs.oasis-open.org/ws-tx/wsat/2006/06</CoordinationType>
             <RegistrationService>
             
               <ns2:Address>https://localhost:9443/ibm/wsatservice/RegistrationService
               </ns2:Address>
             
               <ns2:ReferenceParameters>
             
               <ns3:GlobalID xmlns:ns3="http://com.ibm.ws.wsat/extension">0000019a829022d3000000010a408b543fdb9d98fb1e0693187e0363c55c0c9005f56133
               </ns3:GlobalID>
             
               </ns2:ReferenceParameters>
             </RegistrationService>
             </CoordinationContext>
             
             </soap:Header>
             
             <soap:Body>
             
               <ns2:transfer xmlns:ns2="urn:example:bank">
             <fromAccount>ACC001</fromAccount>
             <toAccount>ACC002</toAccount>
             <cents>5000</cents>
             
               </ns2:transfer>
             
             </soap:Body>
             
             </soap:Envelope>
            """;
    }
    
    /**
     * Create HTTP connection
     */
    private HttpURLConnection createConnection(String urlString, String method) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setDoOutput("POST".equals(method));
        connection.setDoInput(true);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(30000);
        return connection;
    }
    
    /**
     * Send request body
     */
    private void sendRequest(HttpURLConnection connection, String content) throws IOException {
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = content.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
    }
    
    /**
     * Read response from connection
     */
    private String readResponse(HttpURLConnection connection) throws IOException {
        InputStream inputStream;
        
        try {
            inputStream = connection.getInputStream();
        } catch (IOException e) {
            inputStream = connection.getErrorStream();
            if (inputStream == null) {
                throw e;
            }
        }
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
            return response.toString();
        }
    }
}