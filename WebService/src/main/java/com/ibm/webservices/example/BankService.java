package com.ibm.webservices.example;

import jakarta.ejb.Stateless;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.jws.HandlerChain;
import jakarta.xml.ws.soap.Addressing;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.xml.ws.soap.MTOM;

@WebService(
    name = "BankService",
    serviceName = "BankService",
    targetNamespace = "urn:example:bank",
    portName = "BankServicePort"
)
@HandlerChain(file = "/META-INF/handler-chain.xml")
@Stateless
public class BankService {

    @WebMethod
    public boolean transfer(String fromAccount, String toAccount, int cents) {
        if (cents <= 0) {
            throw new IllegalArgumentException("Amount must be > 0");
        }
        return true;
    }
}