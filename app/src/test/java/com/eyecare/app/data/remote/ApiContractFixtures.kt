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
