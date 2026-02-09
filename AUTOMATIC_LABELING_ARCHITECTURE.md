# Automatic Labeling - Architecture & User Flow

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Payment Labeling Engine              │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────┐      ┌──────────────────┐            │
│  │   Web Interface  │      │   REST API       │            │
│  │   (Thymeleaf)    │      │   (JSON/HTTP)    │            │
│  └────────┬─────────┘      └────────┬─────────┘            │
│           │                         │                      │
│           └──────────────┬──────────┘                       │
│                          │                                  │
│                   ┌──────▼──────┐                           │
│                   │  Controllers │                          │
│                   │              │                          │
│   ┌───────────────┤              ├───────────────┐         │
│   │               │  RuleWebCtrl │               │         │
│   │               │  RuleApiCtrl │               │         │
│   │               └──────────────┘               │         │
│   │                                              │         │
│   ▼                                              ▼         │
│ ┌─────────────┐                         ┌─────────────┐  │
│ │   Services  │                         │  Repositories│  │
│ │             │                         │              │  │
│ │LabelingSvc  │─────────┬──────────────▶LabelingRuleRepo│  │
│ │LabelSvc     │         │               │PaymentLabelRepo│ │
│ │PaymentSvc   │         │               │PaymentRepo   │  │
│ │PaymentLblSvc│         │               └─────────────┘  │
│ └────────┬────┘         │                                 │
│          │              │               ┌──────────────┐ │
│          └──────────────┼──────────────▶│   Database   │ │
│                         │               │              │ │
│          ┌──────────────┴──────────────▶│ labeling_    │ │
│          │                              │ rules        │ │
│          │  Regex Pattern Matching      │ payments     │ │
│          │  Field Selection             │ labels       │ │
│          │  Rule Application            │ payment_     │ │
│          │                              │ labels       │ │
│          └──────────────────────────────┴──────────────┘ │
│                                                            │
└─────────────────────────────────────────────────────────────┘
```

## User Interaction Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    USER WORKFLOW                            │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ 1. Navigate to /rules                               │  │
│  │    [Two tabs: Manage Rules | Apply Rules]           │  │
│  └─────────────────────────────────────────────────────┘  │
│                          │                                  │
│                          ▼                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ 2. Click "Add New Rule"                             │  │
│  │    [Modal opens with form]                          │  │
│  └─────────────────────────────────────────────────────┘  │
│                          │                                  │
│                          ▼                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ 3. Fill Rule Details                                │  │
│  │    - Name: "Grocery Stores"                         │  │
│  │    - Field: "Counterparty Name"                     │  │
│  │    - Pattern: "(KAUFLAND|TESCO)"                    │  │
│  │    - Label: "Grocery"                               │  │
│  └─────────────────────────────────────────────────────┘  │
│                          │                                  │
│                          ▼                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ 4. Test Pattern (Optional)                          │  │
│  │    - Enter sample: "KAUFLAND 8820"                  │  │
│  │    - Click "Test"                                   │  │
│  │    - Get feedback: ✓ Matches!                       │  │
│  └─────────────────────────────────────────────────────┘  │
│                          │                                  │
│                          ▼                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ 5. Save Rule                                        │  │
│  │    [Rule stored in database]                        │  │
│  └─────────────────────────────────────────────────────┘  │
│                          │                                  │
│                          ▼                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ 6. Switch to "Apply Rules" tab                      │  │
│  │    - Select rules (all active or specific)          │  │
│  │    - Select payments (all or specific)              │  │
│  └─────────────────────────────────────────────────────┘  │
│                          │                                  │
│                          ▼                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ 7. Click "Apply Rules"                              │  │
│  │    [Processing indicator shown]                     │  │
│  └─────────────────────────────────────────────────────┘  │
│                          │                                  │
│                          ▼                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ 8. View Results                                     │  │
│  │    - Processed: 150 payments                        │  │
│  │    - Labels applied: 245                            │  │
│  │    - Per-payment breakdown shown                    │  │
│  └─────────────────────────────────────────────────────┘  │
│                          │                                  │
│                          ▼                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ 9. Verify on Payments Page                          │  │
│  │    [Payments now have assigned labels]              │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Rule Application Process

```
┌─────────────────────────────────────────────────────────────┐
│              RULE APPLICATION ALGORITHM                     │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Input: Rules to apply, Payments to process                │
│                                                              │
│  START                                                      │
│    │                                                        │
│    ▼                                                        │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Load all payments (or selected payment IDs)          │  │
│  └──────────────────────────────────────────────────────┘  │
│    │                                                        │
│    ▼                                                        │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Load all rules (or selected rule IDs)                │  │
│  └──────────────────────────────────────────────────────┘  │
│    │                                                        │
│    ▼                                                        │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ FOR EACH PAYMENT:                                    │  │
│  │   FOR EACH RULE:                                     │  │
│  │     • Get matching field value from payment          │  │
│  │       (e.g., counterpartyName, reference, etc.)      │  │
│  │     • Compile regex pattern                          │  │
│  │     • Check if pattern matches field value           │  │
│  │       │                                              │  │
│  │       ├─ YES:                                        │  │
│  │       │   • Check if label already assigned          │  │
│  │       │   • If NOT assigned:                         │  │
│  │       │     - Assign label to payment                │  │
│  │       │     - Count this as applied label            │  │
│  │       │                                              │  │
│  │       └─ NO:                                         │  │
│  │           • Skip, continue to next rule              │  │
│  └──────────────────────────────────────────────────────┘  │
│    │                                                        │
│    ▼                                                        │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Return Statistics:                                   │  │
│  │ - Total payments processed                          │  │
│  │ - Total labels applied                              │  │
│  │ - Per-payment counts                                │  │
│  └──────────────────────────────────────────────────────┘  │
│    │                                                        │
│    ▼                                                        │
│  END                                                        │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Database Schema

