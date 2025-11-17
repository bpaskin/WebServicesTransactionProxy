package com.ibm.webservices.proxy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;
import java.util.logging.Logger;
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

/**
 * Configuration class for the Web Services Proxy
 *
 * This class manages configuration settings for the proxy including:
 * - SSL settings
 * - Logging levels
 * - Header filtering rules
 *
 * Note: Destination URLs are extracted from SOAP message To field or HTTP headers,
 * not from configuration.
 */
@ApplicationScoped
public class ProxyConfiguration {
    
    private static final Logger logger = Logger.getLogger(ProxyConfiguration.class.getName());
    
    // Default configuration values
    private boolean removeCoordinationContext = true;
    private boolean removeWSATElements = true;
    private boolean removeTransactionElements = true;
    private boolean enableDetailedLogging = true;
    private int connectionTimeoutMs = 10000;
    private int socketTimeoutMs = 30000;
    
    @PostConstruct
    public void initialize() {
        loadConfiguration();
        logger.info("Proxy configuration initialized");
        logConfiguration();
    }
    
    /**
     * Load configuration from properties file if available
     */
    private void loadConfiguration() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("proxy.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                
                removeCoordinationContext = Boolean.parseBoolean(
                    props.getProperty("proxy.remove.coordination.context", "true"));
                removeWSATElements = Boolean.parseBoolean(
                    props.getProperty("proxy.remove.wsat.elements", "true"));
                removeTransactionElements = Boolean.parseBoolean(
                    props.getProperty("proxy.remove.transaction.elements", "true"));
                
                enableDetailedLogging = Boolean.parseBoolean(
                    props.getProperty("proxy.logging.detailed", "true"));
                
                connectionTimeoutMs = Integer.parseInt(
                    props.getProperty("proxy.connection.timeout.ms", "10000"));
                socketTimeoutMs = Integer.parseInt(
                    props.getProperty("proxy.socket.timeout.ms", "30000"));
                
                logger.info("Configuration loaded from proxy.properties");
            } else {
                logger.info("No proxy.properties found, using default configuration");
            }
        } catch (IOException e) {
            logger.warning("Error loading configuration: " + e.getMessage());
        } catch (NumberFormatException e) {
            logger.warning("Invalid number format in configuration: " + e.getMessage());
        }
    }
    
    /**
     * Log current configuration
     */
    private void logConfiguration() {
        logger.info("=== Proxy Configuration ===");
        logger.info("Remove CoordinationContext: " + removeCoordinationContext);
        logger.info("Remove WS-AT Elements: " + removeWSATElements);
        logger.info("Remove Transaction Elements: " + removeTransactionElements);
        logger.info("Detailed Logging: " + enableDetailedLogging);
        logger.info("Connection Timeout: " + connectionTimeoutMs + "ms");
        logger.info("Socket Timeout: " + socketTimeoutMs + "ms");
        logger.info("===========================");
    }
    
    // Getters
    public boolean isRemoveCoordinationContext() {
        return removeCoordinationContext;
    }
    
    public boolean isRemoveWSATElements() {
        return removeWSATElements;
    }
    
    public boolean isRemoveTransactionElements() {
        return removeTransactionElements;
    }
    
    public boolean isEnableDetailedLogging() {
        return enableDetailedLogging;
    }
    
    public int getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }
    
    public int getSocketTimeoutMs() {
        return socketTimeoutMs;
    }
    
    // Setters for runtime configuration changes
    public void setRemoveCoordinationContext(boolean removeCoordinationContext) {
        this.removeCoordinationContext = removeCoordinationContext;
        logger.info("Remove CoordinationContext updated to: " + removeCoordinationContext);
    }
    
    public void setRemoveWSATElements(boolean removeWSATElements) {
        this.removeWSATElements = removeWSATElements;
        logger.info("Remove WS-AT Elements updated to: " + removeWSATElements);
    }
    
    public void setRemoveTransactionElements(boolean removeTransactionElements) {
        this.removeTransactionElements = removeTransactionElements;
        logger.info("Remove Transaction Elements updated to: " + removeTransactionElements);
    }
    
    public void setEnableDetailedLogging(boolean enableDetailedLogging) {
        this.enableDetailedLogging = enableDetailedLogging;
        logger.info("Detailed logging updated to: " + enableDetailedLogging);
    }
}