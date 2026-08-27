package com.eyecare.app.data.remote

import com.eyecare.app.di.NetworkModule
import kotlinx.serialization.json.Json

internal object ApiContractFixtures {
    val json: Json = NetworkModule.provideJson()

    val paginatedResponse = """
        {
          "data": [
            { "id": 1, "name": "First item" },
            { "id": 2, "name": "Second item" }
          ],
          "links": {
            "first": "https://example.test/api/v1/resources?page=1",
            "last": "https://example.test/api/v1/resources?page=2",
            "prev": null,
            "next": "https://example.test/api/v1/resources?page=2"
          },
          "meta": {
            "current_page": 1,
            "last_page": 2,
            "per_page": 1,
            "total": 2
          }
        }
    """.trimIndent()

    // C1 — Cursor-paginated message list (first page, terminal)
    val messageListPage1 = """
        {
          "data": [
            {
              "id": 2,
              "sender_id": 5,
              "sender_type": "staff",
              "body": "Your frame is ready for pickup.",
              "read_at": "2026-08-01T10:00:00+08:00",
              "created_at": "2026-08-01T09:00:00+08:00",
              "attachments": [
                {
                  "id": 1,
                  "original_name": "receipt.pdf",
                  "mime_type": "application/pdf",
                  "file_size": 45678,
                  "download_url": "/api/v1/conversation/attachments/1"
                }
              ]
            },
            {
              "id": 1,
              "sender_id": 1,
              "sender_type": "patient",
              "body": "Thank you!",
              "read_at": null,
              "created_at": "2026-08-01T08:00:00+08:00",
              "attachments": []
            }
          ],
          "meta": {
            "next_cursor": null,
            "has_more": false
          }
        }
    """.trimIndent()

    // C1 — Cursor-paginated message list (non-terminal, with opaque cursor)
    val messageListPageWithCursor = """
        {
          "data": [
            {
              "id": 50,
              "sender_id": 5,
              "sender_type": "staff",
              "body": "Earlier message.",
              "read_at": null,
              "created_at": "2026-07-15T08:00:00+08:00",
              "attachments": []
            }
          ],
          "meta": {
            "next_cursor": "eyJpZCI6NTAsImNyZWF0ZWRfYXQiOiIyMDI2LTA3LTE1VDA4OjAwOjAwKzA4OjAwIn0=",
            "has_more": true
          }
        }
    """.trimIndent()

    // C1 — Search results with cursor metadata
    val messageSearchResults = """
        {
          "data": [
            {
              "id": 5,
              "sender_id": 1,
              "sender_type": "patient",
              "body": "When will my prescription be ready?",
              "read_at": null,
              "created_at": "2026-08-05T11:00:00+08:00",
              "attachments": []
            }
          ],
          "meta": {
            "next_cursor": null,
            "has_more": false
          }
        }
    """.trimIndent()

    // C2 — Send message response (wrapped in data)
    val sendMessageResponse = """
        {
          "data": {
            "id": 42,
            "sender_id": 1,
            "sender_type": "patient",
            "body": "Do you have the Vista Classic frame available?",
            "read_at": null,
            "created_at": "2026-08-15T10:30:00+08:00",
            "attachments": []
          }
        }
    """.trimIndent()

    // Mark-read response
    val markReadResponse = """
        {
          "marked_count": 3
        }
    """.trimIndent()

    // C3 — Notification list with stable kind and mobile_action
    val notificationList = """
        {
          "data": [
            {
              "id": "550e8400-e29b-41d4-a716-446655440000",
              "kind": "new_message",
              "title": "New Message",
              "body": "Dr. Santos sent a message.",
              "mobile_action": {
                "type": "conversation"
              },
              "read_at": null,
              "created_at": "2026-08-15T10:00:00+08:00"
            },
            {
              "id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
              "kind": "unknown_future_kind",
              "title": "Something happened",
              "body": "A new event occurred.",
              "mobile_action": null,
              "read_at": "2026-08-14T09:00:00+08:00",
              "created_at": "2026-08-14T08:00:00+08:00"
            }
          ],
          "links": { "first": "...", "last": "...", "prev": null, "next": null },
          "meta": { "current_page": 1, "last_page": 1, "per_page": 20, "total": 2 }
        }
    """.trimIndent()

    // C3 — Notification list with legacy fields (ignored by Android)
    val notificationWithLegacyFields = """
        {
          "data": [
            {
              "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
              "type": "App\\Notifications\\NewMessageReceived",
              "kind": "new_message",
              "title": "New Message",
              "body": "Dr. Santos sent a message.",
              "action_url": "/admin/conversations/1",
              "mobile_action": {
                "type": "conversation"
              },
              "related_type": null,
              "related_id": null,
              "read_at": null,
              "created_at": "2026-08-15T10:00:00+08:00"
            }
          ],
          "links": { "first": "...", "last": "...", "prev": null, "next": null },
          "meta": { "current_page": 1, "last_page": 1, "per_page": 20, "total": 1 }
        }
    """.trimIndent()

    // Notification unread count
    val notificationUnreadCount = """
        {
          "unread_count": 3
        }
    """.trimIndent()

    // Mark-one notification read response
    val notificationMarkOneResponse = """
        {
          "message": "Notification marked as read."
        }
    """.trimIndent()

