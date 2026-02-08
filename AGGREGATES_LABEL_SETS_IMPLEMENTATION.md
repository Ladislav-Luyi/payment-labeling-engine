# Aggregate Feature Implementation - Label Sets

## Overview
Successfully implemented the new aggregate feature where:
- Aggregates group payments by **label sets** (multiple labels), not single labels
- One payment can belong to **multiple aggregates** if it has multiple label combinations
- Aggregates are calculated per **month/year**
- UI provides an **expandable view** to show payments within each aggregate

## Implementation Summary

### 1. Database Schema Changes

**Migration File:** `V3__Modify_aggregates_for_label_sets.sql`

- Removed `label_id` column from `aggregates` table
- Created `aggregate_labels` junction table (many-to-many: Aggregate ↔ Label)
- Created `aggregate_payments` junction table (many-to-many: Aggregate ↔ Payment)
- Removed old unique constraint

### 2. Model Changes

**Aggregate.java**
- Changed from `@ManyToOne Label label` to `@ManyToMany Set<Label> labels`
- Added helper methods:
  - `getLabelsSorted()` - Returns labels sorted by name
  - `getLabelNamesAsString()` - Returns comma-separated label names

**AggregatePayment.java** (NEW)
- Junction entity for tracking which payments belong to which aggregates

### 3. Service Layer Changes

**AggregateService.java**
- Updated interface methods:
  - `getAggregateByLabelSetYearMonth(Set<Long> labelIds, ...)` - Find aggregate by label set
  - `recalculateAggregates()` - Recalculate all aggregates from payment data
  - `getPaymentsForAggregate(Long aggregateId)` - Get all payments for an aggregate
  - `calculateAggregatesForPayment(Long paymentId)` - Update aggregates when payment labels change

**AggregateServiceImpl.java**
- Complete rewrite of aggregation logic:
  - Scans all payments and groups by label combinations
  - Creates aggregates for each unique label set + month/year combination
  - Properly handles label entity fetching and association

### 4. Controller Changes

**AggregateController.java** (REST API)
- Added endpoint: `GET /api/aggregates/{id}/payments` - Returns payments for an aggregate
- Added endpoint: `POST /api/aggregates/recalculate` - Triggers recalculation
- Updated filtering to work with label sets

**AggregateWebController.java**
- Added endpoint: `POST /aggregates/recalculate` - Web UI recalculation button
- Updated view logic to handle label sets
- Removed dependency on single-label structure

### 5. Repository Changes

**AggregateRepository.java**
- Added `findByLabelSetYearAndMonth()` - Native query to find aggregate by exact label set
- Added `findByLabelsIn()` - Find aggregates containing any of specified labels
- Updated `findByLabelId()` - Now uses JOIN on labels collection

**AggregatePaymentRepository.java** (NEW)
- Repository for managing aggregate-payment relationships

### 6. UI Changes

**aggregates/index.html**
- Replaced table view with **Bootstrap accordion** (expandable panels)
- Each aggregate shows:
  - Month/Year
  - Label badges (all labels in the set)
  - Total amount and transaction count
- Click to expand and see payments
- "Load Payments" button triggers AJAX call to fetch payment details
- "Recalculate Aggregates" button to regenerate all aggregates

## How It Works

### Aggregation Logic

1. **Payment labeling**: Users assign labels to payments (can be multiple labels per payment)

2. **Aggregate calculation**: When "Recalculate Aggregates" is clicked:
   ```
   For each payment:
     - Extract payment date (year, month)
     - Get all labels assigned to this payment
     - Create/update aggregate for (label_set, year, month)
     - Add payment amount to aggregate total
     - Increment transaction count
   ```

3. **Example**:
   ```
   Payment A: Date=2024-01, Labels={Rent, Housing} → Aggregate(2024, 1, {Rent, Housing})
   Payment B: Date=2024-01, Labels={Rent, Housing} → Same aggregate
   Payment C: Date=2024-01, Labels={Rent} → Aggregate(2024, 1, {Rent})
   Payment D: Date=2024-02, Labels={Rent, Housing} → Aggregate(2024, 2, {Rent, Housing})
   
   Result: 3 aggregates created
   ```

### API Endpoints

**REST API:**
- `GET /api/aggregates` - List all aggregates (with optional filters)
- `GET /api/aggregates/{id}` - Get specific aggregate
- `GET /api/aggregates/{id}/payments` - Get payments for an aggregate (NEW)
- `POST /api/aggregates/recalculate` - Recalculate all aggregates (NEW)
- `GET /api/aggregates/export/csv` - Export aggregates to CSV

