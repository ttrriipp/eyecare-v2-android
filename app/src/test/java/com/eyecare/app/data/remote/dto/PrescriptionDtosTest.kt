package com.eyecare.app.data.remote.dto

import com.eyecare.app.di.NetworkModule
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PrescriptionDtosTest {

    private val json: Json = NetworkModule.provideJson()

    @Test
    fun `prescription list response decodes nested measurements`() {
        val responseJson = """
        {
          "data": [
            {
              "id": 1,
              "appointment_id": 1,
              "previous_prescription_id": null,
              "is_current": true,
              "date": "2026-07-27",
              "measurements": {
                "main": {
                  "od": {"value": null, "sphere": "-2.00", "cylinder": "-0.50"},
                  "os": {"value": null, "sphere": "-1.75", "cylinder": "-0.25"}
                },
                "add": {
                  "od": {"value": null, "sphere": null, "cylinder": null},
                  "os": {"value": null, "sphere": null, "cylinder": null}
                }
              },
              "remarks": null
            }
          ],
          "links": null,
          "meta": {"current_page": 1, "last_page": 1, "per_page": 15, "total": 1}
        }
        """.trimIndent()

        val response = json.decodeFromString<PrescriptionDtos.PrescriptionListResponse>(responseJson)
        assertEquals(1, response.data.size)

        val prescription = response.data[0]
        assertEquals(1, prescription.id)
        assertEquals(1, prescription.appointmentId)
        assertNull(prescription.previousPrescriptionId)
        assertTrue(prescription.isCurrent)
        assertEquals("2026-07-27", prescription.date)
        assertNull(prescription.remarks)

        val mainOd = prescription.measurements.main.od
        assertNull(mainOd.value)
        assertEquals("-2.00", mainOd.sphere)
        assertEquals("-0.50", mainOd.cylinder)

        val mainOs = prescription.measurements.main.os
        assertNull(mainOs.value)
        assertEquals("-1.75", mainOs.sphere)
        assertEquals("-0.25", mainOs.cylinder)

        val addOd = prescription.measurements.add.od
        assertNull(addOd.value)
        assertNull(addOd.sphere)
        assertNull(addOd.cylinder)
    }

    @Test
    fun `prescription detail response decodes with previous_prescription_id`() {
        val responseJson = """
        {
          "data": {
            "id": 2,
            "appointment_id": 1,
            "previous_prescription_id": 1,
            "is_current": false,
            "date": "2026-06-15",
            "measurements": {
              "main": {
                "od": {"value": "20/20", "sphere": "-1.50", "cylinder": "-0.25"},
                "os": {"value": "20/25", "sphere": "-1.25", "cylinder": "-0.50"}
              },
              "add": {
                "od": {"value": null, "sphere": "+1.00", "cylinder": null},
                "os": {"value": null, "sphere": "+1.25", "cylinder": null}
              }
            },
            "remarks": "Patient reports improved comfort"
          }
        }
        """.trimIndent()

        val response = json.decodeFromString<PrescriptionDtos.PrescriptionResponse>(responseJson)
        val prescription = response.data

        assertEquals(2, prescription.id)
        assertEquals(1, prescription.previousPrescriptionId)
        assertEquals(false, prescription.isCurrent)
        assertEquals("2026-06-15", prescription.date)
        assertEquals("Patient reports improved comfort", prescription.remarks)

        val addOd = prescription.measurements.add.od
        assertEquals("+1.00", addOd.sphere)
    }

    @Test
    fun `prescription with all null measurements decodes safely`() {
        val responseJson = """
        {
          "data": {
            "id": 3,
            "appointment_id": 2,
            "previous_prescription_id": null,
            "is_current": true,
            "date": "2026-07-28",
            "measurements": {
              "main": {
                "od": {"value": null, "sphere": null, "cylinder": null},
                "os": {"value": null, "sphere": null, "cylinder": null}
              },
              "add": {
                "od": {"value": null, "sphere": null, "cylinder": null},
                "os": {"value": null, "sphere": null, "cylinder": null}
              }
            },
            "remarks": null
          }
        }
        """.trimIndent()

        val response = json.decodeFromString<PrescriptionDtos.PrescriptionResponse>(responseJson)
        val prescription = response.data

        assertNull(prescription.measurements.main.od.value)
        assertNull(prescription.measurements.main.od.sphere)
        assertNull(prescription.measurements.main.od.cylinder)
        assertNull(prescription.measurements.add.os.sphere)
        assertNull(prescription.remarks)
    }
}
