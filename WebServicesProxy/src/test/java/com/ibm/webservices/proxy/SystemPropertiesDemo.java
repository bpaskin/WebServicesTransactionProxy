package com.ibm.webservices.proxy;

/**
 * Demonstration class to verify system property override functionality
 * 
 * This class shows how system properties can be used to override
 * configuration values at runtime.
 * 
 * Usage examples:
 * - java -Dproxy.allow.restricted.headers=true -cp ... SystemPropertiesDemo
 * - java -Dproxy.remove.coordination.context=false -cp ... SystemPropertiesDemo
 */
public class SystemPropertiesDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Web Services Proxy System Properties Demo ===");
        
        SystemPropertiesDemo demo = new SystemPropertiesDemo();
        
        // Test 1: Default configuration
        System.out.println("\n--- Test 1: Default Configuration ---");
        demo.testDefaultConfiguration();
        
        // Test 2: System property overrides
        System.out.println("\n--- Test 2: System Property Overrides ---");
        demo.testSystemPropertyOverrides();
        
        // Test 3: Runtime property changes
        System.out.println("\n--- Test 3: Runtime Property Changes ---");
        demo.testRuntimePropertyChanges();
        
        // Test 4: Show examples
        demonstrateSystemProperties();
        
        System.out.println("\n=== Demo Complete ===");
        System.out.println("To test system property overrides, run with:");
        System.out.println("java -Dproxy.allow.restricted.headers=true -Dproxy.connection.timeout.ms=15000 SystemPropertiesDemo");
    }
    
    /**
     * Test default configuration loading
     */
    private void testDefaultConfiguration() {
        ProxyConfiguration config = new ProxyConfiguration();
        config.initialize();
        
        System.out.println("Configuration loaded with default/properties file values:");
        printConfiguration(config);
    }
    
    /**
     * Test system property overrides
     */
    private void testSystemPropertyOverrides() {
        // Set some system properties for demonstration
        System.setProperty("proxy.allow.restricted.headers", "false");
        System.setProperty("proxy.connection.timeout.ms", "25000");
        System.setProperty("proxy.logging.detailed", "false");
        
        ProxyConfiguration config = new ProxyConfiguration();
        config.initialize();
        
        System.out.println("Configuration with system property overrides:");
        System.out.println("  - proxy.allow.restricted.headers=false");
        System.out.println("  - proxy.connection.timeout.ms=25000");
        System.out.println("  - proxy.logging.detailed=false");
        printConfiguration(config);
        
        // Clean up
        System.clearProperty("proxy.allow.restricted.headers");
        System.clearProperty("proxy.connection.timeout.ms");
        System.clearProperty("proxy.logging.detailed");
    }
    
    /**
     * Test runtime property changes
     */
    private void testRuntimePropertyChanges() {
        ProxyConfiguration config = new ProxyConfiguration();
        config.initialize();
        
        System.out.println("Original configuration:");
        printConfiguration(config);
        
        // Change configuration at runtime
        config.setAllowRestrictedHeaders(false);
        config.setRemoveCoordinationContext(false);
        config.setEnableDetailedLogging(false);
        
        System.out.println("\nAfter runtime changes:");
        printConfiguration(config);
    }
    
    /**
     * Print current configuration values
     */
    private void printConfiguration(ProxyConfiguration config) {
        System.out.println("  Remove CoordinationContext: " + config.isRemoveCoordinationContext());
        System.out.println("  Remove WS-AT Elements: " + config.isRemoveWSATElements());
        System.out.println("  Remove Transaction Elements: " + config.isRemoveTransactionElements());
        System.out.println("  Detailed Logging: " + config.isEnableDetailedLogging());
        System.out.println("  Allow Restricted Headers: " + config.isAllowRestrictedHeaders());
        System.out.println("  Connection Timeout: " + config.getConnectionTimeoutMs() + "ms");
        System.out.println("  Socket Timeout: " + config.getSocketTimeoutMs() + "ms");
    }
    
    /**
     * Demonstrate specific system property scenarios
     */
    public static void demonstrateSystemProperties() {
        System.out.println("\n=== System Property Examples ===");
        
        // Example 1: Allow restricted headers
        System.out.println("\nExample 1: Allow restricted headers");
        System.out.println("Command: java -Dproxy.allow.restricted.headers=true MyApp");
        System.out.println("Effect: Proxy will forward restricted headers like 'upgrade', 'connection'");
        
        // Example 2: Disable coordination context removal
        System.out.println("\nExample 2: Disable coordination context removal");
        System.out.println("Command: java -Dproxy.remove.coordination.context=false MyApp");
        System.out.println("Effect: CoordinationContext elements will NOT be removed from SOAP headers");
        
        // Example 3: Adjust timeouts
        System.out.println("\nExample 3: Adjust connection timeouts");
        System.out.println("Command: java -Dproxy.connection.timeout.ms=20000 -Dproxy.socket.timeout.ms=60000 MyApp");
        System.out.println("Effect: Connection timeout = 20s, Socket timeout = 60s");
        
        // Example 4: Disable detailed logging
        System.out.println("\nExample 4: Disable detailed logging");
        System.out.println("Command: java -Dproxy.logging.detailed=false MyApp");
        System.out.println("Effect: Reduces log verbosity for production environments");
        
        // Example 5: Multiple properties
        System.out.println("\nExample 5: Multiple properties");
        System.out.println("Command: java -Dproxy.allow.restricted.headers=true \\");
        System.out.println("              -Dproxy.remove.coordination.context=false \\");
        System.out.println("              -Dproxy.connection.timeout.ms=15000 MyApp");
        System.out.println("Effect: Multiple configuration overrides applied simultaneously");
    }
}