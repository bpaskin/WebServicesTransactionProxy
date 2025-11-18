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
    private boolean allowRestrictedHeaders = false;
    private int connectionTimeoutMs = 10000;
    private int socketTimeoutMs = 30000;
    
    @PostConstruct
    public void initialize() {
        loadConfiguration();
        logger.info("Proxy configuration initialized");
        logConfiguration();
    }
    
    /**
     * Load configuration from properties file and system properties
     * System properties take precedence over properties file values
     */
    private void loadConfiguration() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("proxy.properties")) {
            Properties props = new Properties();
            
            if (is != null) {
                props.load(is);
                logger.info("Configuration loaded from proxy.properties");
            } else {
                logger.info("No proxy.properties found, using default configuration");
            }
            
            // Load configuration with system property override support
            removeCoordinationContext = getBooleanProperty(
                "proxy.remove.coordination.context",
                props.getProperty("proxy.remove.coordination.context", "true"));
                
            removeWSATElements = getBooleanProperty(
                "proxy.remove.wsat.elements",
                props.getProperty("proxy.remove.wsat.elements", "true"));
                
            removeTransactionElements = getBooleanProperty(
                "proxy.remove.transaction.elements",
                props.getProperty("proxy.remove.transaction.elements", "true"));
            
            enableDetailedLogging = getBooleanProperty(
                "proxy.logging.detailed",
                props.getProperty("proxy.logging.detailed", "true"));
            
            allowRestrictedHeaders = getBooleanProperty(
                "proxy.allow.restricted.headers",
                props.getProperty("proxy.allow.restricted.headers", "true"));
            
            connectionTimeoutMs = getIntegerProperty(
                "proxy.connection.timeout.ms",
                props.getProperty("proxy.connection.timeout.ms", "10000"));
                
            socketTimeoutMs = getIntegerProperty(
                "proxy.socket.timeout.ms",
                props.getProperty("proxy.socket.timeout.ms", "30000"));
                
            // Log which properties were overridden by system properties
            logSystemPropertyOverrides();
            
        } catch (IOException e) {
            logger.warning("Error loading configuration: " + e.getMessage());
        } catch (NumberFormatException e) {
            logger.warning("Invalid number format in configuration: " + e.getMessage());
        }
    }
    
    /**
     * Get boolean property with system property override support
     */
    private boolean getBooleanProperty(String systemPropertyName, String defaultValue) {
        String systemValue = System.getProperty(systemPropertyName);
        if (systemValue != null) {
            logger.info("Using system property override: " + systemPropertyName + "=" + systemValue);
            return Boolean.parseBoolean(systemValue);
        }
        return Boolean.parseBoolean(defaultValue);
    }
    
    /**
     * Get integer property with system property override support
     */
    private int getIntegerProperty(String systemPropertyName, String defaultValue) {
        String systemValue = System.getProperty(systemPropertyName);
        if (systemValue != null) {
            logger.info("Using system property override: " + systemPropertyName + "=" + systemValue);
            return Integer.parseInt(systemValue);
        }
        return Integer.parseInt(defaultValue);
    }
    
    /**
     * Log information about system property overrides
     */
    private void logSystemPropertyOverrides() {
        String[] propertyNames = {
            "proxy.remove.coordination.context",
            "proxy.remove.wsat.elements",
            "proxy.remove.transaction.elements",
            "proxy.logging.detailed",
            "proxy.allow.restricted.headers",
            "proxy.connection.timeout.ms",
            "proxy.socket.timeout.ms"
        };
        
        boolean hasOverrides = false;
        for (String propertyName : propertyNames) {
            if (System.getProperty(propertyName) != null) {
                hasOverrides = true;
                break;
            }
        }
        
        if (hasOverrides) {
            logger.info("=== System Property Overrides Detected ===");
            for (String propertyName : propertyNames) {
                String systemValue = System.getProperty(propertyName);
                if (systemValue != null) {
                    logger.info("System property: " + propertyName + "=" + systemValue);
                }
            }
            logger.info("==========================================");
        }
    }
    
    /**
     * Log current configuration
     */
    private void logConfiguration() {
        logger.info("=== Proxy Configuration ===");
        logger.info("Remove CoordinationContext: " + removeCoordinationContext + getPropertySource("proxy.remove.coordination.context"));
        logger.info("Remove WS-AT Elements: " + removeWSATElements + getPropertySource("proxy.remove.wsat.elements"));
        logger.info("Remove Transaction Elements: " + removeTransactionElements + getPropertySource("proxy.remove.transaction.elements"));
        logger.info("Detailed Logging: " + enableDetailedLogging + getPropertySource("proxy.logging.detailed"));
        logger.info("Allow Restricted Headers: " + allowRestrictedHeaders + getPropertySource("proxy.allow.restricted.headers"));
        logger.info("Connection Timeout: " + connectionTimeoutMs + "ms" + getPropertySource("proxy.connection.timeout.ms"));
        logger.info("Socket Timeout: " + socketTimeoutMs + "ms" + getPropertySource("proxy.socket.timeout.ms"));
        logger.info("===========================");
    }
    
    /**
     * Get property source indicator for logging
     */
    private String getPropertySource(String propertyName) {
        return System.getProperty(propertyName) != null ? " (system property)" : "";
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
    
    public boolean isAllowRestrictedHeaders() {
        return allowRestrictedHeaders;
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
    
    public void setAllowRestrictedHeaders(boolean allowRestrictedHeaders) {
        this.allowRestrictedHeaders = allowRestrictedHeaders;
        logger.info("Allow restricted headers updated to: " + allowRestrictedHeaders);
    }
}