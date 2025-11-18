package com.ibm.webservices.proxy;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.soap.*;
import jakarta.inject.Inject;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;

import java.io.*;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Web Services Proxy Servlet
 *
 * This servlet acts as a proxy for JAX-WS requests:
 * 1. Accepts all incoming SOAP requests
 * 2. Extracts destination URL from WS-Addressing To field in SOAP header
 * 3. If To field is not available, falls back to HTTP headers for routing
 * 4. Removes CoordinationType and related elements from SOAP headers
 * 5. Forwards the modified request to the destination service
 * 6. Returns the response back to the client
 *
 * Routing Priority:
 * 1. WS-Addressing To field in SOAP header (primary)
 * 2. HTTP headers (X-Proxy-Destination-* headers) (fallback)
 *
 * For WSDL requests, HTTP headers are used since there's no SOAP message.
 */
@WebServlet(name = "WebServicesProxyServlet", urlPatterns = {"/*"})
public class WebServicesProxyServlet extends HttpServlet {
    
    private static final Logger logger = Logger.getLogger(WebServicesProxyServlet.class.getName());
    
    @Inject
    private ProxyConfiguration config;
    
    // Header names for destination configuration
    private static final String DESTINATION_URL_HEADER = "X-Proxy-Destination-URL";
    private static final String DESTINATION_HOST_HEADER = "X-Proxy-Destination-Host";
    private static final String DESTINATION_PORT_HEADER = "X-Proxy-Destination-Port";
    private static final String DESTINATION_PROTOCOL_HEADER = "X-Proxy-Destination-Protocol";
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        if (config.isEnableDetailedLogging()) {
            logger.info("=== PROXY: Incoming JAX-WS Request ===");
            logger.info("PROXY: Request URI: " + request.getRequestURI());
            logger.info("PROXY: Request Method: " + request.getMethod());
            logger.info("PROXY: Content Type: " + request.getContentType());
            logger.info("PROXY: Content Length: " + request.getContentLength());
            logger.info("PROXY: Remote Address: " + request.getRemoteAddr());
            logger.info("PROXY: Query String: " + request.getQueryString());
            logRequestHeaders(request);
        }
        
