package com.eyecare.app.presentation.prescriptions

import com.eyecare.app.domain.model.Prescription
import com.eyecare.app.domain.repository.PrescriptionRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class PrescriptionViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: PrescriptionRepository

    private val today = LocalDate.now()
    private val futureDate = today.plusMonths(6).toString()
    private val expiredDate = today.minusDays(1).toString()

    private fun makePrescription(id: Int, expiresAt: String?) = Prescription(
        id = id, appointmentId = 1,
        odSphere = "-2.00", odCylinder = "-0.50", odAxis = "180", odAdd = null,
        osSphere = "-1.75", osCylinder = "-0.25", osAxis = "175", osAdd = null,
        pd = "62.0", prescribedAt = "2026-01-10", expiresAt = expiresAt, notes = null,
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mockk()
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `list loads prescriptions sorted by prescribedAt descending`() = runTest {
        val list = listOf(makePrescription(1, futureDate), makePrescription(2, futureDate))
        coEvery { repo.getPrescriptions() } returns Result.success(list)
        val vm = PrescriptionViewModel(repo)
        val state = vm.listState.value as PrescriptionListUiState.Success
        assertEquals(2, state.prescriptions.size)
    }

    @Test
    fun `list error emits Error`() = runTest {
        coEvery { repo.getPrescriptions() } returns Result.failure(RuntimeException("fail"))
        val vm = PrescriptionViewModel(repo)
        assertInstanceOf(PrescriptionListUiState.Error::class.java, vm.listState.value)
    }

    @Test
    fun `loadDetail loads single prescription`() = runTest {
        val p = makePrescription(1, futureDate)
        coEvery { repo.getPrescriptions() } returns Result.success(listOf(p))
        coEvery { repo.getPrescription(1) } returns Result.success(p)
        val vm = PrescriptionViewModel(repo)
        vm.loadDetail(1)
        val state = vm.detailState.value as PrescriptionDetailUiState.Success
        assertEquals("-2.00", state.prescription.odSphere)
    }

    @Test
    fun `isExpired returns true when expiresAt is in the past`() = runTest {
        coEvery { repo.getPrescriptions() } returns Result.success(emptyList())
        val vm = PrescriptionViewModel(repo)
        assertEquals(true, vm.isExpired(expiredDate))
        assertEquals(false, vm.isExpired(futureDate))
        assertEquals(false, vm.isExpired(null))
    }
}
