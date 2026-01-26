# Issue #3 - CSV Parser Implementation: Integration Test Plan

## 📋 Overview

This document describes the comprehensive integration test suite created for the **CSV Parser Service**. The tests are written following **TDD (Test-Driven Development)** principles and define the expected behavior of the CSV parser before implementation.

## 🎯 Test Scope

The integration tests cover:
1. CSV parsing from Slovak bank statement format
2. Duplicate detection and skipping
3. Data validation and error handling
4. Database persistence
5. Multiple import scenarios
6. Edge cases and boundary conditions

---

## 📝 Test Cases

### 1. **Basic CSV Parsing**

#### Test: `testParseValidSlovakBankStatement()`
- **Given**: Valid Slovak bank statement CSV file
- **When**: Parse CSV using CsvParserService
- **Then**: Successfully parse 2 payment records with no errors

**Files**: `src/test/resources/csv/valid_bank_statement.csv`
```
Pohyby na ucte c. SK0575000000004020035946 zo dna 24.01.2026
[empty line]
[header line]
23.01.2026,-52.13,EUR,/VS405000021/SS1130630652/KS0608,Platba kartou,,,,"Suma: 52,13 EUR 21.1.2026 Miesto: KAUFLAND 8820 BA I.CE",
23.01.2026,-1.12,EUR,/VS405000021/SS1130630652/KS0608,Platba kartou,,,,"Suma: 1,12 EUR 21.1.2026 Miesto: HOPIN PARKING",
```

**Expected Result**:
- ✅ 2 payments parsed successfully
- ✅ 0 duplicates
- ✅ 0 errors

---

### 2. **Data Field Parsing**

#### Test: `testParsesPaymentDateCorrectly()`
- **Expected**: Dates parsed as `LocalDate.of(2026, 1, 23)`
- **Format**: `DD.MM.YYYY` (Slovak format)

#### Test: `testParsesAmountWithDecimalPrecision()`
- **Expected**: Amounts as `BigDecimal` with 2 decimal places
- **Values**: `-52.13`, `-1.12`
- **Note**: Negative values for outgoing payments (standard)

#### Test: `testParsesAllPaymentFields()`
- **Expected Fields**:
  - `paymentDate`: LocalDate
  - `amount`: BigDecimal
  - `currency`: "EUR"
  - `reference`: Payment reference string
  - `transactionType`: "Platba kartou"
  - `receiverInfo`: Additional info from CSV

---

### 3. **Duplicate Detection**

#### Test: `testSkipsDuplicatePayments()`
- **Scenario**: Import same CSV twice
- **Expected**:
  - First import: 2 new payments
  - Second import: 0 new, 2 duplicates skipped

#### Test: `testDuplicateDetectionByUniqueConstraint()`
- **Unique Fields**: `(payment_date, amount, reference, account_number)`
- **Expected**: When all 4 fields match → treat as duplicate
- **Result**: `existsByPaymentDateAndAmountAndReferenceAndAccountNumber()` returns `true`

#### Test: `testDuplicateDetectionIgnoresDifferentAmounts()`
- **Scenario**: Same date, reference, account but different amount
- **Expected**: NOT a duplicate (should allow import)
- **Result**: `false` from duplicate check

---

### 4. **Date Extraction**

#### Test: `testExtractsMonthAndYearFromPaymentDate()`
- **Given**: Payment with date `23.01.2026`
- **Expected**: 
  - Month: 1 (January)
  - Year: 2026

---

### 5. **Header Handling**

#### Test: `testSkipsFirstThreeHeaderLines()`
- **Given**: CSV with 3 header lines + 2 payment lines
- **Expected**: 
  - Parse only the 2 payment records
  - Skip header lines automatically
  - Total records: 2 (not 5)

#### Test: `testHandlesEmptyPaymentData()`
- **Given**: CSV with only headers, no payment lines
- **Expected**: 
  - Success: `true`
  - Parsed payments: 0 (empty list)

**File**: `src/test/resources/csv/empty_bank_statement.csv`

---

### 6. **Optional Fields**

#### Test: `testHandlesMissingOptionalFields()`
- **Scenario**: CSV with minimal required fields
- **Expected**: 
  - Required fields (date, amount, currency) are populated
  - Optional fields (counterpartyName) can be `null`

**File**: `src/test/resources/csv/minimal_payment_statement.csv`

---

### 7. **Error Handling**

#### Test: `testHandlesInvalidDateFormat()`
- **Given**: CSV with malformed date (e.g., `23/01/2026` instead of `23.01.2026`)
- **Expected**: 
  - Success: `false`
  - Errors list contains message mentioning "date"

**File**: `src/test/resources/csv/invalid_date_format.csv`

#### Test: `testHandlesInvalidAmountFormat()`
- **Given**: CSV with invalid amount (e.g., `"invalid_amount"`)
- **Expected**: 
  - Success: `false`
  - Errors list contains message mentioning "amount"

**File**: `src/test/resources/csv/invalid_amount_format.csv`

---

### 8. **Database Operations**

#### Test: `testSavesParsedPaymentsToDatabase()`
- **Given**: 2 parsed payments
- **When**: Call `savePayments()`
- **Then**: 
  - 2 records in database
  - Data matches original CSV

