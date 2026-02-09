# Automatic Labeling Implementation Guide

## Overview
This document describes the complete implementation of automatic labeling rules for the Payment Labeling Engine. Users can now create regex-based rules that automatically assign labels to payments based on any payment field.

## Features Implemented

### 1. **Labeling Rules Management**
- **Create Rules**: Add new labeling rules with custom regex patterns
- **Edit Rules**: Update existing rules
- **Delete Rules**: Remove rules that are no longer needed
- **Activate/Deactivate**: Control which rules are active
- **Field Selection**: Choose which payment field to match against (Counterparty Name, Reference, Transaction Type, Bank, Account, etc.)

### 2. **Pattern Testing**
- **Real-time Testing**: Test regex patterns before saving
- **Instant Feedback**: See if a pattern matches sample text
- **Regex Validation**: Validate pattern syntax

### 3. **Automatic Labeling Application**
- **Apply All Rules**: Apply all active rules to all payments with one click
- **Selective Application**: Apply specific rules to selected payments
- **Batch Processing**: Process all payments in one operation
- **Results Reporting**: See how many labels were applied

## Database Changes

### New Migration: V5__Add_matching_field_to_labeling_rules.sql
Adds a new column to store which payment field each rule should match against:
```sql
ALTER TABLE labeling_rules ADD COLUMN matching_field VARCHAR(50) NOT NULL DEFAULT 'counterpartyName';
```

## Model Updates

### LabelingRule Entity
New field added:
```java
@Column(name = "matching_field", nullable = false, length = 50)
private String matchingField;
```

**Supported Payment Fields for Matching:**
- `counterpartyName` - Name of the payment counterparty (default)
- `reference` - Payment reference number
- `transactionType` - Type of transaction
- `counterpartyBank` - Bank details
- `counterpartyAccount` - Account details
- `receiverInfo` - Receiver information
- `additionalInfo` - Additional payment information

## API Endpoints

### Rules Management

#### GET /api/rules
Get all labeling rules
```bash
curl http://localhost:8080/api/rules
```

#### GET /api/rules/active
Get only active rules
```bash
curl http://localhost:8080/api/rules/active
```

#### GET /api/rules/{id}
Get a specific rule by ID
```bash
curl http://localhost:8080/api/rules/1
```

#### POST /api/rules
Create a new rule
```bash
curl -X POST http://localhost:8080/api/rules \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Grocery Store Rule",
    "regexPattern": "(KAUFLAND|TESCO|WALMART)",
    "labelId": 1,
    "matchingField": "counterpartyName",
    "isActive": true,
    "description": "Matches grocery store purchases"
  }'
```

#### PUT /api/rules/{id}
Update an existing rule
```bash
curl -X PUT http://localhost:8080/api/rules/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Rule Name",
    "regexPattern": "(KAUFLAND|TESCO)",
    "labelId": 1,
    "matchingField": "counterpartyName",
    "isActive": true
  }'
```

#### DELETE /api/rules/{id}
Delete a rule
```bash
curl -X DELETE http://localhost:8080/api/rules/1
```

### Pattern Testing

#### POST /api/rules/test
Test a regex pattern against sample text
```bash
curl -X POST http://localhost:8080/api/rules/test \
  -H "Content-Type: application/json" \
  -d '{
    "regexPattern": "(KAUFLAND|TESCO)",
    "fieldValue": "KAUFLAND 8820 BA"
  }'
```

Response:
```json
{
  "matches": true,
  "pattern": "(KAUFLAND|TESCO)",
  "value": "KAUFLAND 8820 BA"
}
```

### Automatic Labeling

#### POST /api/rules/apply
Apply labeling rules to payments
```bash
curl -X POST http://localhost:8080/api/rules/apply \
  -H "Content-Type: application/json" \
  -d '{
    "paymentIds": [1, 2, 3],
    "ruleIds": [1, 2]
  }'
```

**Request Body Options:**
- `paymentIds`: Array of payment IDs to process (omit for all payments)
- `ruleIds`: Array of rule IDs to apply (omit for all active rules)

Response:
```json
{
  "success": true,
  "totalPaymentsProcessed": 150,
  "totalLabelsApplied": 245,
  "paymentLabelCounts": {
    "1": 2,
    "2": 1,
    "3": 0
  }
}
```

## User Interface

### Web Pages

#### /rules - Labeling Rules Management Page

**Two Main Tabs:**

1. **Manage Rules Tab**
   - View all rules in a table
   - Create new rules with a modal form
   - Edit existing rules
   - Delete rules
   - See rule status (active/inactive)
   - View pattern abbreviations and labels

2. **Apply Rules Tab**
   - Select which rules to apply (all active or specific)
   - Select which payments to process (all or specific)
   - View processing status and results
   - See statistics on labels applied

