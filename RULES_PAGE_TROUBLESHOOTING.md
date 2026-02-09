# /rules Page 500 Error - Advanced Troubleshooting Guide

## Problem
The `/rules` endpoint is still returning HTTP 500 errors despite previous fixes.

## Diagnostic Steps

### Step 1: Check Application Health
Test if the application is running at all:

```bash
curl http://localhost:8080/api/health
```

Expected response: `OK`

If you get a connection refused error, the application is not running.

### Step 2: Check Service Availability
Test if basic services are responding:

```bash
curl http://localhost:8080/api/test
```

Expected response: `{"status": "application running"}`

### Step 3: Check Server Logs
When accessing `/rules`, watch the server console for error messages:

```
mvn spring-boot:run 2>&1 | grep -i "error\|exception"
```

### Step 4: Check Database Connection
The 500 error might be due to database connectivity. Verify:
1. PostgreSQL is running
2. Database credentials in `application.yml` are correct
3. Database migrations have completed

### Step 5: Test API Endpoints Directly
Test the REST APIs that the page depends on:

```bash
# Test label API
curl http://localhost:8080/api/labels
curl -X POST http://localhost:8080/api/labels \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","description":"Test"}'
```

## Common Causes & Solutions

### Cause 1: Application Not Running
**Symptoms**: Connection refused error

**Solution**:
```bash
cd C:\Users\r23r23\Desktop\work\payment-labeling-engine
mvn spring-boot:run
```

### Cause 2: Database Connection Failure
**Symptoms**: In logs you see "Unable to acquire JDBC Connection"

**Solution**:
1. Verify PostgreSQL is running
2. Check `application.yml` database credentials
3. Verify database exists and migrations have run

### Cause 3: Template File Not Found
**Symptoms**: In logs you see "Template not found"

**Solution**:
Verify file exists: `src/main/resources/templates/rules/index.html`

### Cause 4: Service Autowiring Issue
**Symptoms**: In logs you see "No qualifying bean found"

**Solution**:
Ensure services are properly annotated:
- `@Service` annotation on implementation class
- Implements the service interface
- All dependencies are autowired

### Cause 5: Thymeleaf Parsing Error
**Symptoms**: In logs you see "Thymeleaf parsing error"

**Solution**:
1. Check `rules/index.html` for syntax errors
2. Check `base.html` exists and is valid
3. Verify all `th:` attributes are correct

## Debugging Checklist

- [ ] Application is running (check logs)
- [ ] Database is connected (check logs)
- [ ] All migrations have executed
- [ ] LabelService is not null
- [ ] LabelingRuleService is not null
- [ ] Template file exists
- [ ] Base template exists and is valid
- [ ] No Thymeleaf parsing errors
- [ ] No null pointer exceptions in logs

## Enable Debug Logging

Add to `application.yml`:

```yaml
logging:
  level:
    root: INFO
    com.paymentlabeling: DEBUG
    org.springframework: DEBUG
    org.springframework.web: DEBUG
    org.thymeleaf: DEBUG
```

Then restart the application and check logs for detailed debugging information.

## Alternative Verification

### Check if template renders at all
Create a minimal test template:

Create file: `src/main/resources/templates/test.html`

```html
<!DOCTYPE html>
<html>
<head>
    <title>Test Page</title>
</head>
<body>
    <h1>Test Page - Application is running!</h1>
</body>
</html>
```

Create endpoint in RuleWebController:
```java
@GetMapping("/test")
public String test() {
    return "test";
}
```

Try accessing: `http://localhost:8080/rules/test`

If this works, the issue is with `rules/index.html`.

## Next Steps

1. Run the application with debug logging enabled
2. Access `/rules` and capture full error message
3. Share the complete error stack trace
4. Check server logs for root cause
5. Fix the specific issue identified

## Quick Fix: Minimal Rules Page

If the current template is too complex, here's a minimal version that should work:

**File**: `src/main/resources/templates/rules/index-minimal.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Rules</title>
</head>
<body>
    <h1>Labeling Rules</h1>
    <p th:text="'Total Labels: ' + ${#lists.size(labels)}"></p>
    <p th:text="'Total Rules: ' + ${#lists.size(rules)}"></p>
</body>
</html>
```

Then change RuleWebController to return this minimal template to test if the problem is the template or the controller.

---

## Support

If you're still getting 500 errors:

1. **Capture the full error message** from browser developer tools (F12 → Network tab)
2. **Capture the server log output** when you access `/rules`
3. **Check database logs** for connection errors
4. **Share both** and we can pinpoint the exact issue

The error is likely in one of these areas:
- Application startup/initialization
- Database connectivity
- Service autowiring
- Template rendering
- Spring configuration