**Web UI:**
- `GET /aggregates` - View aggregates page
- `POST /aggregates/recalculate` - Trigger recalculation

## Usage Instructions

### 1. Start the Application
```bash
mvn spring-boot:run
```

### 2. Assign Labels to Payments
- Navigate to "Payments" page
- Assign labels to payments (can assign multiple labels)

### 3. Generate Aggregates
- Navigate to "Aggregates" page
- Click "Recalculate Aggregates" button
- System will analyze all payments and create aggregates

### 4. View Aggregate Details
- Click on any aggregate accordion to expand
- Click "Load Payments" to see individual payments
- View total amount and transaction count

### 5. Filter Aggregates
- Use Year/Month filters to narrow down results
- Filter by specific label (shows aggregates containing that label)

## Database Schema

```sql
-- Aggregates table (main aggregate data)
aggregates:
  - id (PK)
  - year
  - month
  - total_amount
  - transaction_count
  - created_at
  - updated_at

-- Junction table: Aggregate ↔ Labels
aggregate_labels:
  - id (PK)
  - aggregate_id (FK → aggregates.id)
  - label_id (FK → labels.id)

-- Junction table: Aggregate ↔ Payments (for tracking)
aggregate_payments:
  - id (PK)
  - aggregate_id (FK → aggregates.id)
  - payment_id (FK → payments.id)
```

## Testing

### Manual Testing Steps

1. **Create Labels**:
   - Go to Labels page
   - Create labels: "Rent", "Housing", "Utilities", "Groceries"

2. **Import/Create Payments**:
   - Import CSV or manually create payments

3. **Assign Labels**:
   - Payment 1 (Jan 2024): Rent, Housing
   - Payment 2 (Jan 2024): Rent, Housing
   - Payment 3 (Jan 2024): Utilities
   - Payment 4 (Feb 2024): Rent, Housing
   - Payment 5 (Feb 2024): Groceries

4. **Recalculate Aggregates**:
   - Go to Aggregates page
   - Click "Recalculate Aggregates"

5. **Expected Results**:
   - Aggregate 1: Jan 2024, {Rent, Housing}, 2 payments
   - Aggregate 2: Jan 2024, {Utilities}, 1 payment
   - Aggregate 3: Feb 2024, {Rent, Housing}, 1 payment
   - Aggregate 4: Feb 2024, {Groceries}, 1 payment

6. **Verify Expandable View**:
   - Click each aggregate to expand
   - Click "Load Payments" to see payment details

## Key Features Implemented

✅ **Label-Set Based Aggregation** - Aggregates by label combinations, not single labels
✅ **Multi-Aggregate Membership** - One payment can belong to multiple aggregates
✅ **Month/Year Grouping** - Aggregates grouped by time period
✅ **Expandable UI** - Bootstrap accordion with AJAX payment loading
✅ **Recalculation** - On-demand aggregate regeneration
✅ **Filtering** - Filter by year, month, or label
✅ **CSV Export** - Export aggregates with label sets
✅ **Database Migration** - Flyway migration for schema changes

## Notes

- **Performance**: For large datasets, recalculation may take time. Consider running in background.
- **Optimization**: Future improvement could use incremental updates instead of full recalculation.
- **Label Changes**: If payment labels change, click "Recalculate" to update aggregates.
- **Data Consistency**: Recalculation clears and rebuilds all aggregates from scratch.

## Files Modified/Created

### Created:
- `AggregatePayment.java` - Junction entity model
- `V3__Modify_aggregates_for_label_sets.sql` - Database migration
- This documentation file

### Modified:
- `Aggregate.java` - Changed to support label sets
- `AggregateService.java` - New interface methods
- `AggregateServiceImpl.java` - Complete rewrite of aggregation logic
- `AggregateRepository.java` - New queries for label sets
- `AggregateController.java` - New REST endpoints
- `AggregateWebController.java` - Updated for label sets
- `aggregates/index.html` - New accordion UI with AJAX

## Migration Steps

If you have existing data:

1. **Backup database** before running migration
2. Run application - Flyway will auto-apply V3 migration
3. Existing aggregates will be cleared (data is recalculated anyway)
4. Navigate to Aggregates page and click "Recalculate"
5. New aggregates will be generated based on current payment labels

## Future Enhancements

- [ ] Background/async recalculation for large datasets
- [ ] Incremental aggregate updates (instead of full recalculation)
- [ ] Aggregate deletion/editing UI
- [ ] More sophisticated filtering options
- [ ] Charts/graphs for aggregate visualization
- [ ] Export to PDF with formatting

