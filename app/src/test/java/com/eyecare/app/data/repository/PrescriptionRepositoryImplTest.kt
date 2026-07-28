package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.PrescriptionApiService
import com.eyecare.app.data.remote.dto.PaginationMeta
import com.eyecare.app.data.remote.dto.PrescriptionDtos
import com.eyecare.app.domain.model.EyeMeasurement
import com.eyecare.app.domain.model.PrescriptionMeasurementGroup
import com.eyecare.app.domain.model.PrescriptionMeasurements
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PrescriptionRepositoryImplTest {

    private val api: PrescriptionApiService = mockk()
    private val repository = PrescriptionRepositoryImpl(api)

    private fun createEyeMeasurement(
        value: String? = null,
        sphere: String? = null,
        cylinder: String? = null,
    ) = PrescriptionDtos.EyeMeasurementDto(value, sphere, cylinder)

    private fun createDto(
        id: Int = 1,
        appointmentId: Int? = 1,
        previousPrescriptionId: Int? = null,
        isCurrent: Boolean = true,
        date: String = "2026-07-27",
        remarks: String? = null,
        mainOd: PrescriptionDtos.EyeMeasurementDto = createEyeMeasurement(sphere = "-2.00", cylinder = "-0.50"),
        mainOs: PrescriptionDtos.EyeMeasurementDto = createEyeMeasurement(sphere = "-1.75", cylinder = "-0.25"),
        addOd: PrescriptionDtos.EyeMeasurementDto = createEyeMeasurement(),
        addOs: PrescriptionDtos.EyeMeasurementDto = createEyeMeasurement(),
    ) = PrescriptionDtos.PrescriptionDto(
        id = id,
        appointmentId = appointmentId,
        previousPrescriptionId = previousPrescriptionId,
        isCurrent = isCurrent,
        date = date,
        measurements = PrescriptionDtos.MeasurementsDto(
            main = PrescriptionDtos.MeasurementGroupDto(mainOd, mainOs),
            add = PrescriptionDtos.MeasurementGroupDto(addOd, addOs),
        ),
        remarks = remarks,
    )

    @Test
    fun `getPrescriptions maps paginated current versions`() = runTest {
        val dto = createDto()
        val response = PrescriptionDtos.PrescriptionListResponse(
            data = listOf(dto),
            meta = PaginationMeta(currentPage = 1, lastPage = 1, perPage = 15, total = 1),
        )
        coEvery { api.getPrescriptions(any()) } returns response

        val result = repository.getPrescriptions(1).getOrThrow()

        assertEquals(1, result.data.size)
        assertEquals(1, result.currentPage)
        assertEquals(1, result.lastPage)
        assertEquals(1, result.total)

        val prescription = result.data[0]
        assertEquals(1, prescription.id)
        assertEquals(1, prescription.appointmentId)
        assertNull(prescription.previousPrescriptionId)
        assertTrue(prescription.isCurrent)
        assertEquals("2026-07-27", prescription.date)
        assertNull(prescription.remarks)
    }

    @Test
    fun `getPrescription maps detail with nested measurements`() = runTest {
        val dto = createDto(
            id = 2,
            previousPrescriptionId = 1,
            isCurrent = false,
            remarks = "Follow-up notes",
            mainOd = PrescriptionDtos.EyeMeasurementDto(value = "20/20", sphere = "-1.50", cylinder = "-0.25"),
            addOd = PrescriptionDtos.EyeMeasurementDto(sphere = "+1.00"),
        )
        val response = PrescriptionDtos.PrescriptionResponse(data = dto)
        coEvery { api.getPrescription(2) } returns response

        val prescription = repository.getPrescription(2).getOrThrow()

        assertEquals(2, prescription.id)
        assertEquals(1, prescription.previousPrescriptionId)
        assertEquals(false, prescription.isCurrent)
        assertEquals("Follow-up notes", prescription.remarks)

        assertEquals("20/20", prescription.measurements.main.od.value)
        assertEquals("-1.50", prescription.measurements.main.od.sphere)
        assertEquals("-0.25", prescription.measurements.main.od.cylinder)

        assertEquals("+1.00", prescription.measurements.add.od.sphere)
        assertNull(prescription.measurements.add.od.value)
    }

    @Test
    fun `getPrescription maps all-null measurements safely`() = runTest {
        val dto = createDto(
            mainOd = createEyeMeasurement(),
            mainOs = createEyeMeasurement(),
            addOd = createEyeMeasurement(),
            addOs = createEyeMeasurement(),
        )
        val response = PrescriptionDtos.PrescriptionResponse(data = dto)
        coEvery { api.getPrescription(1) } returns response

        val prescription = repository.getPrescription(1).getOrThrow()

        assertNull(prescription.measurements.main.od.value)
        assertNull(prescription.measurements.main.od.sphere)
        assertNull(prescription.measurements.main.od.cylinder)
        assertNull(prescription.measurements.add.os.sphere)
    }
}
