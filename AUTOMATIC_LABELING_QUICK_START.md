# Automatic Labeling - Quick Start Guide

## Overview
The automatic labeling feature allows you to create rules that automatically assign labels to payments based on regex pattern matching against any payment field (counterparty name, reference, transaction type, etc.).

## 🎯 5-Minute Setup

### Step 1: Access Rules Page
Navigate to: **http://localhost:8080/rules**

You'll see two tabs:
- **Manage Rules** - Create, edit, delete labeling rules
- **Apply Rules** - Apply rules to payments

### Step 2: Create Your First Rule
1. Click **"Add New Rule"** button
2. Fill in the form:
   - **Rule Name**: `Grocery Stores`
   - **Matching Field**: `Counterparty Name`
   - **Label**: Select "Grocery" (or create one first)
   - **Regex Pattern**: `(KAUFLAND|TESCO|WALMART)`
   - **Description**: `Matches common grocery store chains`
3. Click **"Test"** to verify pattern works
4. Click **"Save Rule"**

### Step 3: Apply Rules to Payments
1. Go to **Apply Rules** tab
2. Leave defaults:
   - "Apply all active rules"
   - "Apply to all payments"
3. Click **"Apply Rules"**
4. See results showing how many labels were applied

### Step 4: Verify Results
1. Go to **Payments** page
2. Filter by label "Grocery"
3. See all payments matched by your rule

---

## 📋 Common Use Cases & Patterns

### Grocery Stores
```regex
(KAUFLAND|TESCO|WALMART|CARREFOUR|EDEKA|METRO|AHOLD|INGRESO)
```

### Parking Services
```regex
(PARKING|PARKHAUS|PARKING LOT|VALET|PARKWAY|PARKING GARAGE)
```

### Utilities & Energy
```regex
(ELECTRIC|WATER|GAS|ENERGY|UTILITY|EWE|VATTENFALL|ENBW|STROM)
```

### Restaurants & Cafes
```regex
(RESTAURANT|MCDONALDS|BURGER KING|SUBWAY|STARBUCKS|PIZZA HUT|CAFE)
```

### Airlines & Travel
```regex
(LUFTHANSA|RYANAIR|EASYJET|BRITISH AIRWAYS|UNITED|DELTA|HOTEL)
```

### Gas Stations
```regex
(SHELL|ARAL|BP|ESSO|TOTAL|JET|SUPER|FUEL)
```

### Insurance Companies
```regex
(INSURANCE|VERSICHERUNG|ALLIANZ|AXA|GENERALI|ERGO)
```

---

## 🔧 Advanced Features

### Test Patterns Before Saving
In the "Add Rule" modal, use the **Test** section:
1. Enter a sample payment reference: `KAUFLAND 8820 BA`
2. Click **Test**
3. See instant feedback: ✓ "Pattern matches!" or ✗ "Pattern does not match"

### Select Specific Payment Fields
Each rule can match against:
- **Counterparty Name** (default) - Most common
- **Reference** - Payment reference/transaction ID
- **Transaction Type** - Type of payment (e.g., "Platba kartou")
- **Counterparty Bank** - Bank information
- **Counterparty Account** - Account details
- **Receiver Info** - Receiver information
- **Additional Info** - Any additional payment data

Example: Match airline codes in the reference field instead of counterparty name.

### Selective Rule Application
1. Go to **Apply Rules** tab
2. Select **"Apply selected rules only"**
3. Check specific rules you want to apply
4. Leave "Apply to all payments" selected
5. Click **"Apply Rules"**

### Disable Rules Without Deleting
1. Edit a rule
2. Uncheck **"Active"**
3. Save
4. Rule won't be applied with "Apply all active rules"
5. Can re-enable later

---

## 📊 API Reference (for Integration)

### Create Rule via API
```bash
curl -X POST http://localhost:8080/api/rules \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Grocery Stores",
    "regexPattern": "(KAUFLAND|TESCO)",
    "labelId": 1,
    "matchingField": "counterpartyName",
    "isActive": true,
    "description": "Matches grocery stores"
  }'
```

### Get All Rules
```bash
curl http://localhost:8080/api/rules
```

### Get Active Rules Only
```bash
curl http://localhost:8080/api/rules/active
```

