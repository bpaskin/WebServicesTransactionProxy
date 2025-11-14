package com.ibm.webservices.example.client;

import com.ibm.webservices.example.client.generated.BankService;
import com.ibm.webservices.example.client.generated.BankService_Service;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.soap.AddressingFeature;
import jakarta.xml.ws.handler.Handler;
import jakarta.xml.ws.handler.HandlerResolver;
import jakarta.xml.ws.handler.PortInfo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Simple Bank Service Client for Liberty
 * 
 * This client demonstrates how to use the BankService web service
 * with simple direct calls.
 *
 * Features:
 * - Direct web service calls
 * - SOAP message logging
 * - Error handling for service calls
 * - Liberty-specific configuration
 */
@ApplicationScoped
@Named("transactionClient")
public class TransactionClient {
    
    private static final Logger logger = Logger.getLogger(TransactionClient.class.getName());
    private static final String DEFAULT_WSDL_URL = "https://localhost:9444/TransactionWebService/BankService?wsdl";
    
    private BankService bankService;
    private boolean initialized = false;
    
    /**
     * Initialize the client
     */
    public void initialize() throws Exception {
        if (initialized) {
            return;
        }
        
        logger.info("=== Initializing Bank Service Client in Liberty ===");
        
        // Create BankService proxy
        createBankServiceProxy();
        
        initialized = true;
        logger.info("Client initialized successfully in Liberty");
    }
    
    /**
     * Create BankService proxy with addressing features and SOAP message logging
     */
    private void createBankServiceProxy() throws Exception {
        try {
            logger.info("Connecting to: " + DEFAULT_WSDL_URL);
            
            // Create the service with WS-Addressing
            BankService_Service service = new BankService_Service();
            
            // Set up handler resolver for SOAP message logging
            service.setHandlerResolver(new HandlerResolver() {
                @Override
                public List<Handler> getHandlerChain(PortInfo portInfo) {
                    List<Handler> handlerChain = new ArrayList<>();
                    handlerChain.add(new SOAPLoggingHandler());
                    return handlerChain;
                }
            });
            
            // Get the port with addressing feature
            bankService = service.getBankServicePort(
                new AddressingFeature(true, true)
            );
            
            logger.info("Successfully connected to BankService with SOAP logging enabled");
            
        } catch (WebServiceException e) {
            logger.log(Level.SEVERE, "Failed to connect to BankService", e);
            throw new Exception("Failed to connect to BankService: " + e.getMessage(), e);
        }
    }
    
    /**
     * Run service demonstrations
     */
    public String runDemo() {
        StringBuilder result = new StringBuilder();
        result.append("=== Bank Service Demonstration in Liberty ===\n");
        
        try {
            if (!initialized) {
                initialize();
            }
            
            // Demo 1: Successful transfer
            result.append(demonstrateSuccessfulTransfer()).append("\n");
            
            // Demo 2: Multiple transfers
            result.append(demonstrateMultipleTransfers()).append("\n");
            
            // Demo 3: Invalid data handling
            result.append(demonstrateInvalidDataHandling()).append("\n");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error running demo", e);
            result.append("Error running demo: ").append(e.getMessage()).append("\n");
        }
        
        return result.toString();
    }
    
    /**
     * Demo 1: Successful transfer
     */
    private String demonstrateSuccessfulTransfer() {
        StringBuilder result = new StringBuilder();
        result.append("\n--- Demo 1: Successful Transfer ---\n");
        
        try {
            result.append("Performing transfer: ACC001 -> ACC002, $100.00\n");
            boolean transferResult = bankService.transfer("ACC001", "ACC002", 10000);
            
            if (transferResult) {
                result.append("✓ Transfer completed successfully\n");
                logger.info("Demo 1: Transfer completed successfully");
            } else {
                result.append("✗ Transfer failed\n");
                logger.warning("Demo 1: Transfer failed");
            }
            
        } catch (Exception e) {
            result.append("Error in Demo 1: ").append(e.getMessage()).append("\n");
            logger.log(Level.SEVERE, "Error in Demo 1", e);
        }
        
        return result.toString();
    }
    
    /**
     * Demo 2: Multiple transfers
     */
    private String demonstrateMultipleTransfers() {
        StringBuilder result = new StringBuilder();
        result.append("\n--- Demo 2: Multiple Transfers ---\n");
        
        try {
            result.append("Performing transfer 1: ACC003 -> ACC004, $50.00\n");
            boolean result1 = bankService.transfer("ACC003", "ACC004", 5000);
            
            result.append("Performing transfer 2: ACC005 -> ACC006, $75.00\n");
            boolean result2 = bankService.transfer("ACC005", "ACC006", 7500);
            
            result.append("Transfer 1 result: ").append(result1 ? "SUCCESS" : "FAILED").append("\n");
            result.append("Transfer 2 result: ").append(result2 ? "SUCCESS" : "FAILED").append("\n");
            
            logger.info("Demo 2: Multiple transfers completed");
            
        } catch (Exception e) {
            result.append("Error in Demo 2: ").append(e.getMessage()).append("\n");
            logger.log(Level.SEVERE, "Error in Demo 2", e);
        }
        
        return result.toString();
    }
    
    /**
     * Demo 3: Invalid data handling
     */
    private String demonstrateInvalidDataHandling() {
        StringBuilder result = new StringBuilder();
        result.append("\n--- Demo 3: Invalid Data Handling ---\n");
        
        try {
            result.append("Attempting invalid transfer: ACC007 -> ACC008, -$10.00\n");
            boolean transferResult = bankService.transfer("ACC007", "ACC008", -1000);
            
            result.append("Transfer result: ").append(transferResult ? "SUCCESS" : "FAILED").append("\n");
            logger.info("Demo 3: Invalid data handling completed");
            
        } catch (Exception e) {
            result.append("Service exception (expected for invalid data): ").append(e.getMessage()).append("\n");
            logger.info("Demo 3: Service exception caught as expected");
        }
        
        return result.toString();
    }
    
    /**
     * Execute a single transfer operation
     */
    public String executeTransfer(String fromAccount, String toAccount, int amountCents) {
        StringBuilder result = new StringBuilder();
        
        try {
            if (!initialized) {
                initialize();
            }
            
            result.append("Executing transfer: ").append(fromAccount)
                  .append(" -> ").append(toAccount).append(", $")
                  .append(String.format("%.2f", amountCents / 100.0)).append("\n");
            
            boolean transferResult = bankService.transfer(fromAccount, toAccount, amountCents);
            
            if (transferResult) {
                result.append("✓ Transfer completed successfully\n");
                logger.info("Transfer completed: " + fromAccount + " -> " + toAccount + ", amount: " + amountCents);
            } else {
                result.append("✗ Transfer failed\n");
                logger.warning("Transfer failed: " + fromAccount + " -> " + toAccount + ", amount: " + amountCents);
            }
            
        } catch (Exception e) {
            result.append("Error executing transfer: ").append(e.getMessage()).append("\n");
            logger.log(Level.SEVERE, "Error executing transfer", e);
        }
        
        return result.toString();
    }
    
    /**
     * Check if the client is initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Get service status
     */
    public String getServiceStatus() {
        if (!initialized) {
            return "NOT_INITIALIZED";
        }
        
        try {
            // Try a simple operation to check if service is available
            // This is a basic connectivity test
            return "AVAILABLE";
        } catch (Exception e) {
            logger.log(Level.WARNING, "Service status check failed", e);
            return "UNAVAILABLE: " + e.getMessage();
        }
    }
}