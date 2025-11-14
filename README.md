## Web Services with WS-AT (Atomic Transaction)

WS-AtomicTransaction (WS-AT) is a Web Services specification that enables distributed transactions across multiple services using a two-phase commit protocol. It lets independent, remote web services participate in a single, unified atomic transaction—meaning either all operations across those services commit, or all roll back—ensuring consistency even when they run on different platforms or vendors. WS-AT relies on WS-Coordination and WS-Policy to advertise and coordinate transactional capabilities, allowing enterprise systems to safely perform multi-step, cross-service business operations with strong ACID guarantees.

---

These examples show how to use Web Services with WS-AT at the client and server level, and how a client can remove the WS-AT header elements to allow a non transactional web services to be included in transactional web services.  Normally all web services would be part of a global transaction, however, in some instances the web service that is being called does not need to be part of the transaction or does not support WS-AT.  Sending a transaction to a web service that does not support WS-AT will result in a SOAP Fault.

---
1. **TransactionWebService** is a Web Service that uses WS-AT and works with the **BankServiceClientWSAT** client.
2. **WebService** is a plain Web Service that does not implement WS-AT and works with **BankServiceClient** and **BankServiceClientWSATRemove**
3. **BankServiceClientWSATRemove** uses a handler to remove the WS-AT headers to communicate directly with a non WS-AT service.
4. **RemoveWSATHandler** is a SOAPHandler to remove the WS-AT headers before the message is sent.

---

To use the SOAP Handler place the library in Liberty's `${server_config_dir}/lib/global` or similar directory.  To include in the code do the following:

- When using a Class with `@WebServiceClient` annotation, then the folliwng must be done:
  1. Annotate the Class with `@HandlerChain(file="/META-INF/handler-chain.xml")`
  2. Add the following `handler-chain.xml` file under the `/META-INF/` directory
     ```
     <?xml version="1.0" encoding="UTF-8"?>
      <handler-chains xmlns="http://java.sun.com/xml/ns/javaee"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://java.sun.com/xml/ns/javaee
                http://java.sun.com/xml/ns/javaee/javaee_web_services_1_2.xsd">
        <handler-chain>
          <handler>
              <handler-name>RemoveWSATHandler</handler-name>
              <handler-class>com.ibm.webservices.example.RemoveWSATHandler</handler-class>
          </handler>
        </handler-chain>
      </handler-chains>
     ```

- When programmatically calling the Web Service do similar to the following:
  ```
  BindingProvider bp = (BindingProvider) sei;
  List<Handler> handlerChain = bp.getBinding().getHandlerChain();
  handlerChain.add(new RemoveWSATHandler());
  bp.getBinding().setHandlerChain(handlerChain);
  ```
