package com.ibm.webservices.example.client;

import com.ibm.webservices.example.client.generated.BankService;
import com.ibm.webservices.example.client.generated.BankService_Service;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.soap.AddressingFeature;
import jakarta.xml.ws.handler.Handler;
import jakarta.xml.ws.handler.HandlerResolver;
import jakarta.xml.ws.handler.PortInfo;
import jakarta.transaction.UserTransaction;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * WS-AT (Web Services Atomic Transaction) Client for BankService running in Liberty
 * 
 * This client demonstrates how to use distributed transactions with WS-AT
 * to ensure ACID properties across multiple web service calls in a Liberty environment.
 * 
 * Features:
 * - UserTransaction management for distributed transactions
 * - Proper transaction boundaries (begin/commit/rollback)
 * - Error handling for various transaction scenarios
 * - Multiple operations within a single transaction
 * - Transaction rollback on business logic failures
 * - Liberty-specific configuration and resource injection
 */
@ApplicationScoped
@Named("wsatClient")
public class WSATTransactionClient {
    
    private static final Logger logger = Logger.getLogger(WSATTransactionClient.class.getName());
    private static final String DEFAULT_WSDL_URL = "https://localhost:9444/TransactionWebService/BankService?wsdl";
    
    @Resource
    private UserTransaction userTransaction;
    
    private BankService bankService;
    private boolean initialized = false;
    
    /**
     * Initialize the transaction client
     */
    public void initialize() throws Exception {
        if (initialized) {
            return;
        }
        
        logger.info("=== Initializing WS-AT Transaction Client in Liberty ===");
        
        // Initialize UserTransaction if not injected
        initializeUserTransaction();
        
        // Create BankService proxy with WS-AT support
        createBankServiceProxy();
        
        initialized = true;
        logger.info("WS-AT Transaction Client initialized successfully in Liberty");
    }
    
    /**
     * Initialize UserTransaction for managing distributed transactions
     */
    private void initializeUserTransaction() throws NamingException {
        if (userTransaction == null) {
            try {
                // Try to get UserTransaction from JNDI in Liberty
                InitialContext ctx = new InitialContext();
                userTransaction = (UserTransaction) ctx.lookup("java:comp/UserTransaction");
                logger.info("UserTransaction obtained from JNDI in Liberty");
            } catch (NamingException e) {
                logger.severe("Failed to lookup UserTransaction from JNDI: " + e.getMessage());
                throw e;
            }
        } else {
            logger.info("UserTransaction injected successfully via @Resource");
        }
    }
    
    /**
     * Create BankService proxy with WS-AT and addressing features, including SOAP message logging
     */
    private void createBankServiceProxy() throws Exception {
        try {
            logger.info("Connecting to: " + DEFAULT_WSDL_URL);
            
            // Create the service with WS-Addressing required for WS-AT
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
            
            // Get the port with addressing feature for WS-AT support
            bankService = service.getBankServicePort(
                new AddressingFeature(true, true)
            );
            
            logger.info("Successfully connected to BankService with WS-AT support and SOAP logging enabled");
            
        } catch (WebServiceException e) {
            logger.log(Level.SEVERE, "Failed to connect to BankService", e);
            throw new Exception("Failed to connect to BankService: " + e.getMessage(), e);
        }
    }
    
    /**
     * Run comprehensive transaction demonstrations
     */
    public String runTransactionDemo() {
        StringBuilder result = new StringBuilder();
        result.append("=== WS-AT Transaction Demonstration in Liberty ===\n");
        
        try {
            if (!initialized) {
                initialize();
            }
            
            // Demo 1: Successful transaction with multiple operations
            result.append(demonstrateSuccessfulTransaction()).append("\n");
            
            // Demo 2: Transaction rollback due to business logic failure
            result.append(demonstrateTransactionRollback()).append("\n");
            
            // Demo 3: Transaction with invalid data (should rollback)
            result.append(demonstrateInvalidDataTransaction()).append("\n");
            
            // Demo 4: Multiple transactions in sequence
            result.append(demonstrateSequentialTransactions()).append("\n");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error running transaction demo", e);
            result.append("Error running transaction demo: ").append(e.getMessage()).append("\n");
        }
        
        return result.toString();
    }
    
