# Label Management - User Guide

## Accessing Label Management

Navigate to: **http://localhost:8080/rules**

You'll see three tabs at the top:
1. **Manage Labels** (selected by default) ← You are here
2. **Manage Rules**
3. **Apply Rules**

## Creating a New Label

### Steps:
1. Click the **"Add New Label"** button (blue button with + icon)
2. A modal dialog will appear with two fields:
   - **Label Name** (required) - What you want to call this label
   - **Description** (optional) - Additional details about the label

### Example:
- Name: `Grocery Shopping`
- Description: `Purchases at grocery stores and supermarkets`

3. Click **"Save Label"** button
4. You'll see a success message
5. The new label appears in the table below
6. The label is now available in rule creation dropdowns

## Viewing All Labels

Labels are displayed in a table with the following columns:
- **Name** - The label name
- **Description** - Optional description
- **Created** - Date the label was created
- **Actions** - Edit and Delete buttons

## Editing a Label

### Steps:
1. Find the label you want to edit in the table
2. Click the **"Edit"** button (pencil icon) in the Actions column
3. A modal dialog appears with current label information
4. Modify the name and/or description as needed
5. Click **"Update Label"** button
6. You'll see a success message
7. The table and all dropdowns update automatically

## Deleting a Label

### Steps:
1. Find the label you want to delete in the table
2. Click the **"Delete"** button (trash icon) in the Actions column
3. A confirmation dialog appears asking: "Are you sure you want to delete this label?"
4. Click **"OK"** to confirm or **"Cancel"** to keep the label
5. If confirmed, label is deleted and removed from the table
6. All dropdowns are updated automatically

## Important Notes

⚠️ **Warning**: Deleting a label will:
- Remove it from the system
- Remove it from all labeling rules
- Remove it from all payment associations
- This action cannot be undone - make sure you want to delete it!

✅ **Best Practices**:
- Use clear, descriptive label names
- Use consistent naming (e.g., "Grocery Shopping" not "groceries")
- Add helpful descriptions for team clarity
- Review labels regularly and remove unused ones

## Common Use Cases

### Setup Example

**Label 1:**
- Name: `Grocery Shopping`
- Description: `Grocery stores, supermarkets, food purchases`

**Label 2:**
- Name: `Parking`
- Description: `Parking fees, parking lot payments`

**Label 3:**
- Name: `Utilities`
- Description: `Electric, water, gas bills`

**Label 4:**
- Name: `Restaurants`
- Description: `Restaurants, cafes, food services`

**Label 5:**
- Name: `Entertainment`
- Description: `Movies, concerts, events, hobbies`

After creating labels, you can create labeling rules that automatically assign these labels to payments.

## Troubleshooting

### Label won't save
- ✓ Check that you entered a label name
- ✓ Try refreshing the page
- ✓ Check browser console for errors (F12 → Console tab)

### Label won't delete
- ✓ Check that you confirmed the deletion dialog
- ✓ Verify the label isn't being used by active rules
- ✓ Refresh the page and try again

### Dropdowns don't update
- ✓ The page automatically reloads after create/update/delete
- ✓ Wait for the page to reload completely
- ✓ Manually refresh (F5) if needed

### See "Add New Label" button but can't click it
- ✓ Clear browser cache (Ctrl+Shift+Delete)
- ✓ Check that JavaScript is enabled
- ✓ Try a different browser

## Next Steps

After creating labels, proceed to **Manage Rules** tab to:
1. Create labeling rules that use these labels
2. Define regex patterns to automatically apply labels to payments
3. Test your rules before applying them

Then go to **Apply Rules** tab to:
1. Apply your rules to existing payments
2. View the results of automatic labeling

---

**Tips**: 
- Create labels FIRST before creating rules
- Use simple, clear names that your team understands
- Test rules thoroughly before applying to all payments
- Review and organize labels monthly

For more information, see LABEL_MANAGEMENT_FIX.md documentation.

