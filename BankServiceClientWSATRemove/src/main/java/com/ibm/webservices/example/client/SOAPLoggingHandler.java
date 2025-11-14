package com.ibm.webservices.example.client;

import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPHeader;
import jakarta.xml.soap.SOAPElement;

import javax.xml.namespace.QName;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * SOAP Handler for logging SOAP messages in Liberty environment
 * 
 * This handler intercepts SOAP messages and logs them using Java Util Logging
 * for debugging and monitoring purposes in Liberty.
 */
public class SOAPLoggingHandler implements SOAPHandler<SOAPMessageContext> {
    
    private static final Logger logger = Logger.getLogger(SOAPLoggingHandler.class.getName());
    
    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        Boolean outboundProperty = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        boolean isOutbound = outboundProperty != null && outboundProperty;
        
        if (isOutbound) {
            // Remove CoordinationContext from outbound messages
            removeCoordinationContext(context);
        }
        
        logSOAPMessage(context);
        return true; // Continue processing
    }
    
    @Override
    public boolean handleFault(SOAPMessageContext context) {
        logger.severe("SOAP FAULT DETECTED");
        logSOAPMessage(context);
        return true; // Continue processing
    }
    
    @Override
    public void close(MessageContext context) {
        // No cleanup needed
    }
    
    @Override
    public Set<QName> getHeaders() {
        return null; // We don't handle any specific headers
    }
    
    /**
     * Logs the SOAP message using Java Util Logging
     * 
     * @param context The SOAP message context
     */
    private void logSOAPMessage(SOAPMessageContext context) {
        Boolean outboundProperty = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        boolean isOutbound = outboundProperty != null && outboundProperty;
        
        try {
            SOAPMessage message = context.getMessage();
            
            StringBuilder logMessage = new StringBuilder();
            
            if (isOutbound) {
                logMessage.append("\n").append("▼".repeat(80));
                logMessage.append("\nOUTBOUND SOAP MESSAGE (Request to Server)");
                logMessage.append("\n").append("▼".repeat(80));
            } else {
                logMessage.append("\n").append("▲".repeat(80));
                logMessage.append("\nINBOUND SOAP MESSAGE (Response from Server)");
                logMessage.append("\n").append("▲".repeat(80));
            }
            
            // Add message details
            logMessage.append(getMessageDetails(context, isOutbound));
            
            // Add the actual SOAP message
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            message.writeTo(baos);
            String soapMessageString = baos.toString("UTF-8");
            
            logMessage.append("\n\nSOAP Message Content:");
            logMessage.append("\n").append("-".repeat(40));
            logMessage.append("\n").append(formatXML(soapMessageString));
            logMessage.append("\n").append("-".repeat(40));
            
            // Log the complete message
            logger.info(logMessage.toString());
            
            // Also print to console for immediate visibility
            System.out.println(logMessage.toString());
            
        } catch (SOAPException | IOException e) {
            e.printStackTrace(System.err);
            logger.log(Level.SEVERE, "Error logging SOAP message", e);
        }
    }
    
    /**
     * Gets additional message details from the context
     * 
     * @param context The SOAP message context
     * @param isOutbound Whether this is an outbound message
     * @return Formatted message details
     */
    private String getMessageDetails(SOAPMessageContext context, boolean isOutbound) {
        StringBuilder details = new StringBuilder();
        
        try {
            if (isOutbound) {
                details.append("\nDirection: Client → Server");
                Object endpointAddress = context.get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
                if (endpointAddress != null) {
                    details.append("\nEndpoint: ").append(endpointAddress);
                }
            } else {
                details.append("\nDirection: Server → Client");
                Object responseCode = context.get(MessageContext.HTTP_RESPONSE_CODE);
                if (responseCode != null) {
                    details.append("\nHTTP Response Code: ").append(responseCode);
                }
            }
            
            // Add HTTP headers if available
            @SuppressWarnings("unchecked")
            java.util.Map<String, java.util.List<String>> headers = 
                (java.util.Map<String, java.util.List<String>>) context.get(MessageContext.HTTP_REQUEST_HEADERS);
            
            if (headers != null && !headers.isEmpty()) {
                details.append("\n\nHTTP Headers:");
                for (java.util.Map.Entry<String, java.util.List<String>> entry : headers.entrySet()) {
                    details.append("\n  ").append(entry.getKey()).append(": ").append(entry.getValue());
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace(System.err);
            logger.log(Level.WARNING, "Error getting message details", e);
            details.append("\nError getting message details: ").append(e.getMessage());
        }
        
        return details.toString();
    }
    
    /**
     * Formats XML for better readability
     * 
     * @param xml The XML string to format
     * @return Formatted XML string
     */
    private String formatXML(String xml) {
        try {
            // Simple formatting - add line breaks after major elements
            return xml.replace("><", ">\n<")
                     .replace("<?xml", "\n<?xml")
                     .replace("<soap:", "\n<soap:")
                     .replace("</soap:", "\n</soap:")
                     .replace("<ns", "\n  <ns")
                     .replace("</ns", "\n  </ns")
                     .trim();
        } catch (Exception e) {
            e.printStackTrace(System.err);
            logger.log(Level.WARNING, "Error formatting XML", e);
            // If formatting fails, return original
            return xml;
        }
    }
    
    /**
     * Removes CoordinationContext and its children from outbound SOAP messages
     *
     * @param context The SOAP message context
     */
    private void removeCoordinationContext(SOAPMessageContext context) {
        try {
            SOAPMessage message = context.getMessage();
            SOAPHeader header = message.getSOAPHeader();
            
            if (header != null) {
                // Look for CoordinationContext elements in the header
                Iterator<?> headerElements = header.getChildElements();
                
                while (headerElements.hasNext()) {
                    Object element = headerElements.next();
                    if (element instanceof SOAPElement) {
                        SOAPElement soapElement = (SOAPElement) element;
                        
                        // Check if this is a CoordinationContext element
                        // Common namespaces for WS-Coordination:
                        // - http://docs.oasis-open.org/ws-tx/wscoor/2006/06
                        // - http://schemas.xmlsoap.org/ws/2004/10/wscoor
                        String localName = soapElement.getLocalName();
                        String namespaceURI = soapElement.getNamespaceURI();
                        
                        if ("CoordinationContext".equals(localName) &&
                            (namespaceURI != null &&
                             (namespaceURI.contains("wscoor") ||
                              namespaceURI.contains("ws-coordination") ||
                              namespaceURI.contains("coordination")))) {
                            
                            logger.info("Removing CoordinationContext element from outbound message");
                            headerElements.remove();
                            
                            // Log the removal for debugging
                            System.out.println("REMOVED CoordinationContext from SOAP header");
                        }
                    }
                }
                
                // Also check for any other coordination-related elements
                removeCoordinationRelatedElements(header);
            }
            
        } catch (SOAPException e) {
            logger.log(Level.WARNING, "Error removing CoordinationContext from SOAP message", e);
            e.printStackTrace(System.err);
        }
    }
    
    /**
     * Removes other coordination-related elements that might be present
     *
     * @param header The SOAP header to clean
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
                    if (namespaceURI != null &&
                        (namespaceURI.contains("wsat") ||
                         namespaceURI.contains("ws-atomictransaction") ||
                         namespaceURI.contains("atomic-transaction"))) {
                        
                        logger.info("Removing WS-AT related element: " + localName);
                        headerElements.remove();
                        System.out.println("REMOVED WS-AT element: " + localName);
                    }
                    
                    // Remove transaction-related elements
                    if (localName != null &&
                        (localName.contains("Transaction") ||
                         localName.contains("Coordination") ||
                         localName.contains("Activity"))) {
                        
                        logger.info("Removing transaction-related element: " + localName);
                        headerElements.remove();
                        System.out.println("REMOVED transaction element: " + localName);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error removing coordination-related elements", e);
            e.printStackTrace(System.err);
        }
    }
}