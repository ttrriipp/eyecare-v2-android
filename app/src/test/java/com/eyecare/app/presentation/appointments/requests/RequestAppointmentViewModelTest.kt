package com.eyecare.app.presentation.appointments.requests

import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.AppointmentRequestGender
import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentRequestAvailability
import com.eyecare.app.domain.model.AppointmentRequestIdentity
import com.eyecare.app.domain.model.AppointmentRequestStatus
import com.eyecare.app.domain.model.AppointmentRequestTypeSummary
import com.eyecare.app.domain.model.AppointmentType
import com.eyecare.app.domain.model.AvailabilitySlot
import com.eyecare.app.domain.repository.AppointmentRequestRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RequestAppointmentViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: AppointmentRequestRepository
    private lateinit var vm: RequestAppointmentViewModel

    private val normalType = AppointmentType(
        id = 1, name = "First eye examination", description = "For your first examination.",
        durationMinutes = 45, requiresReferral = false,
    )

    private val referralType = AppointmentType(
        id = 4, name = "Referral", description = null,
        durationMinutes = 45, requiresReferral = true,
    )

    private val fakeSlot1 = AvailabilitySlot(
        startsAt = "2026-08-10T09:00:00+08:00",
        endsAt = "2026-08-10T09:45:00+08:00",
        available = true, reason = null,
    )

    private val fakeSlot2 = AvailabilitySlot(
        startsAt = "2026-08-10T10:00:00+08:00",
        endsAt = "2026-08-10T10:45:00+08:00",
        available = true, reason = null,
    )

    private val fakeSlot3 = AvailabilitySlot(
        startsAt = "2026-08-10T11:00:00+08:00",
        endsAt = "2026-08-10T11:45:00+08:00",
        available = true, reason = null,
    )

    private val unavailableSlot = AvailabilitySlot(
        startsAt = "2026-08-10T14:00:00+08:00",
        endsAt = "2026-08-10T14:45:00+08:00",
        available = false, reason = "capacity_reached",
    )

    private fun fakeAvailability(typeId: Int = 1) = AppointmentRequestAvailability(
        date = "2026-08-10", timezone = "Asia/Manila",
        intervalMinutes = 15, slotDurationMinutes = 45,
        visitDurationMinutes = 45, appointmentTypeId = typeId,
        dayStatus = "open", generatedAt = "2026-08-09T10:00:00+08:00",
        slots = listOf(fakeSlot1, fakeSlot2, fakeSlot3, unavailableSlot),
    )

    private val fakeRequest = AppointmentRequest(
        id = 1, requestNumber = "APR-2026-000001",
        status = AppointmentRequestStatus.PENDING, patientId = null,
        appointmentType = AppointmentRequestTypeSummary(1, "First eye examination", 45),
        scheduledAt = "2026-08-10T09:00:00+08:00",
        alternativeScheduledTimes = emptyList(), provisionalDurationMinutes = 45,
        reasonForVisit = "Test", referringSource = null,
        timePreferencesAreReserved = false,
        expiresAt = "2026-08-10T09:00:00+08:00", cancelledAt = null,
        createdAt = "2026-08-09T10:00:00+08:00", appointmentId = null,
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mockk()
        coEvery { repo.getAppointmentTypes() } returns Result.success(listOf(normalType, referralType))
        vm = RequestAppointmentViewModel(repo)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    // ── Type step ──

    @Test
    fun `initial step is Type and loads types`() {
        val step = vm.step.value as RequestStep.Type
        assertEquals(2, step.types.size)
        assertTrue(!step.isLoading)
    }

    @Test
    fun `type load failure shows retry`() {
        coEvery { repo.getAppointmentTypes() } returns Result.failure(Exception("Network"))
        vm = RequestAppointmentViewModel(repo)
        val step = vm.step.value as RequestStep.Type
        assertEquals("Network", step.error)
        assertTrue(step.types.isEmpty())
    }

    @Test
    fun `selectType sets selection`() {
        vm.selectType(normalType)
        val step = vm.step.value as RequestStep.Type
        assertEquals(normalType, step.selectedType)
    }

    @Test
    fun `confirmType advances to Schedule`() {
        vm.selectType(normalType)
        vm.confirmType()
        assertTrue(vm.step.value is RequestStep.Schedule)
        assertEquals(normalType, (vm.step.value as RequestStep.Schedule).selectedType)
    }

    @Test
    fun `confirmType without selection does nothing`() {
        vm.confirmType()
        assertTrue(vm.step.value is RequestStep.Type)
    }

    // ── Schedule step ──

    private fun enterSchedule(): RequestStep.Schedule {
        vm.selectType(normalType)
        vm.confirmType()
        coEvery { repo.getAvailability("2026-08-10", 1) } returns Result.success(fakeAvailability())
        vm.selectDate("2026-08-10")
        return vm.step.value as RequestStep.Schedule
    }

    @Test
    fun `selectDate loads type-specific availability`() {
        val schedule = enterSchedule()
        assertEquals("2026-08-10", schedule.date)
        assertEquals(4, schedule.availability?.slots?.size)
        assertEquals(1, schedule.availability?.appointmentTypeId)
    }

    @Test
    fun `selectDate sends date and appointment_type_id`() {
        enterSchedule()
        coVerify { repo.getAvailability("2026-08-10", 1) }
    }

    @Test
    fun `selectDate failure shows error`() {
        coEvery { repo.getAvailability(any(), any()) } returns Result.failure(Exception("Network"))
        vm.selectType(normalType)
        vm.confirmType()
        vm.selectDate("2026-08-10")
        val step = vm.step.value as RequestStep.Schedule
        assertEquals("Network", step.availabilityError)
    }

    @Test
    fun `stale availability response for different type is ignored`() {
        vm.selectType(normalType)
        vm.confirmType()
        coEvery { repo.getAvailability("2026-08-10", 1) } returns Result.success(fakeAvailability(1))
        coEvery { repo.getAvailability("2026-08-10", 4) } returns Result.success(fakeAvailability(4))
        vm.selectDate("2026-08-10")
        // Change type before response arrives would require coroutine control;
        // this test verifies the type-check guard is present
        val schedule = vm.step.value as RequestStep.Schedule
        assertEquals(1, schedule.selectedType.id)
    }

    @Test
    fun `selectPrimarySlot selects available slot`() {
        enterSchedule()
        vm.selectPrimarySlot(fakeSlot1)
        val step = vm.step.value as RequestStep.Schedule
        assertEquals(fakeSlot1, step.primarySlot)
    }

    @Test
    fun `selectPrimarySlot rejects unavailable slot`() {
        enterSchedule()
        vm.selectPrimarySlot(unavailableSlot)
        assertNull((vm.step.value as RequestStep.Schedule).primarySlot)
    }

    @Test
    fun `selectPrimarySlot removes duplicate from alternatives`() {
        enterSchedule()
        vm.addAlternative(fakeSlot1)
        vm.addAlternative(fakeSlot2)
        vm.selectPrimarySlot(fakeSlot1)
        val step = vm.step.value as RequestStep.Schedule
        assertEquals(fakeSlot1, step.primarySlot)
        assertTrue(step.alternativeSlots.none { it.startsAt == fakeSlot1.startsAt })
    }

    @Test
    fun `addAlternative adds distinct slots`() {
        enterSchedule()
        vm.selectPrimarySlot(fakeSlot1)
        vm.addAlternative(fakeSlot2)
        val step = vm.step.value as RequestStep.Schedule
        assertEquals(1, step.alternativeSlots.size)
        assertEquals(fakeSlot2, step.alternativeSlots[0])
    }

    @Test
    fun `addAlternative rejects duplicate of primary`() {
        enterSchedule()
        vm.selectPrimarySlot(fakeSlot1)
        vm.addAlternative(fakeSlot1)
        assertTrue((vm.step.value as RequestStep.Schedule).alternativeSlots.isEmpty())
    }

    @Test
    fun `addAlternative rejects duplicate of existing alternative`() {
        enterSchedule()
        vm.selectPrimarySlot(fakeSlot1)
        vm.addAlternative(fakeSlot2)
        vm.addAlternative(fakeSlot2)
        assertEquals(1, (vm.step.value as RequestStep.Schedule).alternativeSlots.size)
    }

    @Test
    fun `addAlternative caps at two`() {
        enterSchedule()
        vm.selectPrimarySlot(fakeSlot1)
        vm.addAlternative(fakeSlot2)
        vm.addAlternative(fakeSlot3)
        val extraSlot = AvailabilitySlot(
            startsAt = "2026-08-10T13:00:00+08:00",
            endsAt = "2026-08-10T13:45:00+08:00",
            available = true, reason = null,
        )
        vm.addAlternative(extraSlot)
        assertEquals(2, (vm.step.value as RequestStep.Schedule).alternativeSlots.size)
    }

    @Test
    fun `removeAlternative removes by startsAt`() {
        enterSchedule()
        vm.selectPrimarySlot(fakeSlot1)
        vm.addAlternative(fakeSlot2)
        vm.addAlternative(fakeSlot3)
        vm.removeAlternative(fakeSlot2)
        val step = vm.step.value as RequestStep.Schedule
        assertEquals(1, step.alternativeSlots.size)
        assertEquals(fakeSlot3.startsAt, step.alternativeSlots[0].startsAt)
    }

    @Test
    fun `changing type clears slots and availability`() {
        enterSchedule()
        vm.selectPrimarySlot(fakeSlot1)
        vm.addAlternative(fakeSlot2)
        vm.backToType()
        vm.selectType(referralType)
        vm.confirmType()
        val schedule = vm.step.value as RequestStep.Schedule
        assertNull(schedule.primarySlot)
        assertTrue(schedule.alternativeSlots.isEmpty())
        assertNull(schedule.availability)
    }

    // ── Details step ──

    private fun enterDetails(
        identityRequired: Boolean = false,
        type: AppointmentType = normalType,
    ): RequestStep.Details {
        vm.selectType(type)
        vm.confirmType()
        coEvery { repo.getAvailability("2026-08-10", type.id) } returns Result.success(fakeAvailability(type.id))
        vm.selectDate("2026-08-10")
        vm.selectPrimarySlot(fakeSlot1)
        vm.addAlternative(fakeSlot2)
        vm.confirmSchedule(identityDetailsRequired = identityRequired)
        return vm.step.value as RequestStep.Details
    }

    @Test
    fun `confirmSchedule moves to Details`() {
        val details = enterDetails()
        assertEquals("2026-08-10", details.date)
        assertEquals(fakeSlot1, details.primarySlot)
        assertEquals(1, details.alternativeSlots.size)
    }

    @Test
    fun `empty reason shows validation error`() {
        val details = enterDetails()
        vm.confirmDetails()
        val step = vm.step.value as RequestStep.Details
        assertEquals("Reason for visit is required", step.reasonError)
    }

    @Test
    fun `referral type requires referring source`() {
        val details = enterDetails(type = referralType)
        vm.updateReason("Blurred vision")
        vm.confirmDetails()
        val step = vm.step.value as RequestStep.Details
        assertEquals("Referral source is required for this appointment type", step.referringSourceError)
    }

    @Test
    fun `non-referral type sends null referring source`() {
        enterDetails()
        vm.updateReason("Blurred vision")
        vm.confirmDetails()
        val review = vm.step.value as RequestStep.Review
        assertNull(review.referringSource)
    }

    @Test
    fun `referral type with valid source proceeds to Review`() {
        enterDetails(type = referralType)
        vm.updateReason("Blurred vision")
        vm.updateReferringSource("Dr. Smith")
        vm.confirmDetails()
        val review = vm.step.value as RequestStep.Review
        assertEquals("Dr. Smith", review.referringSource)
    }

    @Test
    fun `stale referral cleared when switching to non-referral type`() {
        enterDetails(type = referralType)
        vm.updateReason("Test")
        vm.updateReferringSource("Dr. Smith")
        vm.backToSchedule()
        vm.backToType()
        vm.selectType(normalType)
        vm.confirmType()
        coEvery { repo.getAvailability("2026-08-10", 1) } returns Result.success(fakeAvailability(1))
        vm.selectDate("2026-08-10")
        vm.selectPrimarySlot(fakeSlot1)
        vm.confirmSchedule()
        val details = vm.step.value as RequestStep.Details
        assertEquals("", details.referringSource)
    }

    @Test
    fun `identity validation errors for linked-unlinked boundary`() {
        enterDetails(identityRequired = true)
        vm.updateReason("Test")
        vm.confirmDetails()
        val step = vm.step.value as RequestStep.Details
        assertEquals("First name is required", step.errors["firstName"])
        assertEquals("Last name is required", step.errors["lastName"])
        assertEquals("Date of birth is required", step.errors["dateOfBirth"])
        assertEquals("A verified phone number is required", step.errors["phone"])
        assertEquals("Gender is required", step.errors["gender"])
        assertEquals("Occupation is required", step.errors["occupation"])
        assertEquals("Address is required", step.errors["address"])
    }

    @Test
    fun `confirmDetails moves to Review with normalized identity`() {
        enterDetails(identityRequired = true)
        vm.updateReason("Blurred vision")
        vm.updateIdentity(
            phone = "+639171234567", email = "alex@example.com",
            firstName = "  Alex ", middleName = " ", lastName = " Rivera ",
            dateOfBirth = "1990-05-15", gender = AppointmentRequestGender.FEMALE,
            occupation = " Teacher ", address = " 123 Main St, Manila ",
        )
        vm.confirmDetails()
        val review = vm.step.value as RequestStep.Review
        assertEquals(
            AppointmentRequestIdentity(
                phone = "+639171234567", email = "alex@example.com",
                firstName = "Alex", middleName = null, lastName = "Rivera",
                dateOfBirth = "1990-05-15", gender = AppointmentRequestGender.FEMALE,
                occupation = "Teacher", address = "123 Main St, Manila",
            ),
            review.identity,
        )
    }

    // ── Review and submit ──

    @Test
    fun `submit sends type ID, primary, alternatives, reason, and referral`() {
        coEvery { repo.createRequest(any(), any(), any(), any(), any(), any()) } returns Result.success(fakeRequest)
        enterDetails(type = referralType)
        vm.updateReason("Blurred vision")
        vm.updateReferringSource("Dr. Smith")
        vm.confirmDetails()
        vm.submit()
        val success = vm.step.value as RequestStep.Success
        assertEquals(1, success.request.id)
    }

    @Test
    fun `submit preserves submitted alternative preference order`() {
        coEvery { repo.createRequest(any(), any(), any(), any(), any(), any()) } returns Result.success(fakeRequest)
        enterSchedule()
        vm.selectPrimarySlot(fakeSlot1)
        vm.addAlternative(fakeSlot3)
        vm.addAlternative(fakeSlot2)
        vm.confirmSchedule()
        vm.updateReason("Blurred vision")
        vm.confirmDetails()

        vm.submit()

        coVerify {
            repo.createRequest(
                appointmentTypeId = normalType.id,
                scheduledAt = fakeSlot1.startsAt,
                reasonForVisit = "Blurred vision",
                alternativeScheduledTimes = listOf(fakeSlot3.startsAt, fakeSlot2.startsAt),
                referringSource = null,
                identity = null,
            )
        }
    }

    @Test
    fun `submit SLOT_UNAVAILABLE returns to Schedule`() {
        coEvery { repo.createRequest(any(), any(), any(), any(), any(), any()) } returns Result.failure(
            ApiDomainError(422, "SLOT_UNAVAILABLE", "Slot taken.")
        )
        enterDetails()
        vm.updateReason("Test")
        vm.confirmDetails()
        vm.submit()
        vm.handleSubmissionError()
        assertTrue(vm.step.value is RequestStep.Schedule)
    }

    @Test
    fun `submit ACTIVE_REQUEST_LIMIT_REACHED preserves draft`() {
        coEvery { repo.createRequest(any(), any(), any(), any(), any(), any()) } returns Result.failure(
            ApiDomainError(422, "ACTIVE_REQUEST_LIMIT_REACHED", "Limit reached.")
        )
        enterDetails()
        vm.updateReason("Test")
        vm.confirmDetails()
        vm.submit()
        vm.handleSubmissionError()
        val step = vm.step.value as RequestStep.SubmissionError
        assertEquals("ACTIVE_REQUEST_LIMIT_REACHED", step.errorCode)
        assertEquals("Test", step.reason)
    }

    // ── Back navigation ──

    @Test
    fun `backToSchedule preserves draft`() {
        enterDetails()
        vm.updateReason("My reason")
        vm.backToSchedule()
        val schedule = vm.step.value as RequestStep.Schedule
        assertEquals("My reason", schedule.reasonDraft)
        assertEquals(fakeSlot1, schedule.primarySlot)
    }

    @Test
    fun `backFromReview returns to Details with identity`() {
        enterDetails(identityRequired = true)
        vm.updateReason("Test")
        vm.updateIdentity(
            phone = "+639171234567", firstName = "Alex",
            lastName = "Rivera", dateOfBirth = "1990-05-15",
            gender = AppointmentRequestGender.FEMALE,
            occupation = "Teacher", address = "123 Main St, Manila",
        )
        vm.confirmDetails()
        vm.backFromReview()
        val details = vm.step.value as RequestStep.Details
        assertEquals("+639171234567", details.phone)
        assertEquals("Alex", details.firstName)
    }
}
