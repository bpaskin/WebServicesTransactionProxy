package com.ibm.webservices.example.client;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.Set;
import java.util.HashSet;

/**
 * JAX-RS Application configuration for the WS-AT Transaction Client
 */
@ApplicationPath("/api")
public class TransactionClientApplication extends Application {
    
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(TransactionClientResource.class);
        return classes;
    }
}