# Acceptance Criteria — T032

## Functional
1. When a weekly schedule is uploaded (POST /api/weekly-schedule/upload), the PDCA integration is triggered automatically.
2. When a weekly schedule item is edited (PUT /api/weekly-schedule/:id), the PDCA integration is re-triggered for that week_start+factory.
3. After PDCA integration runs, `material_demand` table contains the calculated material requirements for the given week_start+factory.
4. Previous material_demand records for the same week_start+factory are replaced on each trigger.
5. PDCA stub returns realistic mock data (at least 3 material items per schedule entry).

## API Contract (PDCA — Internal)

### Request (to PDCA)
```json
{
  "schedule": [
    {
      "product_code": "121AR002",
      "quantity": 1200,
      "demand_date": "2025-08-15"
    }
  ]
}
```

### Response (from PDCA)
```json
{
  "materials": [
    {
      "material_code": "AA08C",
      "material_name": "關華豆膠(LF20)/25kg/包",
      "unit": "KG",
      "demand_date": "2025-08-08",
      "expected_delivery": 100.0,
      "demand_quantity": 150.5,
      "estimated_inventory": 50.5
    }
  ]
}
```

## Non-Functional
- PDCA API call failure must NOT cause the weekly schedule upload to fail.
- Errors must be logged with sufficient detail for debugging.
- Stub must be swappable via Spring profile or configuration.

## How to Verify
1. Upload a weekly schedule via API.
2. Query `material_demand` table: `SELECT * FROM material_demand WHERE week_start = ? AND factory = ?`
3. Verify records exist with material_code, demand_quantity, etc.
4. Re-upload schedule for same week_start+factory → old records replaced.
5. Check application logs for PDCA integration log entries.
