package com.ibm.webservices.example;

import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.soap.SOAPException;

import javax.xml.namespace.QName;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
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
            logger.log(Level.SEVERE, "Error logging SOAP message", e);
            System.err.println("Error logging SOAP message: " + e.getMessage());
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
            logger.log(Level.WARNING, "Error formatting XML", e);
            // If formatting fails, return original
            return xml;
        }
    }
}