        try {
            // Read the incoming SOAP message first
            String soapContent = readRequestBody(request);
            if (config.isEnableDetailedLogging()) {
                logger.info("PROXY: Original SOAP message received");
                logger.info("PROXY: Incoming SOAP Content:\n" + soapContent);
            }
            
            // Parse the SOAP message
            SOAPMessage soapMessage = parseSOAPMessage(soapContent, request.getContentType());
            
            // Determine destination URL from SOAP To field, fallback to HTTP headers
            String destinationUrl = extractDestinationFromSOAP(soapMessage, request);
            if (destinationUrl == null) {
                if (config.isEnableDetailedLogging()) {
                    logger.info("PROXY: No WS-Addressing To field found, falling back to HTTP headers");
                }
                destinationUrl = extractDestinationUrl(request);
                if (destinationUrl == null) {
                    sendErrorResponse(response, new IllegalArgumentException(
                        "No destination URL found in SOAP message To field or HTTP headers. " +
                        "Ensure the SOAP message contains a valid WS-Addressing To header or provide " +
                        "destination via HTTP headers (" + DESTINATION_URL_HEADER + " or " +
                        DESTINATION_HOST_HEADER + "/" + DESTINATION_PORT_HEADER + "/" +
                        DESTINATION_PROTOCOL_HEADER + ")."));
                    return;
                }
            }
            
            // Remove CoordinationType and related elements
            removeCoordinationElements(soapMessage);
            
            // Convert back to string for forwarding
            String modifiedSoapContent = soapMessageToString(soapMessage);
            if (config.isEnableDetailedLogging()) {
                logger.info("PROXY: SOAP message processed and CoordinationType elements removed");
                logger.info("PROXY: Modified SOAP Content for forwarding:\n" + modifiedSoapContent);
                logger.info("PROXY: Forwarding to destination: " + destinationUrl);
            }
            
            // Forward the request
            String responseContent = forwardRequest(destinationUrl, modifiedSoapContent, request);
            
            // Send response back to client
            sendResponse(response, responseContent, request);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "PROXY: Error processing proxy request", e);
            if (config.isEnableDetailedLogging()) {
                logger.severe("PROXY: Exception details: " + e.getClass().getName() + " - " + e.getMessage());
                if (e.getCause() != null) {
                    logger.severe("PROXY: Caused by: " + e.getCause().getClass().getName() + " - " + e.getCause().getMessage());
                }
            }
            sendErrorResponse(response, e);
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        // Handle WSDL requests by forwarding them
        if (request.getQueryString() != null && request.getQueryString().toLowerCase().contains("wsdl")) {
            if (config.isEnableDetailedLogging()) {
                logger.info("=== PROXY: Incoming WSDL Request ===");
                logger.info("PROXY: Request URI: " + request.getRequestURI());
                logger.info("PROXY: Query String: " + request.getQueryString());
                logRequestHeaders(request);
            }
            
            try {
                // For WSDL requests, fall back to header-based destination since there's no SOAP message
                String destinationUrl = extractDestinationUrl(request);
                if (destinationUrl == null) {
                    sendErrorResponse(response, new IllegalArgumentException(
                        "No destination URL specified for WSDL request. Use " + DESTINATION_URL_HEADER +
                        " header or " + DESTINATION_HOST_HEADER + "/" + DESTINATION_PORT_HEADER +
                        "/" + DESTINATION_PROTOCOL_HEADER + " headers."));
                    return;
                }
                
                String wsdlContent = forwardGetRequest(destinationUrl, request);
                
                response.setContentType("text/xml");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(wsdlContent);
                
            } catch (Exception e) {
                logger.log(Level.SEVERE, "PROXY: Error forwarding WSDL request", e);
                if (config.isEnableDetailedLogging()) {
                    logger.severe("PROXY: WSDL Exception details: " + e.getClass().getName() + " - " + e.getMessage());
                }
                sendErrorResponse(response, e);
            }
        } else {
            // Return proxy status page
            sendStatusPage(response);
        }
    }
    
    /**
     * Log request headers for debugging
     */
    private void logRequestHeaders(HttpServletRequest request) {
        logger.info("PROXY: Request Headers:");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            logger.info("PROXY:   " + headerName + ": " + headerValue);
        }
    }
    
    /**
     * Extract destination URL from SOAP message To field (WS-Addressing)
     */
    private String extractDestinationFromSOAP(SOAPMessage soapMessage, HttpServletRequest request) {
        try {
            SOAPHeader header = soapMessage.getSOAPHeader();
            if (header == null) {
                if (config.isEnableDetailedLogging()) {
                    logger.warning("No SOAP header found in message");
                }
                return null;
            }
            
            // Look for WS-Addressing To element
            Iterator<?> headerElements = header.getChildElements();
            while (headerElements.hasNext()) {
                Object element = headerElements.next();
                if (element instanceof SOAPElement) {
                    SOAPElement soapElement = (SOAPElement) element;
                    String localName = soapElement.getLocalName();
                    String namespaceURI = soapElement.getNamespaceURI();
                    
                    // Check if this is a WS-Addressing To element
                    if ("To".equals(localName) && namespaceURI != null &&
                        (namespaceURI.contains("addressing") ||
                         namespaceURI.equals("http://www.w3.org/2005/08/addressing") ||
                         namespaceURI.equals("http://schemas.xmlsoap.org/ws/2004/08/addressing"))) {
                        
                        String toAddress = soapElement.getTextContent();
                        if (toAddress != null && !toAddress.trim().isEmpty()) {
                            if (config.isEnableDetailedLogging()) {
                                logger.info("PROXY: Found WS-Addressing To field: " + toAddress);
                            }
                            
                            // Add query string if present in original request
                            if (request.getQueryString() != null) {
                                String separator = toAddress.contains("?") ? "&" : "?";
                                toAddress += separator + request.getQueryString();
                            }
                            
                            return toAddress;
                        }
                    }
                }
            }
            
            if (config.isEnableDetailedLogging()) {
                logger.warning("No WS-Addressing To field found in SOAP header");
            }
            return null;
            
        } catch (SOAPException e) {
            logger.log(Level.SEVERE, "PROXY: Error extracting destination from SOAP message", e);
            if (config.isEnableDetailedLogging()) {
                logger.severe("PROXY: SOAP parsing error details: " + e.getMessage());
            }
            return null;
        }
    }
    
    /**
     * Extract destination URL from request headers (legacy method - kept for fallback)
     */
    private String extractDestinationUrl(HttpServletRequest request) {
        // First check for complete destination URL
        String destinationUrl = request.getHeader(DESTINATION_URL_HEADER);
        if (destinationUrl != null && !destinationUrl.trim().isEmpty()) {
            // Add the request path and query string to the destination URL
            String requestPath = request.getRequestURI();
            String contextPath = request.getContextPath();
            
            // Remove proxy context path to get the target service path
            String servicePath = requestPath;
            if (contextPath != null && !contextPath.isEmpty()) {
                servicePath = requestPath.substring(contextPath.length());
            }
            
            // Ensure proper URL concatenation between destination URL and service path
            if (!destinationUrl.endsWith("/") && !servicePath.startsWith("/")) {
                destinationUrl += "/" + servicePath;
            } else if (destinationUrl.endsWith("/") && servicePath.startsWith("/")) {
                destinationUrl += servicePath.substring(1);
            } else {
                destinationUrl += servicePath;
            }
            
            // Add query string if present
            if (request.getQueryString() != null) {
                destinationUrl += "?" + request.getQueryString();
            }
            
            return destinationUrl;
        }
        
        // Check for individual components
        String host = request.getHeader(DESTINATION_HOST_HEADER);
        String port = request.getHeader(DESTINATION_PORT_HEADER);
        String protocol = request.getHeader(DESTINATION_PROTOCOL_HEADER);
        
        if (host != null && !host.trim().isEmpty() &&
            port != null && !port.trim().isEmpty() &&
            protocol != null && !protocol.trim().isEmpty()) {
            
            // Build URL from components - all components must be provided
            String requestPath = request.getRequestURI();
            String contextPath = request.getContextPath();
            
            String servicePath = requestPath;
            if (contextPath != null && !contextPath.isEmpty()) {
                servicePath = requestPath.substring(contextPath.length());
            }
            
            StringBuilder url = new StringBuilder();
            url.append(protocol).append("://").append(host).append(":").append(port);
            
            // Ensure proper path concatenation
            if (!servicePath.startsWith("/")) {
                url.append("/");
            }
            url.append(servicePath);
            
            if (request.getQueryString() != null) {
                url.append("?").append(request.getQueryString());
            }
            
            return url.toString();
        }
        
        // No destination specified in headers
        return null;
    }
    
    /**
     * Read the request body content
     */
    private String readRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder content = new StringBuilder();
        
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        
        return content.toString();
    }
    
    /**
     * Parse SOAP message from string content
     */
    private SOAPMessage parseSOAPMessage(String soapContent, String contentType) throws SOAPException {
        MessageFactory messageFactory = MessageFactory.newInstance();
        
        try (ByteArrayInputStream bais = new ByteArrayInputStream(soapContent.getBytes("UTF-8"))) {
            return messageFactory.createMessage(null, bais);
        } catch (IOException e) {
            throw new SOAPException("Error parsing SOAP message", e);
        }
    }
    
    /**
     * Remove CoordinationType and related elements from SOAP header
     * Based on the logic from RemoveWSATHandler
     */
    private void removeCoordinationElements(SOAPMessage message) throws SOAPException {
        SOAPHeader header = message.getSOAPHeader();
        
        if (header != null) {
            if (config.isEnableDetailedLogging()) {
                logger.info("PROXY: Processing SOAP header for coordination elements removal");
            }
            
            // Look for CoordinationContext elements in the header
            if (config.isRemoveCoordinationContext()) {
                Iterator<?> headerElements = header.getChildElements();
                
                while (headerElements.hasNext()) {
                    Object element = headerElements.next();
                    if (element instanceof SOAPElement) {
                        SOAPElement soapElement = (SOAPElement) element;
                        
                        // Check if this is a CoordinationContext element
                        String localName = soapElement.getLocalName();
                        String namespaceURI = soapElement.getNamespaceURI();
                        
                        if ("CoordinationContext".equals(localName) &&
                            (namespaceURI != null &&
                             (namespaceURI.contains("wscoor") ||
                              namespaceURI.contains("ws-coordination") ||
                              namespaceURI.contains("coordination")))) {
                            
                            logger.info("PROXY: Removing CoordinationContext element from SOAP header");
                            headerElements.remove();
                            System.out.println("PROXY: Removed CoordinationContext from SOAP header");
                        }
                    }
                }
            }
            
            // Also check for any other coordination-related elements
            removeCoordinationRelatedElements(header);
        }
        
        // Save changes to the message
        message.saveChanges();
    }
    
    /**
     * Remove other coordination-related elements
     */
    private void removeCoordinationRelatedElements(SOAPHeader header) {
        try {
            Iterator<?> headerElements = header.getChildElements();
            
            while (headerElements.hasNext()) {
                Object element = headerElements.next();
                if (element instanceof SOAPElement) {
                    SOAPElement soapElement = (SOAPElement) element;
                    String localName = soapElement.getLocalName();
                    String namespaceURI = soapElement.getNamespaceURI();
                    
                    // Remove other WS-AT related elements
                    if (config.isRemoveWSATElements() && namespaceURI != null &&
                        (namespaceURI.contains("wsat") ||
                         namespaceURI.contains("ws-atomictransaction") ||
                         namespaceURI.contains("atomic-transaction"))) {
                        
                        logger.info("PROXY: Removing WS-AT related element: " + localName);
                        headerElements.remove();
                        System.out.println("PROXY: Removed WS-AT element: " + localName);
                    }
                    
                    // Remove transaction-related elements
                    if (config.isRemoveTransactionElements() && localName != null &&
                        (localName.contains("Transaction") ||
                         localName.contains("Coordination") ||
                         localName.contains("Activity"))) {
                        
                        logger.info("PROXY: Removing transaction-related element: " + localName);
                        headerElements.remove();
                        System.out.println("PROXY: Removed transaction element: " + localName);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "PROXY: Error removing coordination-related elements", e);
            if (config.isEnableDetailedLogging()) {
                logger.warning("PROXY: Coordination removal error: " + e.getMessage());
            }
        }
    }
    
    /**
     * Convert SOAP message back to string
     */
    private String soapMessageToString(SOAPMessage message) throws SOAPException, IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            message.writeTo(baos);
            return baos.toString("UTF-8");
        }
    }
    
    
    /**
     * Forward POST request to destination service
     */
    private String forwardRequest(String destinationUrl, String soapContent, HttpServletRequest originalRequest)
            throws IOException {
        
        if (config.isEnableDetailedLogging()) {
            logger.info("=== PROXY: Outgoing SOAP Request ===");
            logger.info("PROXY: Target URL: " + destinationUrl);
            logger.info("PROXY: Outgoing SOAP Content:\n" + soapContent);
        }
        
        try {
            HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getConnectionTimeoutMs()))
                .build();
             
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(destinationUrl))
                .timeout(Duration.ofMillis(config.getSocketTimeoutMs()))
                .header("Content-Type", "text/xml; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(soapContent));
            
            // Copy relevant headers
            copyHeaders(originalRequest, requestBuilder);
            
            HttpRequest request = requestBuilder.build();
            
            if (config.isEnableDetailedLogging()) {
                logger.info("PROXY: Outgoing Request Headers:");
                request.headers().map().forEach((name, values) -> {
                    values.forEach(value -> logger.info("PROXY:   " + name + ": " + value));
                });
            }
            
            // Execute request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (config.isEnableDetailedLogging()) {
                logger.info("=== PROXY: Response from Target Endpoint ===");
                logger.info("PROXY: Response Status: " + response.statusCode());
                logger.info("PROXY: Response Headers:");
                response.headers().map().forEach((name, values) -> {
                    values.forEach(value -> logger.info("PROXY:   " + name + ": " + value));
                });
                logger.info("PROXY: Response Content:\n" + response.body());
            }
            
            return response.body();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request was interrupted", e);
        }
    }
    
    /**
     * Forward GET request (for WSDL)
     */
    private String forwardGetRequest(String destinationUrl, HttpServletRequest originalRequest)
            throws IOException {
        
        if (config.isEnableDetailedLogging()) {
            logger.info("=== PROXY: Outgoing WSDL Request ===");
            logger.info("PROXY: Target URL: " + destinationUrl);
        }
        
        try {
            HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getConnectionTimeoutMs()))
                .build();
            
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(destinationUrl))
                .timeout(Duration.ofMillis(config.getSocketTimeoutMs()))
                .GET();
            
            // Copy relevant headers
            copyHeaders(originalRequest, requestBuilder);
            
            HttpRequest request = requestBuilder.build();
            
            if (config.isEnableDetailedLogging()) {
                logger.info("PROXY: Outgoing WSDL Request Headers:");
                request.headers().map().forEach((name, values) -> {
                    values.forEach(value -> logger.info("PROXY:   " + name + ": " + value));
                });
            }
            
            // Execute request
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (config.isEnableDetailedLogging()) {
                logger.info("=== PROXY: WSDL Response from Target Endpoint ===");
                logger.info("PROXY: Response Status: " + response.statusCode());
                logger.info("PROXY: Response Headers:");
                response.headers().map().forEach((name, values) -> {
                    values.forEach(value -> logger.info("PROXY:   " + name + ": " + value));
                });
                logger.info("PROXY: WSDL Response Content:\n" + response.body());
            }
            
            return response.body();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request was interrupted", e);
        }
    }
    
    /**
     * Copy headers from original request to forwarded request
     */
    private void copyHeaders(HttpServletRequest originalRequest, HttpRequest.Builder requestBuilder) {
        Enumeration<String> headerNames = originalRequest.getHeaderNames();
        
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            
            // Skip headers that shouldn't be forwarded
            if (!shouldSkipHeader(headerName)) {
                String headerValue = originalRequest.getHeader(headerName);
                requestBuilder.header(headerName, headerValue);
            }
        }
    }
    
    /**
     * Check if header should be skipped when forwarding
     */
    private boolean shouldSkipHeader(String headerName) {
        String lowerName = headerName.toLowerCase();
        
        // Always skip these headers
        if (lowerName.equals("host") ||
            lowerName.equals("content-length") ||
            lowerName.equals("transfer-encoding") ||
            lowerName.startsWith("x-proxy-destination")) {  // Skip proxy-specific headers
            return true;
        }
        
        // Handle restricted headers based on configuration
        if (isRestrictedHeader(lowerName)) {
            if (config.isAllowRestrictedHeaders()) {
                if (config.isEnableDetailedLogging()) {
                    logger.info("PROXY: Allowing restricted header: " + headerName);
                }
                return false; // Don't skip - allow the header
            } else {
                if (config.isEnableDetailedLogging()) {
                    logger.info("PROXY: Skipping restricted header: " + headerName);
                }
                return true; // Skip the header
            }
        }
        
        return false; // Don't skip other headers
    }
    
    /**
     * Check if a header is considered restricted
     */
    private boolean isRestrictedHeader(String lowerHeaderName) {
        // Common restricted headers that are typically blocked by HTTP clients
        return lowerHeaderName.equals("connection") ||
               lowerHeaderName.equals("upgrade") ||
               lowerHeaderName.equals("proxy-connection") ||
               lowerHeaderName.equals("proxy-authenticate") ||
               lowerHeaderName.equals("proxy-authorization") ||
               lowerHeaderName.equals("te") ||
               lowerHeaderName.equals("trailers") ||
               lowerHeaderName.equals("expect");
    }
    
    /**
     * Send response back to client
     */
    private void sendResponse(HttpServletResponse response, String content, HttpServletRequest originalRequest) 
            throws IOException {
        
        response.setContentType("text/xml");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        
        try (PrintWriter writer = response.getWriter()) {
            writer.write(content);
        }
        
        if (config.isEnableDetailedLogging()) {
            logger.info("PROXY: Response sent back to client");
            logger.info("PROXY: Final Response Content:\n" + content);
        }
    }
    
    /**
     * Send error response
     */
    private void sendErrorResponse(HttpServletResponse response, Exception e) throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("text/plain");
        
        try (PrintWriter writer = response.getWriter()) {
            writer.write("Proxy Error: " + e.getMessage());
        }
    }
    
    /**
     * Send status page for GET requests to proxy root
     */
    private void sendStatusPage(HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        
        try (PrintWriter writer = response.getWriter()) {
            writer.write("<html><head><title>Web Services Proxy</title></head>");
            writer.write("<body>");
            writer.write("<h1>Web Services Proxy</h1>");
            writer.write("<p>This proxy removes CoordinationType elements from JAX-WS requests and forwards them based on the WS-Addressing To field or HTTP headers as fallback.</p>");
            writer.write("<p>Status: Active</p>");
            writer.write("<h3>Configuration:</h3>");
            writer.write("<p>Remove CoordinationContext: " + config.isRemoveCoordinationContext() + "</p>");
            writer.write("<p>Remove WS-AT Elements: " + config.isRemoveWSATElements() + "</p>");
            writer.write("<p>Remove Transaction Elements: " + config.isRemoveTransactionElements() + "</p>");
            writer.write("<p>Allow Restricted Headers: " + config.isAllowRestrictedHeaders() + "</p>");
            writer.write("<h3>Usage:</h3>");
            writer.write("<p><strong>SOAP Requests:</strong> The proxy automatically extracts the destination URL from the WS-Addressing To field in the SOAP header. If not found, it falls back to HTTP headers.</p>");
            writer.write("<p><strong>WSDL Requests:</strong> Use one of these headers to specify destination:</p>");
            writer.write("<ul>");
            writer.write("<li><code>" + DESTINATION_URL_HEADER + "</code>: Complete destination URL</li>");
            writer.write("<li><code>" + DESTINATION_HOST_HEADER + "</code>: Destination host</li>");
            writer.write("<li><code>" + DESTINATION_PORT_HEADER + "</code>: Destination port</li>");
            writer.write("<li><code>" + DESTINATION_PROTOCOL_HEADER + "</code>: Destination protocol (http/https)</li>");
            writer.write("</ul>");
            writer.write("<h3>Restricted Headers:</h3>");
            writer.write("<p>When 'Allow Restricted Headers' is enabled, the proxy will forward restricted headers like 'upgrade', 'connection', etc. that are normally blocked by HTTP clients.</p>");
            writer.write("<p>Restricted headers include: connection, upgrade, proxy-connection, proxy-authenticate, proxy-authorization, te, trailers, expect</p>");
            writer.write("<h3>System Properties:</h3>");
            writer.write("<p>Configuration can be overridden at runtime using system properties. System properties take precedence over proxy.properties file values:</p>");
            writer.write("<ul>");
            writer.write("<li><code>-Dproxy.allow.restricted.headers=true</code> - Allow restricted headers</li>");
            writer.write("<li><code>-Dproxy.remove.coordination.context=false</code> - Disable coordination context removal</li>");
            writer.write("<li><code>-Dproxy.remove.wsat.elements=false</code> - Disable WS-AT element removal</li>");
            writer.write("<li><code>-Dproxy.remove.transaction.elements=false</code> - Disable transaction element removal</li>");
            writer.write("<li><code>-Dproxy.logging.detailed=false</code> - Disable detailed logging</li>");
            writer.write("<li><code>-Dproxy.connection.timeout.ms=15000</code> - Set connection timeout</li>");
            writer.write("<li><code>-Dproxy.socket.timeout.ms=45000</code> - Set socket timeout</li>");
            writer.write("</ul>");
            writer.write("<p>Example: <code>java -Dproxy.allow.restricted.headers=true -jar myapp.jar</code></p>");
            writer.write("<h3>Requirements:</h3>");
            writer.write("<p>SOAP messages should contain a valid WS-Addressing To header with the destination service URL. If not available, provide destination via HTTP headers.</p>");
            writer.write("</body></html>");
        }
    }
}