### Test Pattern
```bash
curl -X POST http://localhost:8080/api/rules/test \
  -H "Content-Type: application/json" \
  -d '{
    "regexPattern": "(KAUFLAND|TESCO)",
    "fieldValue": "KAUFLAND 8820 BA"
  }'
```

### Apply Rules to All Payments
```bash
curl -X POST http://localhost:8080/api/rules/apply \
  -H "Content-Type: application/json" \
  -d '{}'
```

### Apply Specific Rules to All Payments
```bash
curl -X POST http://localhost:8080/api/rules/apply \
  -H "Content-Type: application/json" \
  -d '{
    "ruleIds": [1, 2, 3]
  }'
```

### Apply All Rules to Specific Payments
```bash
curl -X POST http://localhost:8080/api/rules/apply \
  -H "Content-Type: application/json" \
  -d '{
    "paymentIds": [10, 20, 30]
  }'
```

---

## 🎓 Regex Pattern Tips

### Basic Patterns
```regex
KAUFLAND              # Exact match
kaufland              # Case-sensitive!
KAUF.*               # Starts with KAUF, anything after
.*LAND               # Ends with LAND
KAUF.{2,5}LAND       # KAUF, 2-5 chars, LAND
```

### Multiple Options (OR)
```regex
(KAUFLAND|TESCO)     # KAUFLAND or TESCO
(A|B|C)              # A or B or C
```

### Character Classes
```regex
[0-9]                # Any digit
[a-z]                # Any lowercase letter
[A-Z]                # Any uppercase letter
[A-Za-z0-9]          # Any letter or digit
```

### Special Characters
```regex
\.                   # Literal dot (escape with \)
\-                   # Literal dash
\(                   # Literal parenthesis
```

### Anchors (Optional)
```regex
^KAUFLAND            # Starts with KAUFLAND
LAND$                # Ends with LAND
^KAUFLAND$           # Exactly KAUFLAND
```

---

## ❓ FAQ

**Q: Can I use case-insensitive matching?**
A: Not yet - patterns are case-sensitive. Use `(KAUFLAND|kaufland)` for both cases.

**Q: What if a payment matches multiple rules?**
A: All matching labels are assigned. System prevents duplicate label assignment.

**Q: Can I undo rule application?**
A: Currently no. You must manually remove labels. Use a test label to practice first.

**Q: What happens if a rule has invalid regex?**
A: The UI validates before saving. Invalid patterns are rejected with an error message.

**Q: Can I schedule automatic rule application?**
A: Not in this version. Apply manually via UI or API, or integrate via scheduler.

**Q: How many payments can I process?**
A: No limit - all payments are supported. Processing is done in a single transaction.

---

## 🚨 Troubleshooting

### Rules Not Applying
1. ✓ Check rule is marked as **"Active"**
2. ✓ Verify payment field contains data
3. ✓ Test pattern with sample data first
4. ✓ Check regex syntax

### Pattern Not Matching
1. Use the **Test** tool in the rule form
2. Remember patterns are **case-sensitive**
3. Escape special characters with `\`
4. Try simpler pattern first

### Labels Not Showing
1. Refresh the page
2. Check database for duplicate prevention
3. Verify label exists in system

### API Errors
- **400 Bad Request**: Check JSON format and required fields
- **404 Not Found**: Label or rule doesn't exist
- **500 Internal Error**: Check server logs

---

## 📚 Learn More

For complete documentation, see:
- `AUTOMATIC_LABELING_IMPLEMENTATION.md` - Full API reference and features
- `AUTOMATIC_LABELING_UI_SUMMARY.md` - Implementation summary

---

## ✨ Example Workflow

1. **Create Labels First**
   - Go to Labels page
   - Create: Grocery, Parking, Utilities, Restaurants

2. **Create Rules**
   - Go to Rules page
   - Create rule for each category with appropriate patterns

3. **Test Rules**
   - Use test feature in each rule
   - Verify with sample payment data

4. **Apply Rules**
   - Go to Apply Rules tab
   - Start with "Apply all active rules"
   - Click "Apply Rules"

5. **Review Results**
   - Check Payments page
   - Filter by label to verify
   - Adjust rules if needed

---

## 🎯 Next Steps

- Create your first labeling rule
- Test it with sample data
- Apply to all payments
- Refine patterns based on results
- Build your complete rule set

---

**Last Updated:** February 9, 2026
**Version:** 1.0