```
┌─────────────────────────────────────────────────────────────┐
│                  ENTITY RELATIONSHIPS                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────────┐      ┌──────────────────────┐   │
│  │   labeling_rules     │      │      labels          │   │
│  ├──────────────────────┤      ├──────────────────────┤   │
│  │ id (PK)              │      │ id (PK)              │   │
│  │ name                 │      │ name                 │   │
│  │ regex_pattern        │      │ description          │   │
│  │ label_id (FK) ───────┼─────▶│                      │   │
│  │ is_active            │      └──────────────────────┘   │
│  │ matching_field       │                                  │
│  │ description          │                                  │
│  │ created_at           │      ┌──────────────────────┐   │
│  │ updated_at           │      │     payments         │   │
│  └──────────────────────┘      ├──────────────────────┤   │
│                                │ id (PK)              │   │
│                                │ amount               │   │
│  ┌──────────────────────┐      │ counterparty_name    │   │
│  │  payment_labels      │      │ reference            │   │
│  ├──────────────────────┤      │ transaction_type     │   │
│  │ id (PK)              │      │ ...                  │   │
│  │ payment_id (FK) ─────┼──────┤                      │   │
│  │ label_id (FK) ───────┤      └──────────────────────┘   │
│  └──────────────────────┘                                  │
│           │                                                │
│           │ (Many-to-Many)                                │
│           │ Linking payments to labels                    │
│           │ Labels from rules applied                     │
│           │                                                │
│           └────────────────────────────────────────────►  │
│                                                            │
└─────────────────────────────────────────────────────────────┘
```

## UI Component Hierarchy

```
/rules Page (RuleWebController)
│
├─ Tabs Section
│  ├─ Tab 1: Manage Rules
│  │  ├─ Rules Table
│  │  │  ├─ Column: Name
│  │  │  ├─ Column: Label (badge)
│  │  │  ├─ Column: Matching Field
│  │  │  ├─ Column: Pattern
│  │  │  ├─ Column: Status
│  │  │  ├─ Column: Created
│  │  │  └─ Column: Actions (Edit/Delete)
│  │  │
│  │  └─ Add New Rule Button
│  │     └─ Modal: Add Rule Form
│  │        ├─ Input: Rule Name
│  │        ├─ Textarea: Description
│  │        ├─ Select: Matching Field
│  │        ├─ Select: Label
│  │        ├─ Input: Regex Pattern
│  │        ├─ Test Section
│  │        │  ├─ Input: Test Value
│  │        │  ├─ Button: Test
│  │        │  └─ Result: Match/No Match
│  │        ├─ Checkbox: Active
│  │        └─ Buttons: Cancel/Save
│  │
│  └─ Edit Rule Modal (Similar to Add)
│
│
└─ Tab 2: Apply Rules
   ├─ Rules Selection
   │  ├─ Radio: Apply all active rules
   │  └─ Radio: Apply selected rules only
   │     └─ Checkboxes: List of rules
   │
   ├─ Payment Selection
   │  ├─ Radio: Apply to all payments
   │  └─ Radio: Apply to selected payments
   │
   ├─ Apply Button
   │
   ├─ Loading Indicator (while processing)
   │
   └─ Results Display
      ├─ Payments Processed: X
      ├─ Labels Applied: Y
      └─ Per-Payment Breakdown

```

## Data Flow: Create and Apply Rule

```
User Interface
     │
     ├─────────────┬─────────────┐
     │             │             │
     ▼             ▼             ▼
[Create Rule] [Test Pattern] [Apply Rules]
     │             │             │
     │ JSON        │ JSON        │ JSON
     │ POST        │ POST        │ POST
     │ /api/rules  │ /api/rules/ │ /api/rules/
     │             │ test        │ apply
     │
     └─────────┬───────────────┬──────────┘
               │               │
               ▼               ▼
        LabelingRuleController
               │
        ┌──────┴──────┐
        │             │
        ▼             ▼
   Validate     Pattern
   Inputs      Matching
        │             │
        └──────┬──────┘
               │
               ▼
        LabelingRuleService
        PaymentLabelService
        PaymentService
               │
               ▼
        Repositories
               │
               ▼
        Database
               │
               ▼
        JSON Response
               │
               ▼
        JavaScript
               │
               ▼
        Update UI
               │
               ▼
        User sees results
```

## Matching Field Selection Flow

```
Rule Creation
     │
     ▼
┌──────────────────────────────┐
│ Select Matching Field        │
│ (Dropdown with 7 options)    │
├──────────────────────────────┤
│ • Counterparty Name ◄────┐   │
│ • Reference            │   │
│ • Transaction Type     │   │
│ • Counterparty Bank    │   │
│ • Counterparty Account │   │
│ • Receiver Info        │   │
│ • Additional Info      │   │
└──────────────────────────────┘
     │
     │ Selected field name
     │ (e.g., "counterpartyName")
     │
     ▼
Save to database
(matching_field column)
     │
     ▼
During Rule Application
     │
     ├─ For each payment:
     │  └─ Get field value:
     │     payment.getCounterpartyName()
     │     payment.getReference()
     │     payment.getTransactionType()
     │     etc.
     │
     ▼
Match against regex
     │
     ▼
Apply label if match

```

---

**Visual Guide Created:** February 9, 2026
**Architecture Version:** 1.0

