# BLSS HTTP Client Tests

IntelliJ IDEA HTTP Client format for testing BLSS API endpoints.

## Setup

1. Open IntelliJ IDEA
2. Go to `Run/Debug Configurations`
3. Select the environment: `http-client.env.json` (or create your own)
4. Open `BLSS_E2E_Tests.http`

## Running Tests

- **Run single request**: Click the green play icon next to any `###` block
- **Run all requests**: Click the play icon at the top of the file
- **Run from specific request**: Right-click → Run

## Environment Variables

The following variables are available:

| Variable | Description | Source |
|----------|-------------|--------|
| `baseUrl` | Base API URL | Environment |
| `adminUsername` | Admin username | Environment |
| `adminPassword` | Admin password | Environment |
| `managerUsername` | Manager username | Environment |
| `managerPassword` | Manager password | Environment |
| `consultantUsername` | Consultant username | Environment |
| `consultantPassword` | Consultant password | Environment |
| `warehouseUsername` | Warehouse username | Environment |
| `warehousePassword` | Warehouse password | Environment |
| `runId` | Dynamic run identifier | Auto-generated |
| `e2eUsername` | Dynamic test username | Auto-generated |
| `deliveryPointId` | Created delivery point ID | Response |
| `productId1` | First product ID | Response |
| `productId2` | Second product ID | Response |
| `orderId1` | First order ID | Response |
| `orderItemId1` | First order item ID | Response |
| `orderId2` | Second order ID | Response |

## Test Flow

1. **Health Check** - Verify API is running
2. **User Management** - Create, read, update, delete test user
3. **Delivery Point** - Create PVZ location
4. **Inventory** - Create and manage products
5. **Order Creation** - Create orders with products
6. **Order Status** - Advance order through workflow
7. **Delivery** - Mark items as delivered
8. **Cancellation** - Cancel second order
9. **Cleanup** - Delete test user

## Assertions

Tests include JavaScript assertions using the `client.test()` API:
- Status code validation
- Response data extraction
- Dynamic variable assignment

## Migration from Postman

This file replaces `BLSS_All_Endpoints_E2E.postman_collection.json`.

**Key differences:**
- Uses `client.global.set()` instead of `pm.collectionVariables.set()`
- Uses `response.status` instead of `pm.response.code`
- Uses `response.body` instead of `pm.response.json()`
- Variables are defined in `http-client.env.json`

## Troubleshooting

**Issue**: Variables not being set
- **Solution**: Ensure response has correct structure, check `client.global.set()` calls

**Issue**: Authentication failures
- **Solution**: Verify credentials in `http-client.env.json`

**Issue**: Port mismatch
- **Solution**: Update `baseUrl` to match your service port (default: 25102)