**Add Rule Modal Features:**
- Rule name (required)
- Description (optional)
- Matching field selector (required)
- Label selector (required)
- Regex pattern input with help link (required)
- Pattern testing functionality
- Active/Inactive toggle

**Edit Rule Modal Features:**
- Same as Add Rule Modal
- Pre-populated with existing rule data

## Regex Pattern Examples

### Common Use Cases

**Match Grocery Stores:**
```regex
(KAUFLAND|TESCO|WALMART|CARREFOUR|EDEKA|METRO)
```

**Match Parking Services:**
```regex
(PARKING|PARKHAUS|PARKING LOT|VALET)
```

**Match Utilities:**
```regex
(ELECTRIC|WATER|GAS|ENERGY|UTILITY|EWE|VATTENFALL)
```

**Match Restaurant Chains:**
```regex
(MC DONALD|BURGER KING|SUBWAY|STARBUCKS|PIZZA HUT)
```

**Match Airlines:**
```regex
(LUFTHANSA|RYANAIR|EASYJET|BRITISH AIRWAYS|UNITED AIRLINES)
```

## Implementation Details

### Files Created

1. **LabelingRuleController.java**
   - REST API endpoints for rule management
   - Pattern testing endpoint
   - Automatic labeling application endpoint

2. **V5__Add_matching_field_to_labeling_rules.sql**
   - Database migration for new column
   - Index creation for performance

3. **Updated rules/index.html**
   - Comprehensive UI for rule management
   - Tab-based interface
   - Modal forms for CRUD operations
   - JavaScript for API interactions

### Files Modified

1. **LabelingRule.java**
   - Added `matchingField` property
   - Updated builder pattern
   - Added getter/setter methods

2. **RuleWebController.java**
   - Added LabelingRuleService injection
   - Pass rules to template
   - Provide payment field options

### Key Features in Code

**Payment Field Matching:**
```java
private String getPaymentFieldValue(Payment payment, String fieldName) {
    return switch (fieldName) {
        case "counterpartyName" -> payment.getCounterpartyName();
        case "reference" -> payment.getReference();
        case "transactionType" -> payment.getTransactionType();
        // ... more fields
        default -> payment.getCounterpartyName();
    };
}
```

**Apply Rules Algorithm:**
1. Get list of payments to process (all or specific)
2. Get list of rules to apply (all active or specific)
3. For each payment:
   - For each rule:
     - Get the matching field value
     - Try to match regex pattern
     - If matches and label not already assigned:
       - Assign label to payment
       - Count the assignment
4. Return statistics

**Duplicate Prevention:**
- Before assigning a label, check if it's already assigned
- Avoid processing the same label-payment pair twice

## Usage Guide

### Step 1: Create a Labeling Rule
1. Go to `/rules`
2. Click "Add New Rule"
3. Fill in rule details:
   - Name: "Grocery Store Rule"
   - Matching Field: "Counterparty Name"
   - Label: Select "Grocery"
   - Pattern: `(KAUFLAND|TESCO)`
4. Test the pattern with sample text
5. Click "Save Rule"

### Step 2: Apply Rules to Payments
1. Go to `/rules`
2. Click "Apply Rules" tab
3. Select rules to apply:
   - "Apply all active rules" OR
   - Check specific rules
4. Select payments to process:
   - "Apply to all payments" OR
   - Select specific payments
5. Click "Apply Rules"
6. View results

### Step 3: View Labeled Payments
1. Go to `/payments`
2. Payments now have labels automatically assigned
3. Filter by label to see grouped payments

## Performance Considerations

- Rules are processed sequentially for each payment
- Index on `matching_field` improves query performance
- Consider the complexity of regex patterns
- Large batch processing happens in a single transaction

## Error Handling

- **Invalid Regex**: User is notified if pattern syntax is incorrect
- **Missing Required Fields**: Form validation prevents incomplete submissions
- **Label Not Found**: Error message if selected label doesn't exist
- **Pattern Mismatch**: No error, label simply won't be applied

## Security Notes

- All endpoints require the application to be running
- No specific authentication is implemented (can be added)
- Regex patterns are validated before use
- SQL injection protection via JPA parameterized queries

## Future Enhancements

- Batch import/export of rules
- Rule scheduling (apply automatically on schedule)
- Rule effectiveness statistics
- A/B testing different rule patterns
- Machine learning-based rule suggestions
- Multi-pattern rules (AND/OR logic)
- Rule versioning and history

## Troubleshooting

**Rules not applying?**
- Check if rule is marked as "Active"
- Verify regex pattern with test tool
- Ensure payment field contains data to match against
- Check browser console for JavaScript errors

**Pattern matching not working?**
- Test pattern in modal first
- Review regex syntax at regex101.com
- Check if correct payment field is selected
- Remember patterns are case-sensitive (use flags for case-insensitive)

**Labels not appearing?**
- Refresh the page
- Check database for actual saved labels
- Verify PaymentLabel entries in database