#### Test: `testHandlesNullValuesGracefully()`
- **Given**: Payment with `null` fields (reference, account)
- **Expected**: Saves successfully without throwing exception

---

### 9. **Multiple Imports with Overlap**

#### Test: `testImportsMultipleCsvsWithOverlap()`
- **Scenario**:
  1. First import: 2 payments
  2. Second import: 1 overlapping + 1 new

- **Expected After First Import**: 2 total payments
- **Expected After Second Import**:
  - New payments: 1
  - Duplicates skipped: 1
  - Total in database: 3

**Files**: 
- `src/test/resources/csv/valid_bank_statement.csv` (first import)
- `src/test/resources/csv/overlapping_bank_statement.csv` (second import)

---

### 10. **Import Summary Statistics**

#### Test: `testReturnsImportSummaryWithStatistics()`
- **Expected Result** (`CsvImportResult`):
  - `newPaymentCount`: 2
  - `duplicateCount`: 0
  - `errors`: empty list
  - `importedAt`: current timestamp
  - `totalProcessed()`: 2

---

## 🗂️ Test CSV Files

| File | Purpose | Records |
|------|---------|---------|
| `valid_bank_statement.csv` | Standard valid import | 2 payments |
| `empty_bank_statement.csv` | No payment records (headers only) | 0 payments |
| `minimal_payment_statement.csv` | Only required fields | 1 payment |
| `invalid_date_format.csv` | Malformed date | Error case |
| `invalid_amount_format.csv` | Malformed amount | Error case |
| `overlapping_bank_statement.csv` | 1 duplicate + 1 new | 2 payments |
| `comma_delimited.csv` | Standard comma-separated values | 2 payments |

---

## 🔧 Test Infrastructure

### Configuration

**File**: `src/test/resources/application-test.yml`

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb  # In-memory H2 database
    username: sa
    password: (empty)
  jpa:
    hibernate:
      ddl-auto: create-drop   # Create schema for each test, drop after
    dialect: H2Dialect
```

### Database Setup

- **Database**: H2 (in-memory for fast tests)
- **Schema**: Auto-created from entities via `ddl-auto: create-drop`
- **Transaction**: `@Transactional` annotation ensures rollback after each test

---

## 📊 Expected Service Interfaces

### CsvParserService

```java
public interface CsvParserService {
    
    /**
     * Parse CSV input stream
     */
    CsvParseResult parseCsv(InputStream csvStream);
    
    /**
     * Save payments without duplicate checking
     */
    void savePayments(List<Payment> payments);
    
    /**
     * Save payments with duplicate detection
     */
    CsvImportResult savePaymentsWithDuplicateDetection(List<Payment> payments);
}
```

### CsvParseResult

```java
@Data
public class CsvParseResult {
    private List<Payment> parsedPayments;      // Successfully parsed records
    private int skippedDuplicates;             // Count of duplicates found
    private List<String> errors;               // Error messages
    private boolean success;                   // Overall success flag
}
```

### CsvImportResult

```java
@Data
public class CsvImportResult {
    private long newPaymentCount;              // New records saved
    private long duplicateCount;               // Duplicates skipped
    private List<String> errors;               // Error messages
    private LocalDateTime importedAt;          // Import timestamp
    
    public long getTotalProcessed() {
        return newPaymentCount + duplicateCount;
    }
}
```

---

## ✅ Test Execution

### Run All Tests

```bash
mvn test
```

### Run Only CSV Parser Tests

```bash
mvn test -Dtest=CsvParserServiceIntegrationTest
```

### Run Specific Test

```bash
mvn test -Dtest=CsvParserServiceIntegrationTest#testParseValidSlovakBankStatement
```

---

## 🎯 Key Assertions

Each test validates:

1. **Data Correctness**: Parsed values match expected format and precision
2. **Error Handling**: Invalid input produces appropriate error messages
3. **Duplicate Detection**: Overlapping imports correctly skip duplicates
4. **Database Persistence**: Data is correctly saved and retrievable
5. **Edge Cases**: Null values, empty files, malformed data handled gracefully
6. **Statistics**: Import summaries accurately report counts

---

## 📋 Test Statistics

- **Total Test Cases**: 20
- **Test File**: `CsvParserServiceIntegrationTest.java`
- **CSV Fixtures**: 7 test files
- **Lines of Test Code**: ~450 lines
- **Database**: H2 in-memory
- **Framework**: JUnit 5 + Spring Boot Test

---

## 🚀 Next Steps

1. **Code Review**: Review this test plan with the user
2. **Approval**: Get feedback on test cases and expectations
3. **Implementation**: Implement `CsvParserService` to pass all tests
4. **Continuous Integration**: Run tests with each commit

---

## 📝 Notes

- Tests follow **TDD principles** - they define the contract before implementation
- Uses **integration testing** - tests include database operations
- **Database isolation** - `@Transactional` ensures clean state between tests
- **Real CSV files** - tests use actual CSV data matching Slovak bank format
- **Comprehensive coverage** - tests cover happy paths, edge cases, and error scenarios

---

**Status**: ✅ Tests Ready for Review

Once approved, implementation will follow these specifications exactly.