    // Mark-all notifications read response
    val notificationMarkAllResponse = """
        {
          "message": "All notifications marked as read."
        }
    """.trimIndent()

    // --- Saved Frames fixtures ---

    // Saved Frames page (available item, string price, typed AR)
    val savedFramesPageAvailable = """
        {
          "data": [
            {
              "product_variant_id": 42,
              "saved_at": "2026-08-27T10:00:00+08:00",
              "availability": "available",
              "variant": {
                "id": 42,
                "name": "Black / 52mm",
                "sku": "RB-CR-BLK-52",
                "price": "4500.00",
                "compare_at_price": null,
                "attributes": { "color": "black", "size": "52mm" },
                "images": ["/storage/variants/42/front.jpg"],
                "ar": {
                  "status": "ready",
                  "asset": {
                    "url": "https://cdn.example.com/ar/variants/42/v1/model.glb",
                    "format": "glb",
                    "version": 1,
                    "byte_size": 5256552,
                    "sha256": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
                  },
                  "calibration": {
                    "frame_width_mm": 123.0,
                    "scale": { "x": 0.123, "y": 0.123, "z": 0.123 },
                    "anchor": { "x": 0.0, "y": 0.0, "z": 0.0 },
                    "rotation_degrees": { "x": 0.0, "y": 0.0, "z": 0.0 }
                  }
                },
                "product": {
                  "id": 7,
                  "name": "Classic Rectangle",
                  "brand": "Ray-Ban",
                  "category": "Full Rim"
                }
              }
            }
          ],
          "links": {
            "first": "https://example.test/api/v1/saved-frames?page=1",
            "last": "https://example.test/api/v1/saved-frames?page=1",
            "prev": null,
            "next": null
          },
          "meta": {
            "current_page": 1,
            "last_page": 1,
            "per_page": 15,
            "total": 1
          }
        }
    """.trimIndent()

    // Saved Frames page (unavailable item, number price, null AR)
    val savedFramesPageUnavailable = """
        {
          "data": [
            {
              "product_variant_id": 99,
              "saved_at": "2026-08-26T15:30:00+08:00",
              "availability": "unavailable",
              "variant": {
                "id": 99,
                "name": "Tortoise / 54mm",
                "sku": "RB-CR-TORT-54",
                "price": 5200,
                "compare_at_price": 6000.00,
                "attributes": { "color": "tortoise", "size": "54mm" },
                "images": [],
                "ar": null,
                "product": {
                  "id": 7,
                  "name": "Classic Rectangle",
                  "brand": "Ray-Ban",
                  "category": "Full Rim"
                }
              }
            }
          ],
          "links": {
            "first": "https://example.test/api/v1/saved-frames?page=1",
            "last": "https://example.test/api/v1/saved-frames?page=1",
            "prev": null,
            "next": null
          },
          "meta": {
            "current_page": 1,
            "last_page": 1,
            "per_page": 15,
            "total": 1
          }
        }
    """.trimIndent()

    // Save response (PUT returns single resource)
    val savedFrameSaveResponse = """
        {
          "data": {
            "product_variant_id": 42,
            "saved_at": "2026-08-27T10:00:00+08:00",
            "availability": "available",
            "variant": {
              "id": 42,
              "name": "Black / 52mm",
              "sku": "RB-CR-BLK-52",
              "price": "4500.00",
              "compare_at_price": null,
              "attributes": { "color": "black", "size": "52mm" },
              "images": [],
              "ar": null,
              "product": {
                "id": 7,
                "name": "Classic Rectangle",
                "brand": "Ray-Ban",
                "category": "Full Rim"
              }
            }
          }
        }
    """.trimIndent()

    // Frame catalog variant with is_saved field
    val frameCatalogVariantWithSaved = """
        {
          "id": 1,
          "name": "Black / 52mm",
          "sku": "RB-CR-BLK-52",
          "price": 4500.00,
          "compare_at_price": null,
          "attributes": { "color": "black", "size": "52mm" },
          "ar_eligible": true,
          "ar_asset_reference": "rb-cr-blk-52.usdz",
          "is_saved": true,
          "ar": null,
          "images": []
        }
    """.trimIndent()

    // Frame catalog variant without is_saved (legacy cache fallback)
    val frameCatalogVariantLegacy = """
        {
          "id": 2,
          "name": "Tortoise / 54mm",
          "sku": "RB-CR-TORT-54",
          "price": 5200.00,
          "compare_at_price": null,
          "attributes": { "color": "tortoise", "size": "54mm" },
          "ar_eligible": false,
          "ar_asset_reference": null,
          "ar": null,
          "images": []
        }
    """.trimIndent()

    val errorResponses: Map<Int, String> = mapOf(
        401 to """
            {
              "message": "Unauthenticated."
            }
        """.trimIndent(),
        403 to """
            {
              "message": "This action is unauthorized."
            }
        """.trimIndent(),
        404 to """
            {
              "message": "No query results for model [App\\Models\\Appointment] 999"
            }
        """.trimIndent(),
        422 to """
            {
              "message": "The given data was invalid.",
              "errors": {
                "email": ["The email has already been taken."],
                "scheduled_at": ["The scheduled at must be a date after now."]
              }
            }
        """.trimIndent(),
        429 to """
            {
              "message": "Too Many Attempts."
            }
        """.trimIndent(),
    )
}
