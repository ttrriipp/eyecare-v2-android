# Spec: Product Search & Filter + Billing Enhancements

Status: Pending approval
Phase: Specify

## Objective

Leverage new backend capabilities to improve the product catalog and billing experiences in the Android app:
1. **Server-side product search/filter/sort** — replace broken client-side filtering (which only filters loaded pages) with proper API-driven search
2. **Billing OR number** — display the Official Receipt number (`or_number`) returned by the API
3. **Billing PDF download** — let customers download their billing receipt as PDF

## Assumptions

1. `GET /brands` returns `{ "data": [{ "id": 1, "name": "..." }] }`
2. `GET /categories` returns `{ "data": [{ "id": 1, "name": "..." }] }`
3. `GET /billing/{id}/pdf` returns a binary PDF file (Content-Type: application/pdf)
4. The current hardcoded `CATEGORIES` list in `ProductListScreen` will be replaced with API data
5. Product search debounces user input (300ms) before hitting the API
6. Filters reset pagination to page 1

## Changes Required

### 1. OR Number on Billing (trivial)

- Add `or_number: String?` to `BillingDtos.BillingDto`
- Add `orNumber: String?` to domain `Billing`
- Display OR number in `BillingDetailScreen` header (below billing number)

### 2. Server-Side Product Search & Filter

**Current problem:** The app loads page 1 (15 items), then filters client-side. If the user filters by a category that only has items on page 3, they see nothing. Search also only searches loaded items.

**Solution:** Pass all filter params to the API. Reset to page 1 on any filter change.

**API params to support:**
- `search` (string) — text search
- `brand` (int) — brand ID filter
- `category` (int) — category ID filter
- `sort` (string) — `name`, `newest`, `price_asc`, `price_desc`
- `in_stock` (boolean) — only in-stock products

**Not implementing (v1):** `min_price`, `max_price` — adds UI complexity, low user value for a clinic app.

**New endpoints needed:**
- `GET /brands` → for brand filter dropdown
- `GET /categories` → for category filter dropdown

### 3. Billing PDF Download

- Add `GET /billing/{id}/pdf` call that returns `ResponseBody` (raw bytes)
- Save to Downloads folder or open with intent
- Add "Download Receipt" button on `BillingDetailScreen`

## Success Criteria

- [ ] Billing screen shows OR number (e.g., "OR-2026-000001") when present
- [ ] Product search sends `?search=` to API (debounced 300ms)
- [ ] Brand filter dropdown populated from `GET /brands`, sends `?brand={id}` to API
- [ ] Category filter dropdown populated from `GET /categories`, sends `?category={id}` to API
- [ ] Sort options (Name, Newest, Price ↑, Price ↓) send `?sort=` to API
- [ ] Changing any filter resets to page 1 and reloads
- [ ] Pagination still works with active filters (page 2 includes filter params)
- [ ] "Download Receipt" button on billing screen downloads PDF
- [ ] App compiles with zero errors

## Boundaries

- **Always:** Send filters as API query params, never filter paginated data client-side
- **Ask first:** Before adding price range slider UI
- **Never:** Cache filter results in Room (always fresh from API)

## Open Questions

None — all endpoints confirmed in BACKEND_CONTEXT.md.
