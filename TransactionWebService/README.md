# WS-AT Account Web Service

This project implements a Web Services Atomic Transaction (WS-AT) enabled web service based on the AccountService. It provides distributed transaction support for account operations using JAX-WS and Liberty server.

## Features

- **WS-AT Support**: Full Web Services Atomic Transaction support for distributed transactions
- **Account Management**: Create, find, and manage accounts
- **Transaction Operations**: Deposit, withdraw, and transfer with ACID properties
- **Liberty Server**: Optimized for IBM Liberty server with WS-AT features
- **JAX-WS**: Standard Java web services implementation

## Project Structure

```
TransactionWebService/
├── src/main/java/com/example/
│   ├── entity/
│   │   └── Account.java                    # Account entity
│   ├── service/
│   │   └── AccountService.java             # Business logic service
│   ├── webservice/
│   │   ├── AccountWebService.java          # Web service interface
│   │   └── AccountWebServiceImpl.java      # Web service implementation
│   └── client/
│       └── AccountWebServiceClient.java    # Test client
├── src/main/webapp/WEB-INF/
│   ├── web.xml                             # Web application configuration
│   └── wsdl/
│       └── AccountWebService.wsdl          # WSDL definition
├── src/main/liberty/config/
│   └── server.xml                          # Liberty server configuration
└── pom.xml                                 # Maven configuration
```

## Prerequisites

- Java 11 or higher
- Maven 3.6 or higher
- IBM Liberty Server (included via Maven plugin)

## Building the Project

1. **Clean and compile:**
   ```bash
   mvn clean compile
   ```

2. **Package the WAR file:**
   ```bash
   mvn package
   ```

## Deploying to Liberty Server

1. **Start Liberty server with WS-AT features:**
   ```bash
   mvn liberty:start
   ```

2. **Deploy the application:**
   ```bash
   mvn liberty:deploy
   ```

3. **Verify deployment:**
   - Server will be available at: `http://localhost:9080`
   - WSDL will be available at: `http://localhost:9080/TransactionWebService/AccountWebService?wsdl`

## WS-AT Configuration

The Liberty server is configured with the following WS-AT features:

- **wsAtomicTransaction-1.2**: Core WS-AT support
- **jta-2.0**: Java Transaction API
- **Transaction Timeout**: 120 seconds
- **Recovery Log**: Enabled for transaction recovery
- **Coordinator URL**: `http://localhost:9080/ws/wsatCoordinator`
- **Participant URL**: `http://localhost:9080/ws/wsatParticipant`

## Web Service Operations

### Account Management
- `createAccount(accountNumber, accountHolder, initialBalance)`: Create new account
- `findByAccountNumber(accountNumber)`: Find account by number
- `findAllAccounts()`: Get all accounts
- `getAccountCount()`: Get total account count

### Transaction Operations (WS-AT Enabled)
- `deposit(accountNumber, amount)`: Deposit money
- `withdraw(accountNumber, amount)`: Withdraw money
- `transfer(fromAccount, toAccount, amount)`: Transfer between accounts
- `getBalance(accountNumber)`: Get account balance

### Utility Operations
- `initializeTestData()`: Create test accounts
- `clearAllAccounts()`: Clear all accounts (testing)

## Testing the Web Service

### Using the Test Client

1. **Ensure the server is running:**
   ```bash
   mvn liberty:start
   ```

2. **Run the test client:**
   ```bash
   mvn exec:java -Dexec.mainClass="com.example.client.AccountWebServiceClient"
   ```

### Manual Testing with SOAP UI

1. Import the WSDL: `http://localhost:9080/TransactionWebService/AccountWebService?wsdl`
2. Create test requests for various operations
3. Verify WS-AT headers are included in requests

### Sample SOAP Request (Deposit)

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
               xmlns:web="http://webservice.example.com/">
   <soap:Header>
      <wsa:Action xmlns:wsa="http://www.w3.org/2005/08/addressing">
         http://webservice.example.com/AccountWebService/deposit
      </wsa:Action>
      <wsa:To xmlns:wsa="http://www.w3.org/2005/08/addressing">
         http://localhost:9080/TransactionWebService/AccountWebService
      </wsa:To>
   </soap:Header>
   <soap:Body>
      <web:depositRequest>
         <web:accountNumber>TEST001</web:accountNumber>
         <web:amount>100.00</web:amount>
      </web:depositRequest>
   </soap:Body>
</soap:Envelope>
```

## WS-AT Transaction Flow

1. **Client initiates transaction** with WS-AT coordinator
2. **Web service participates** in the distributed transaction
3. **Business logic executes** within transaction context
4. **Two-phase commit** ensures ACID properties:
   - **Prepare phase**: All participants vote to commit/abort
   - **Commit phase**: Coordinator directs final outcome

## Monitoring and Logging

### Server Logs
- Location: `target/liberty/wlp/usr/servers/ws-at-server/logs/`
- Key files:
  - `messages.log`: General server messages
  - `trace.log`: Detailed WS-AT transaction traces

### WS-AT Specific Logging
The server is configured to log WS-AT activities:
```xml
<logging traceSpecification="*=info:com.example.*=all:com.ibm.ws.wsat.*=all"/>
```

## Troubleshooting

### Common Issues

1. **Port conflicts**: Ensure ports 9080 and 9443 are available
2. **WS-AT not working**: Verify `wsAtomicTransaction-1.2` feature is enabled
3. **Transaction timeouts**: Adjust timeout values in `server.xml`
4. **Recovery issues**: Check recovery log directory permissions

### Useful Commands

```bash
# View server status
mvn liberty:status

# Stop server
mvn liberty:stop

# View logs
tail -f target/liberty/wlp/usr/servers/ws-at-server/logs/messages.log

# Clean and restart
mvn clean liberty:stop liberty:start
```

## Development Notes

### Adding New Operations

1. Add method to `AccountWebService` interface
2. Implement in `AccountWebServiceImpl`
3. Add `@Transactional` for WS-AT participation
4. Update WSDL if needed
5. Redeploy application

### Transaction Best Practices

- Keep transactions short-lived
- Handle exceptions properly to ensure rollback
- Use appropriate isolation levels
- Monitor transaction logs for issues

## Security Considerations

- Configure SSL/TLS for production
- Implement authentication/authorization
- Secure WS-AT coordinator endpoints
- Monitor for transaction replay attacks

## Production Deployment

1. **Configure SSL**: Update `server.xml` with proper certificates
2. **Database Integration**: Replace in-memory storage with persistent database
3. **Clustering**: Configure multiple Liberty instances for high availability
4. **Monitoring**: Set up application performance monitoring
5. **Security**: Implement proper authentication and authorization

## References

- [IBM Liberty WS-AT Documentation](https://www.ibm.com/docs/en/was-liberty/base?topic=liberty-configuring-ws-atomic-transaction-support)
- [JAX-WS Specification](https://jakarta.ee/specifications/xml-web-services/)
- [WS-AT Specification](http://docs.oasis-open.org/ws-tx/wsat/2006/06)