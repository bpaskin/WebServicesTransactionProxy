package com.ibm.webservices.example;

import jakarta.ejb.Stateless;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.xml.ws.soap.Addressing;
import jakarta.transaction.Transactional;
import jakarta.xml.ws.soap.MTOM;

@WebService(
    name = "BankService",
    serviceName = "BankService",
    targetNamespace = "urn:example:bank",
    portName = "BankServicePort",
    wsdlLocation = "WEB-INF/wsdl/BankService.wsdl"
)
@Addressing(enabled = true, required = true)
@MTOM(enabled = false)
@Stateless
public class BankService {

    @WebMethod
    @Transactional(Transactional.TxType.MANDATORY) 
    public boolean transfer(String fromAccount, String toAccount, int cents) {
        if (cents <= 0) {
            throw new IllegalArgumentException("Amount must be > 0");
        }
        return true;
    }
}