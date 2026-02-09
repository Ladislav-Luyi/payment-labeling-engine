# Quick Diagnostic Commands

## Test If Application Is Running

```bash
# Check if port 8080 is listening
netstat -ano | findstr :8080

# Try a simple API call
curl http://localhost:8080/api/health
```

## Start Application With Debug Output

```bash
cd C:\Users\r23r23\Desktop\work\payment-labeling-engine

# Option 1: Maven run with full output
mvn spring-boot:run -X

# Option 2: Maven run with Spring debug enabled
mvn spring-boot:run -D"spring.jpa.show-sql=true"

# Option 3: Build and run JAR
mvn clean package
java -jar target/payment-labeling-engine-*.jar
```

## Check Application Logs in Real-Time

While the application is running, watch the logs:

```bash
# Linux/Mac
tail -f logs/application.log | grep -i "error\|exception"

# Windows PowerShell
Get-Content logs/application.log -Tail 20 -Wait | Select-String "error|exception"
```

## Test Specific Endpoints

```bash
# Test health endpoint
curl http://localhost:8080/api/health

# Test rules/labels page
curl -v http://localhost:8080/rules

# Test label API
curl http://localhost:8080/api/labels

# Test with verbose output to see headers
curl -v http://localhost:8080/rules
```

## Check Database Connection

```bash
# PostgreSQL connection test
psql -h localhost -U payment_user -d payment_db -c "SELECT 1"

# Or with URI
psql "postgresql://payment_user:password@localhost:5432/payment_db" -c "SELECT 1"
```

## Capture Error Details

When accessing `/rules` in browser:

1. **Press F12** to open Developer Tools
2. **Go to Console tab** - look for JavaScript errors
3. **Go to Network tab** - click on the failed request
4. **Look at Response tab** - copy the full HTML response (the error message)
5. **Share this error message** - it will identify the exact problem

## Check if Services Are Initialized

Add this temporary endpoint to RuleWebController:

```java
@GetMapping("/debug")
@ResponseBody
public String debug() {
    return "LabelService: " + (labelService != null ? "OK" : "NULL") + "\n" +
           "RuleService: " + (labelingRuleService != null ? "OK" : "NULL") + "\n" +
           "Labels: " + (labelService != null ? labelService.getAllLabels().size() : "ERROR");
}
```

Then access: `http://localhost:8080/rules/debug`

## Enable Full Logging

Create/edit `src/main/resources/logback-spring.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <root level="DEBUG">
        <appender-ref ref="CONSOLE"/>
    </root>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    <logger name="com.paymentlabeling" level="DEBUG"/>
    <logger name="org.springframework.web" level="DEBUG"/>
    <logger name="org.thymeleaf" level="DEBUG"/>
</configuration>
```

## Verify All Files Are in Place

```bash
# Check key files exist
ls -la src/main/java/com/paymentlabeling/controller/RuleWebController.java
ls -la src/main/resources/templates/rules/index.html
ls -la src/main/resources/templates/base.html
ls -la src/main/java/com/paymentlabeling/service/LabelService*.java
```

## Next Step

Run one of these diagnostic commands and share the output. This will help identify the exact cause of the 500 error.

The most helpful would be:
1. Application startup output with debug logging
2. Full error response from browser (F12 → Network → Response tab)
3. Server logs showing the exception stack trace