    /**
     * Demo 1: Successful transaction with multiple operations
     */
    private String demonstrateSuccessfulTransaction() {
        StringBuilder result = new StringBuilder();
        result.append("\n--- Demo 1: Successful Multi-Operation Transaction ---\n");
        
        try {
            // Begin transaction
            userTransaction.begin();
            result.append("Transaction started\n");
            logger.info("Transaction started for Demo 1");
            
            // Perform multiple operations within the transaction
            result.append("Performing transfer 1: ACC001 -> ACC002, $100.00\n");
            boolean result1 = bankService.transfer("ACC001", "ACC002", 10000);
            
            result.append("Performing transfer 2: ACC003 -> ACC004, $50.00\n");
            boolean result2 = bankService.transfer("ACC003", "ACC004", 5000);
            
            if (result1 && result2) {
                // Commit transaction
                userTransaction.commit();
                result.append("✓ Transaction committed successfully - All transfers completed\n");
                logger.info("Demo 1: Transaction committed successfully");
            } else {
                // Rollback if any operation failed
                userTransaction.rollback();
                result.append("✗ Transaction rolled back - One or more transfers failed\n");
                logger.warning("Demo 1: Transaction rolled back due to failed transfers");
            }
            
        } catch (Exception e) {
            result.append(handleTransactionError("Demo 1", e));
        }
        
        return result.toString();
    }
    
    /**
     * Demo 2: Transaction rollback due to business logic failure
     */
    private String demonstrateTransactionRollback() {
        StringBuilder result = new StringBuilder();
        result.append("\n--- Demo 2: Transaction Rollback Scenario ---\n");
        
        try {
            // Begin transaction
            userTransaction.begin();
            result.append("Transaction started\n");
            logger.info("Transaction started for Demo 2");
            
            // First operation succeeds
            result.append("Performing transfer 1: ACC005 -> ACC006, $75.00\n");
            boolean result1 = bankService.transfer("ACC005", "ACC006", 7500);
            
            // Simulate business logic that determines transaction should be rolled back
            result.append("Performing transfer 2: ACC007 -> ACC008, $25.00\n");
            boolean result2 = bankService.transfer("ACC007", "ACC008", 2500);
            
            // Simulate a business rule violation (e.g., daily limit exceeded)
            boolean dailyLimitExceeded = true; // Simulated condition
            
            if (result1 && result2 && !dailyLimitExceeded) {
                userTransaction.commit();
                result.append("✓ Transaction committed successfully\n");
                logger.info("Demo 2: Transaction committed successfully");
            } else {
                userTransaction.rollback();
                result.append("✗ Transaction rolled back - Business rule violation (daily limit exceeded)\n");
                logger.info("Demo 2: Transaction rolled back due to business rule violation");
            }
            
        } catch (Exception e) {
            result.append(handleTransactionError("Demo 2", e));
        }
        
        return result.toString();
    }
    
    /**
     * Demo 3: Transaction with invalid data (should rollback)
     */
    private String demonstrateInvalidDataTransaction() {
        StringBuilder result = new StringBuilder();
        result.append("\n--- Demo 3: Invalid Data Transaction ---\n");
        
        try {
            // Begin transaction
            userTransaction.begin();
            result.append("Transaction started\n");
            logger.info("Transaction started for Demo 3");
            
            // Valid operation
            result.append("Performing valid transfer: ACC009 -> ACC010, $30.00\n");
            boolean result1 = bankService.transfer("ACC009", "ACC010", 3000);
            
            // Invalid operation (negative amount)
            result.append("Attempting invalid transfer: ACC011 -> ACC012, -$10.00\n");
            try {
                boolean result2 = bankService.transfer("ACC011", "ACC012", -1000);
                
                if (result1 && result2) {
                    userTransaction.commit();
                    result.append("✓ Transaction committed\n");
                    logger.info("Demo 3: Transaction committed");
                } else {
                    userTransaction.rollback();
                    result.append("✗ Transaction rolled back - Invalid operation detected\n");
                    logger.info("Demo 3: Transaction rolled back due to invalid operation");
                }
            } catch (Exception serviceException) {
                // Service threw exception for invalid data
                userTransaction.rollback();
                result.append("✗ Transaction rolled back - Service exception: ").append(serviceException.getMessage()).append("\n");
                logger.info("Demo 3: Transaction rolled back due to service exception");
            }
            
        } catch (Exception e) {
            result.append(handleTransactionError("Demo 3", e));
        }
        
        return result.toString();
    }
    
    /**
     * Demo 4: Multiple sequential transactions
     */
    private String demonstrateSequentialTransactions() {
        StringBuilder result = new StringBuilder();
        result.append("\n--- Demo 4: Sequential Transactions ---\n");
        
        // Transaction 1
        try {
            userTransaction.begin();
            result.append("Transaction 1 started\n");
            logger.info("Transaction 1 started for Demo 4");
            
            boolean txResult = bankService.transfer("ACC013", "ACC014", 2000);
            
            if (txResult) {
                userTransaction.commit();
                result.append("✓ Transaction 1 committed: $20.00 transferred\n");
                logger.info("Demo 4: Transaction 1 committed successfully");
            } else {
                userTransaction.rollback();
                result.append("✗ Transaction 1 rolled back\n");
                logger.warning("Demo 4: Transaction 1 rolled back");
            }
            
        } catch (Exception e) {
            result.append(handleTransactionError("Demo 4 - Transaction 1", e));
        }
        
        // Transaction 2
        try {
            userTransaction.begin();
            result.append("Transaction 2 started\n");
            logger.info("Transaction 2 started for Demo 4");
            
            boolean txResult = bankService.transfer("ACC015", "ACC016", 1500);
            
            if (txResult) {
                userTransaction.commit();
                result.append("✓ Transaction 2 committed: $15.00 transferred\n");
                logger.info("Demo 4: Transaction 2 committed successfully");
            } else {
                userTransaction.rollback();
                result.append("✗ Transaction 2 rolled back\n");
                logger.warning("Demo 4: Transaction 2 rolled back");
            }
            
        } catch (Exception e) {
            result.append(handleTransactionError("Demo 4 - Transaction 2", e));
        }
        
        return result.toString();
    }
    
    /**
     * Execute a single transfer operation within a transaction
     */
    public String executeTransfer(String fromAccount, String toAccount, int amountCents) {
        StringBuilder result = new StringBuilder();
        
        try {
            if (!initialized) {
                initialize();
            }
            
            userTransaction.begin();
            result.append("Transaction started for transfer: ").append(fromAccount)
                  .append(" -> ").append(toAccount).append(", $")
                  .append(String.format("%.2f", amountCents / 100.0)).append("\n");
            
            boolean transferResult = bankService.transfer(fromAccount, toAccount, amountCents);
            
            if (transferResult) {
                userTransaction.commit();
                result.append("✓ Transfer completed successfully\n");
                logger.info("Transfer completed: " + fromAccount + " -> " + toAccount + ", amount: " + amountCents);
            } else {
                userTransaction.rollback();
                result.append("✗ Transfer failed - Transaction rolled back\n");
                logger.warning("Transfer failed: " + fromAccount + " -> " + toAccount + ", amount: " + amountCents);
            }
            
        } catch (Exception e) {
            result.append(handleTransactionError("Single Transfer", e));
        }
        
        return result.toString();
    }
    
    /**
     * Get current transaction status
     */
    public String getTransactionStatus() {
        try {
            if (userTransaction == null) {
                return "UserTransaction not available";
            }
            
            int status = userTransaction.getStatus();
            return getTransactionStatusText(status);
        } catch (SystemException e) {
            logger.log(Level.WARNING, "Error getting transaction status", e);
            return "Error getting status: " + e.getMessage();
        }
    }
    
    private String getTransactionStatusText(int status) {
        switch (status) {
            case Status.STATUS_ACTIVE: return "ACTIVE";
            case Status.STATUS_COMMITTED: return "COMMITTED";
            case Status.STATUS_COMMITTING: return "COMMITTING";
            case Status.STATUS_MARKED_ROLLBACK: return "MARKED_ROLLBACK";
            case Status.STATUS_NO_TRANSACTION: return "NO_TRANSACTION";
            case Status.STATUS_PREPARED: return "PREPARED";
            case Status.STATUS_PREPARING: return "PREPARING";
            case Status.STATUS_ROLLEDBACK: return "ROLLEDBACK";
            case Status.STATUS_ROLLING_BACK: return "ROLLING_BACK";
            case Status.STATUS_UNKNOWN: return "UNKNOWN";
            default: return "UNDEFINED(" + status + ")";
        }
    }
    
    /**
     * Handle transaction errors with proper cleanup
     */
    private String handleTransactionError(String context, Exception e) {
        StringBuilder result = new StringBuilder();
        result.append("Error in ").append(context).append(": ").append(e.getMessage()).append("\n");
        logger.log(Level.SEVERE, "Error in " + context, e);
        
        try {
            if (userTransaction != null) {
                int status = userTransaction.getStatus();
                if (status == Status.STATUS_ACTIVE || status == Status.STATUS_MARKED_ROLLBACK) {
                    userTransaction.rollback();
                    result.append("✓ Transaction rolled back due to error\n");
                    logger.info("Transaction rolled back due to error in " + context);
                }
            }
        } catch (Exception rollbackException) {
            result.append("Failed to rollback transaction: ").append(rollbackException.getMessage()).append("\n");
            logger.log(Level.SEVERE, "Failed to rollback transaction", rollbackException);
        }
        
        return result.toString();
    }
    
    /**
     * Check if the client is initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
}