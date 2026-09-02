package com.eyecare.app.presentation.appointments.requests

import androidx.lifecycle.SavedStateHandle
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentRequestAvailability
import com.eyecare.app.domain.model.AppointmentRequestGender
import com.eyecare.app.domain.model.AppointmentRequestIdentity
import com.eyecare.app.domain.model.AppointmentRequestStatus
import com.eyecare.app.domain.model.AppointmentRequestTypeSummary
import com.eyecare.app.domain.model.AppointmentType
import com.eyecare.app.domain.model.AvailabilitySlot
import com.eyecare.app.domain.repository.AppointmentRequestRepository
import com.eyecare.app.presentation.appointments.DayAvailability
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
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

    private val presetType = normalType.copy(
        visitReasonPresets = listOf(
            com.eyecare.app.domain.model.VisitReasonPreset(
                id = 21,
                label = "Blurred or reduced vision",
            ),
            com.eyecare.app.domain.model.VisitReasonPreset(
                id = 22,
                label = "Eye pain or discomfort",
            ),
        ),
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

    private val fakeSlotOtherDate = AvailabilitySlot(
        startsAt = "2026-08-11T09:00:00+08:00",
        endsAt = "2026-08-11T09:45:00+08:00",
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
        expiresAt = "2026-08-10T09:00:00+08:00", cancelledAt = null, rejectionReason = null,
        createdAt = "2026-08-09T10:00:00+08:00", appointmentId = null,
    )

    private fun newViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) =
        RequestAppointmentViewModel(repo, savedStateHandle)

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mockk()
        coEvery { repo.getAppointmentTypes() } returns Result.success(listOf(normalType, referralType))
        // The schedule step prefetches a whole week around today, so every date must answer.
        // Individual tests override the specific dates they assert on.
        coEvery { repo.getAvailability(any(), any()) } returns Result.success(fakeAvailability())
        vm = newViewModel()
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    // ── Type step ──

    @Test
    fun `initial step is Type and loads types`() {
        val step = vm.step.value as RequestStep.Type
        assertEquals(2, step.types.size)
        assertFalse(step.isLoading)
    }

    @Test
    fun `type load failure shows retry`() {
        coEvery { repo.getAppointmentTypes() } returns Result.failure(Exception("Network"))
        vm = newViewModel()
        val step = vm.step.value as RequestStep.Type
        assertEquals("We couldn't load appointment types. Please try again.", step.error)
        assertTrue(step.types.isEmpty())
    }

    @Test
    fun `unknown type load error uses patient safe copy`() {
        coEvery { repo.getAppointmentTypes() } returns Result.failure(
            ApiDomainError(500, "INTERNAL_ERROR", "SQL exception with private details"),
        )

        vm = newViewModel()

        val step = vm.step.value as RequestStep.Type
        assertEquals("We couldn't load appointment types. Please try again.", step.error)
    }

    @Test
    fun `stale type catalog response is ignored`() {
        val firstResponse = CompletableDeferred<Result<List<AppointmentType>>>()
        val latestResponse = CompletableDeferred<Result<List<AppointmentType>>>()
        var requestCount = 0

        coEvery { repo.getAppointmentTypes() } coAnswers {
            val response = if (requestCount++ == 0) firstResponse else latestResponse
            withContext(NonCancellable) { response.await() }
        }

        vm = newViewModel()
        vm.retryTypes()

        latestResponse.complete(Result.success(listOf(referralType)))
        firstResponse.complete(Result.success(listOf(normalType)))

        val step = vm.step.value as RequestStep.Type
        assertEquals(listOf(referralType), step.types)
    }

    @Test
    fun `selectType sets selection`() {
        vm.selectType(normalType)
        assertEquals(normalType, (vm.step.value as RequestStep.Type).selectedType)
    }

    @Test
    fun `confirmType advances to Schedule`() {
        vm.selectType(normalType)
        vm.confirmType(identityRequired = false)
        val schedule = vm.step.value as RequestStep.Schedule
        assertEquals(normalType, schedule.selectedType)
        assertFalse(schedule.identityRequired)
    }

    @Test
    fun `confirmType without selection does nothing`() {
        vm.confirmType(identityRequired = false)
        assertTrue(vm.step.value is RequestStep.Type)
    }

    @Test
    fun `confirmType carries identityRequired into the flow`() {
        vm.selectType(normalType)
        vm.confirmType(identityRequired = true)
        assertTrue((vm.step.value as RequestStep.Schedule).identityRequired)
    }

    // ── Step labelling ──

    @Test
    fun `linked patients get four steps and unlinked get five`() {
        assertEquals(listOf("Type", "Schedule", "Reason", "Review"), requestStepLabels(false))
        assertEquals(
            listOf("Type", "Schedule", "Reason", "Details", "Review"),
            requestStepLabels(true),
        )
    }

    @Test
    fun `review is the last step in both variants`() {
        assertEquals(3, requestStepIndex(RequestStepId.REVIEW, identityRequired = false))
        assertEquals(4, requestStepIndex(RequestStepId.REVIEW, identityRequired = true))
    }

    // ── Schedule step ──

    private fun enterSchedule(
        identityRequired: Boolean = false,
        type: AppointmentType = normalType,
    ): RequestStep.Schedule {
        vm.selectType(type)
        vm.confirmType(identityRequired = identityRequired)
        coEvery { repo.getAvailability("2026-08-10", type.id) } returns
            Result.success(fakeAvailability(type.id))
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
        vm.selectType(normalType)
        vm.confirmType(identityRequired = false)
        coEvery { repo.getAvailability("2026-08-10", 1) } returns Result.failure(Exception("Network"))
        vm.selectDate("2026-08-10")
        val step = vm.step.value as RequestStep.Schedule
        assertEquals("We couldn't load times for this day. Please try again.", step.availabilityError)
    }

    @Test
    fun `stale response for repeated date is ignored`() {
        val firstResponse = CompletableDeferred<Result<AppointmentRequestAvailability>>()
        val latestResponse = CompletableDeferred<Result<AppointmentRequestAvailability>>()
        var requestCount = 0

        vm.selectType(normalType)
        vm.confirmType(identityRequired = false)

        coEvery { repo.getAvailability("2026-08-10", 1) } coAnswers {
            val response = if (requestCount++ == 0) firstResponse else latestResponse
            withContext(NonCancellable) { response.await() }
        }
        coEvery { repo.getAvailability("2026-08-11", 1) } returns Result.success(
            fakeAvailability().copy(
                date = "2026-08-11",
                generatedAt = "2026-08-09T11:00:00+08:00",
                slots = listOf(fakeSlotOtherDate),
            ),
        )

        vm.selectDate("2026-08-10")
        vm.selectDate("2026-08-11")
        vm.selectDate("2026-08-10")

        latestResponse.complete(Result.success(fakeAvailability().copy(generatedAt = "latest")))
        firstResponse.complete(Result.success(fakeAvailability().copy(generatedAt = "stale")))

        assertEquals("latest", (vm.step.value as RequestStep.Schedule).availability?.generatedAt)
    }

    @Test
    fun `week prefetch classifies open closed and full days`() {
        vm.selectType(normalType)
        coEvery { repo.getAvailability(any(), 1) } returns Result.success(
            fakeAvailability().copy(dayStatus = "closed", slots = emptyList()),
        )
        vm.confirmType(identityRequired = false)

        val schedule = vm.step.value as RequestStep.Schedule
        assertTrue(schedule.dayAvailability.values.isNotEmpty())
        assertTrue(schedule.dayAvailability.values.all { it == DayAvailability.CLOSED })
    }

    @Test
    fun `week prefetch marks a day with no free slots as full`() {
        vm.selectType(normalType)
        coEvery { repo.getAvailability(any(), 1) } returns Result.success(
            fakeAvailability().copy(dayStatus = "open", slots = listOf(unavailableSlot)),
        )
        vm.confirmType(identityRequired = false)

        val schedule = vm.step.value as RequestStep.Schedule
        assertTrue(schedule.dayAvailability.values.all { it == DayAvailability.FULL })
    }

    @Test
    fun `selectPrimarySlot selects available slot`() {
        enterSchedule()
        vm.selectPrimarySlot(fakeSlot1)
        assertEquals(fakeSlot1, (vm.step.value as RequestStep.Schedule).primarySlot)
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
        vm.selectPrimarySlot(fakeSlot2)
        vm.toggleAlternative(fakeSlot1)
        vm.selectPrimarySlot(fakeSlot1)
        val step = vm.step.value as RequestStep.Schedule
        assertEquals(fakeSlot1, step.primarySlot)
        assertTrue(step.alternativeSlots.none { it.startsAt == fakeSlot1.startsAt })
    }

    // ── Two-phase alternatives ──

    @Test
    fun `schedule starts in preferred phase`() {
        assertEquals(SchedulePhase.PREFERRED, enterSchedule().phase)
    }

    @Test
    fun `alternatives phase requires a preferred slot first`() {
        enterSchedule()
        vm.startAddingAlternatives()
        assertEquals(SchedulePhase.PREFERRED, (vm.step.value as RequestStep.Schedule).phase)

        vm.selectPrimarySlot(fakeSlot1)
        vm.startAddingAlternatives()
        assertEquals(SchedulePhase.ALTERNATIVES, (vm.step.value as RequestStep.Schedule).phase)
    }

    @Test
    fun `finishAddingAlternatives returns to preferred phase`() {
        enterSchedule()
        vm.selectPrimarySlot(fakeSlot1)
        vm.startAddingAlternatives()
        vm.finishAddingAlternatives()
        assertEquals(SchedulePhase.PREFERRED, (vm.step.value as RequestStep.Schedule).phase)
    }

    @Test
    fun `toggleAlternative adds distinct slots`() {
        enterSchedule()
        vm.selectPrimarySlot(fakeSlot1)
        vm.toggleAlternative(fakeSlot2)
        val step = vm.step.value as RequestStep.Schedule
        assertEquals(listOf(fakeSlot2), step.alternativeSlots)
    }

    @Test
    fun `toggleAlternative removes an already chosen slot`() {
        enterSchedule()
        vm.selectPrimarySlot(fakeSlot1)
        vm.toggleAlternative(fakeSlot2)
        vm.toggleAlternative(fakeSlot2)
        assertTrue((vm.step.value as RequestStep.Schedule).alternativeSlots.isEmpty())
    }

    @Test
    fun `toggleAlternative rejects the preferred slot`() {
        enterSchedule()
        vm.selectPrimarySlot(fakeSlot1)
        vm.toggleAlternative(fakeSlot1)
        assertTrue((vm.step.value as RequestStep.Schedule).alternativeSlots.isEmpty())
    }

    @Test
    fun `toggleAlternative rejects unavailable slot`() {
        enterSchedule()
        vm.selectPrimarySlot(fakeSlot1)
        vm.toggleAlternative(unavailableSlot)
        assertTrue((vm.step.value as RequestStep.Schedule).alternativeSlots.isEmpty())
    }

    @Test
    fun `toggleAlternative caps at two`() {
        enterSchedule()
        vm.selectPrimarySlot(fakeSlot1)
        vm.toggleAlternative(fakeSlot2)
        vm.toggleAlternative(fakeSlot3)
        val extraSlot = AvailabilitySlot(
            startsAt = "2026-08-10T13:00:00+08:00",
            endsAt = "2026-08-10T13:45:00+08:00",
            available = true, reason = null,
        )
        vm.toggleAlternative(extraSlot)
        assertEquals(2, (vm.step.value as RequestStep.Schedule).alternativeSlots.size)
    }

    @Test
    fun `changing date preserves primary and allows an alternative from the new date`() {
        enterSchedule()
        vm.selectPrimarySlot(fakeSlot1)
        coEvery { repo.getAvailability("2026-08-11", 1) } returns Result.success(
            fakeAvailability().copy(date = "2026-08-11", slots = listOf(fakeSlotOtherDate)),
        )

        vm.selectDate("2026-08-11")
        vm.toggleAlternative(fakeSlotOtherDate)

        val schedule = vm.step.value as RequestStep.Schedule
        assertEquals(fakeSlot1, schedule.primarySlot)
        assertEquals(listOf(fakeSlotOtherDate), schedule.alternativeSlots)
    }

    @Test
    fun `removeAlternative removes by startsAt`() {
        enterSchedule()
        vm.selectPrimarySlot(fakeSlot1)
        vm.toggleAlternative(fakeSlot2)
        vm.toggleAlternative(fakeSlot3)
        vm.removeAlternative(fakeSlot2)
        val step = vm.step.value as RequestStep.Schedule
        assertEquals(listOf(fakeSlot3.startsAt), step.alternativeSlots.map { it.startsAt })
    }

    @Test
    fun `backToType keeps the chosen type highlighted`() {
        enterSchedule()
        vm.backToType()
        val step = vm.step.value as RequestStep.Type
        assertEquals(normalType, step.selectedType)
        assertEquals(2, step.types.size)
    }

    @Test
    fun `backToType drops a selection the catalog no longer offers`() {
        enterSchedule()
        coEvery { repo.getAppointmentTypes() } returns Result.success(listOf(referralType))
        vm.backToType()
        assertNull((vm.step.value as RequestStep.Type).selectedType)
    }

    @Test
    fun `changing type clears slots and availability`() {
        enterSchedule()
        vm.selectPrimarySlot(fakeSlot1)
        vm.toggleAlternative(fakeSlot2)
        vm.backToType()
        vm.selectType(referralType)
        vm.confirmType(identityRequired = false)
        val schedule = vm.step.value as RequestStep.Schedule
        assertNull(schedule.primarySlot)
        assertTrue(schedule.alternativeSlots.isEmpty())
        assertNull(schedule.availability)
    }

    // ── Reason step ──

    private fun enterReason(
        identityRequired: Boolean = false,
        type: AppointmentType = normalType,
    ): RequestStep.Reason {
        enterSchedule(identityRequired = identityRequired, type = type)
        vm.selectPrimarySlot(fakeSlot1)
        vm.toggleAlternative(fakeSlot2)
        vm.confirmSchedule()
        return vm.step.value as RequestStep.Reason
    }

    @Test
    fun `confirmSchedule moves to Reason`() {
        val reason = enterReason()
        assertEquals("2026-08-10", reason.date)
        assertEquals(fakeSlot1, reason.primarySlot)
        assertEquals(1, reason.alternativeSlots.size)
    }

    @Test
    fun `confirmSchedule uses primary date after browsing another date`() {
        enterSchedule()
        vm.selectPrimarySlot(fakeSlot1)
        coEvery { repo.getAvailability("2026-08-11", 1) } returns Result.success(
            fakeAvailability().copy(date = "2026-08-11", slots = listOf(fakeSlotOtherDate)),
        )
        vm.selectDate("2026-08-11")
        vm.toggleAlternative(fakeSlotOtherDate)

        vm.confirmSchedule()

        assertEquals("2026-08-10", (vm.step.value as RequestStep.Reason).date)
    }

    @Test
    fun `confirmSchedule without a preferred slot does nothing`() {
        enterSchedule()
        vm.confirmSchedule()
        assertTrue(vm.step.value is RequestStep.Schedule)
    }

    @Test
    fun `empty reason shows validation error`() {
        enterReason()
        vm.confirmReason()
        assertEquals(
            "Tell the clinic what you'd like to be seen for.",
            (vm.step.value as RequestStep.Reason).reasonError,
        )
    }

    @Test
    fun `preset selection with optional details composes the final reason`() {
        enterReason(type = presetType)
        vm.selectReasonChoice(VisitReasonChoice.Preset(presetId = 21))
        vm.updateReason("mostly in my left eye")

        vm.confirmReason()

        assertEquals(
            "Blurred or reduced vision: mostly in my left eye",
            (vm.step.value as RequestStep.Review).reason,
        )
    }

    @Test
    fun `preset selection without details is a valid reason`() {
        enterReason(type = presetType)
        vm.selectReasonChoice(VisitReasonChoice.Preset(presetId = 22))

        vm.confirmReason()

        assertEquals("Eye pain or discomfort", (vm.step.value as RequestStep.Review).reason)
    }

    @Test
    fun `preset-only selection does not report unsaved input`() {
        // A chip tap alone is a one-tap, trivially redoable choice, so it shouldn't trigger a
        // "Discard this request?" dialog on Back the way typed text does.
        enterReason(type = presetType)
        vm.selectReasonChoice(VisitReasonChoice.Preset(presetId = 22))

        assertFalse((vm.step.value as RequestStep.Reason).hasUnsavedInput)
    }

    @Test
    fun `typed reason details report unsaved input`() {
        enterReason(type = presetType)
        vm.selectReasonChoice(VisitReasonChoice.Preset(presetId = 22))
        vm.updateReason("some detail")

        assertTrue((vm.step.value as RequestStep.Reason).hasUnsavedInput)
    }

    @Test
    fun `preset type requires an explicit choice`() {
        enterReason(type = presetType)
        vm.updateReason("typed without choosing a category")

        vm.confirmReason()

        val reason = vm.step.value as RequestStep.Reason
        assertEquals(VisitReasonCompositionError.CHOICE_REQUIRED, reason.reasonErrorCode)
        assertEquals("typed without choosing a category", reason.reason)
    }

    @Test
    fun `other requires a custom description`() {
        enterReason(type = presetType)
        vm.selectReasonChoice(VisitReasonChoice.Other)

        vm.confirmReason()

        val reason = vm.step.value as RequestStep.Reason
        assertEquals(VisitReasonCompositionError.REASON_REQUIRED, reason.reasonErrorCode)
    }

    @Test
    fun `reason selection and details survive returning to schedule`() {
        enterReason(type = presetType)
        vm.selectReasonChoice(VisitReasonChoice.Preset(presetId = 21))
        vm.updateReason("for two weeks")

        vm.backToSchedule()

        val schedule = vm.step.value as RequestStep.Schedule
        assertEquals(VisitReasonChoice.Preset(presetId = 21), schedule.reasonChoice)
        assertEquals("for two weeks", schedule.reasonDraft)
    }

    @Test
    fun `reason selection and details survive returning from linked review`() {
        enterReason(type = presetType)
        vm.selectReasonChoice(VisitReasonChoice.Preset(presetId = 21))
        vm.updateReason("for two weeks")
        vm.confirmReason()

        vm.backFromReview()

        val reason = vm.step.value as RequestStep.Reason
        assertEquals(VisitReasonChoice.Preset(presetId = 21), reason.reasonChoice)
        assertEquals("for two weeks", reason.reason)
    }

    @Test
    fun `changing appointment type clears stale preset identity but preserves final reason`() {
        enterReason(type = presetType)
        vm.selectReasonChoice(VisitReasonChoice.Preset(presetId = 21))
        vm.updateReason("for two weeks")
        vm.backToSchedule()
        vm.backToType()

        vm.selectType(referralType)
        vm.confirmType(identityRequired = false)

        val schedule = vm.step.value as RequestStep.Schedule
        assertEquals(VisitReasonChoice.None, schedule.reasonChoice)
        assertEquals("Blurred or reduced vision: for two weeks", schedule.reasonDraft)
    }

    @Test
    fun `legacy saved reason becomes Other when restored for a preset type`() {
        val saved = SavedStateHandle(
            mapOf(
                "appointment_request_draft" to RequestDraft(
                    appointmentTypeId = presetType.id,
                    reason = "Legacy custom description",
                ),
            ),
        )
        vm = newViewModel(saved)

        vm.selectType(presetType)
        vm.confirmType(identityRequired = false)

        val schedule = vm.step.value as RequestStep.Schedule
        assertEquals(VisitReasonChoice.Other, schedule.reasonChoice)
        assertEquals("Legacy custom description", schedule.reasonDraft)
    }

    @Test
    fun `deactivated preset restores its last composed reason as custom text`() {
        val saved = SavedStateHandle(
            mapOf(
                "appointment_request_draft" to RequestDraft(
                    appointmentTypeId = normalType.id,
                    reason = "for two weeks",
                    composedReason = "Blurred or reduced vision: for two weeks",
                    visitReasonPresetId = 21,
                ),
            ),
        )
        vm = newViewModel(saved)

        vm.selectType(normalType)
        vm.confirmType(identityRequired = false)

        val schedule = vm.step.value as RequestStep.Schedule
        assertEquals(VisitReasonChoice.None, schedule.reasonChoice)
        assertEquals("Blurred or reduced vision: for two weeks", schedule.reasonDraft)
    }

    @Test
    fun `referral type requires referring source`() {
        enterReason(type = referralType)
        vm.updateReason("Blurred vision")
        vm.confirmReason()
        assertEquals(
            "Referral needs a referral. Add who referred you.",
            (vm.step.value as RequestStep.Reason).referringSourceError,
        )
    }

    @Test
    fun `linked patient skips the identity step`() {
        enterReason(identityRequired = false)
        vm.updateReason("Blurred vision")
        vm.confirmReason()
        val review = vm.step.value as RequestStep.Review
        assertNull(review.referringSource)
        assertNull(review.identity)
        assertFalse(review.identityRequired)
    }

    @Test
    fun `unlinked patient goes to the identity step`() {
        enterReason(identityRequired = true)
        vm.updateReason("Blurred vision")
        vm.confirmReason()
        assertTrue(vm.step.value is RequestStep.Identity)
    }

    @Test
    fun `referral type with valid source proceeds`() {
        enterReason(type = referralType)
        vm.updateReason("Blurred vision")
        vm.updateReferringSource("Dr. Smith")
        vm.confirmReason()
        assertEquals("Dr. Smith", (vm.step.value as RequestStep.Review).referringSource)
    }

    @Test
    fun `stale referral cleared when switching to non-referral type`() {
        enterReason(type = referralType)
        vm.updateReason("Test")
        vm.updateReferringSource("Dr. Smith")
        vm.backToSchedule()
        vm.backToType()
        vm.selectType(normalType)
        vm.confirmType(identityRequired = false)
        coEvery { repo.getAvailability("2026-08-10", 1) } returns Result.success(fakeAvailability(1))
        vm.selectDate("2026-08-10")
        vm.selectPrimarySlot(fakeSlot1)
        vm.confirmSchedule()
        assertEquals("", (vm.step.value as RequestStep.Reason).referringSource)
    }

    // ── Identity step ──

    /** The verified phone always arrives from the account; it is never typed on this step. */
    private val accountIdentity = AppointmentRequestIdentity(
        phone = "+639171234567", email = null, firstName = null, middleName = null,
        lastName = null, dateOfBirth = null, gender = null, occupation = null, address = null,
    )

    private fun enterIdentity(): RequestStep.Identity {
        enterReason(identityRequired = true)
        vm.updateReason("Blurred vision")
        vm.confirmReason(initialIdentity = accountIdentity)
        return vm.step.value as RequestStep.Identity
    }

    @Test
    fun `identity validation names every missing field`() {
        enterIdentity()
        vm.confirmIdentity()
        val step = vm.step.value as RequestStep.Identity
        assertEquals("Enter your first name.", step.errors["firstName"])
        assertEquals("Enter your last name.", step.errors["lastName"])
        assertEquals("Choose your date of birth.", step.errors["dateOfBirth"])
        assertEquals("Choose an option.", step.errors["gender"])
        assertEquals("Enter your occupation.", step.errors["occupation"])
        assertEquals("Enter your home address.", step.errors["address"])
    }

    @Test
    fun `identity validation points at the first invalid field`() {
        enterIdentity()
        vm.confirmIdentity()
        assertNotNull((vm.step.value as RequestStep.Identity).focusField)
    }

    @Test
    fun `editing one field clears only that field's error`() {
        enterIdentity()
        vm.confirmIdentity()
        val before = vm.step.value as RequestStep.Identity
        assertTrue(before.errors.containsKey("firstName"))
        assertTrue(before.errors.containsKey("lastName"))

        vm.updateIdentity(firstName = "Alex")

        val after = vm.step.value as RequestStep.Identity
        assertFalse(after.errors.containsKey("firstName"))
        assertTrue(after.errors.containsKey("lastName"))
    }

    @Test
    fun `invalid email is rejected but a blank one is allowed`() {
        enterIdentity()
        vm.updateIdentity(email = "not-an-email")
        vm.confirmIdentity()
        assertEquals(
            "Enter an email like name@example.com, or leave this empty.",
            (vm.step.value as RequestStep.Identity).errors["email"],
        )
    }

    @Test
    fun `future date of birth is rejected`() {
        enterIdentity()
        vm.updateIdentity(dateOfBirth = "2099-01-01")
        vm.confirmIdentity()
        assertEquals(
            "Date of birth must be in the past.",
            (vm.step.value as RequestStep.Identity).errors["dateOfBirth"],
        )
    }

    private fun fillValidIdentity() {
        vm.updateIdentity(
            email = "alex@example.com",
            firstName = "  Alex ", middleName = " ", lastName = " Rivera ",
            dateOfBirth = "1990-05-15", gender = AppointmentRequestGender.FEMALE,
            occupation = " Teacher ", address = " 123 Main St, Manila ",
        )
    }

    @Test
    fun `identity step seeds the verified phone from the account`() {
        assertEquals("+639171234567", enterIdentity().phone)
    }

    @Test
    fun `confirmIdentity moves to Review with normalized identity`() {
        enterIdentity()
        fillValidIdentity()
        vm.confirmIdentity()

        assertEquals(
            AppointmentRequestIdentity(
                phone = "+639171234567", email = "alex@example.com",
                firstName = "Alex", middleName = null, lastName = "Rivera",
                dateOfBirth = "1990-05-15", gender = AppointmentRequestGender.FEMALE,
                occupation = "Teacher", address = "123 Main St, Manila",
            ),
            (vm.step.value as RequestStep.Review).identity,
        )
    }

    @Test
    fun `backFromReview returns to Identity for an unlinked patient`() {
        enterIdentity()
        fillValidIdentity()
        vm.confirmIdentity()
        vm.backFromReview()
        assertEquals("Alex", (vm.step.value as RequestStep.Identity).firstName)
    }

    @Test
    fun `backToReason keeps identity as a draft`() {
        enterIdentity()
        vm.updateIdentity(firstName = "Alex")
        vm.backToReason()
        val reason = vm.step.value as RequestStep.Reason
        assertEquals("Alex", reason.identityDraft?.firstName)
    }

    // ── Review and submit ──

    private fun enterReview(type: AppointmentType = normalType): RequestStep.Review {
        enterReason(type = type)
        vm.updateReason("Blurred vision")
        if (type.requiresReferral) vm.updateReferringSource("Dr. Smith")
        vm.confirmReason()
        return vm.step.value as RequestStep.Review
    }

    @Test
    fun `submit sends type ID, primary, alternatives, reason, and referral`() {
        coEvery { repo.createRequest(any(), any(), any(), any(), any(), any()) } returns
            Result.success(fakeRequest)
        enterReview(type = referralType)
        vm.submit()
        assertEquals(1, (vm.step.value as RequestStep.Success).request.id)
    }

    @Test
    fun `submit preserves submitted alternative preference order`() {
        coEvery { repo.createRequest(any(), any(), any(), any(), any(), any()) } returns
            Result.success(fakeRequest)
        enterSchedule()
        vm.selectPrimarySlot(fakeSlot1)
        vm.toggleAlternative(fakeSlot3)
        vm.toggleAlternative(fakeSlot2)
        vm.confirmSchedule()
        vm.updateReason("Blurred vision")
        vm.confirmReason()

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
    fun `submit SLOT_UNAVAILABLE returns to Schedule and drops the taken slot`() {
        coEvery { repo.createRequest(any(), any(), any(), any(), any(), any()) } returns
            Result.failure(ApiDomainError(422, "SLOT_UNAVAILABLE", "Slot taken."))
        enterReview()
        vm.submit()
        vm.handleSubmissionError()
        val schedule = vm.step.value as RequestStep.Schedule
        assertNull(schedule.primarySlot)
        assertEquals("Blurred vision", schedule.reasonDraft)
    }

    @Test
    fun `ACTIVE_REQUEST_LIMIT_REACHED is not retryable and keeps the draft`() {
        coEvery { repo.createRequest(any(), any(), any(), any(), any(), any()) } returns
            Result.failure(ApiDomainError(422, "ACTIVE_REQUEST_LIMIT_REACHED", "Limit reached."))
        enterReview()
        vm.submit()
        val step = vm.step.value as RequestStep.SubmissionError
        assertEquals("ACTIVE_REQUEST_LIMIT_REACHED", step.errorCode)
        assertFalse(step.canRetry)
        assertEquals("Blurred vision", step.reason)
    }

    @Test
    fun `an ordinary failure stays retryable`() {
        coEvery { repo.createRequest(any(), any(), any(), any(), any(), any()) } returns
            Result.failure(ApiDomainError(500, "INTERNAL_ERROR", "Database stack trace"))
        enterReview()
        vm.submit()
        assertTrue((vm.step.value as RequestStep.SubmissionError).canRetry)
    }

    @Test
    fun `linked account identity rejection explains that the request must be restarted`() {
        coEvery { repo.createRequest(any(), any(), any(), any(), any(), any()) } returns
            Result.failure(ApiDomainError(422, "IDENTITY_NOT_ALLOWED", "Identity is not allowed."))
        enterReview()

        vm.submit()

        assertEquals(
            "Your account is already linked. Please start the appointment request again.",
            (vm.step.value as RequestStep.SubmissionError).errorMessage,
        )
    }

    @Test
    fun `identity rejection drops identity before retrying the linked request`() {
        coEvery { repo.createRequest(any(), any(), any(), any(), any(), any()) } returns
            Result.failure(ApiDomainError(422, "IDENTITY_NOT_ALLOWED", "Identity is not allowed."))
        enterIdentity()
        fillValidIdentity()
        vm.confirmIdentity()

        vm.submit()
        vm.handleSubmissionError()

        val review = vm.step.value as RequestStep.Review
        assertFalse(review.identityRequired)
        assertNull(review.identity)
    }

    @Test
    fun `backFromSubmissionError returns to Review with everything intact`() {
        coEvery { repo.createRequest(any(), any(), any(), any(), any(), any()) } returns
            Result.failure(ApiDomainError(422, "ACTIVE_REQUEST_LIMIT_REACHED", "Limit reached."))
        enterReview()
        vm.submit()
        vm.backFromSubmissionError()
        val review = vm.step.value as RequestStep.Review
        assertEquals("Blurred vision", review.reason)
        assertEquals(fakeSlot1, review.primarySlot)
        assertFalse(review.isSubmitting)
    }

    @Test
    fun `type validation failure refreshes catalog and explains selection is unavailable`() {
        coEvery { repo.createRequest(any(), any(), any(), any(), any(), any()) } returns Result.failure(
            ApiDomainError(
                httpStatus = 422,
                code = "VALIDATION",
                message = "Invalid appointment type.",
                fieldErrors = mapOf("appointment_type_id" to listOf("The selected type is unavailable.")),
            ),
        )

        enterReview()
        vm.submit()
        vm.handleSubmissionError()

        val step = vm.step.value as RequestStep.Type
        assertEquals("That appointment type is no longer offered. Please choose another.", step.notice)
        assertEquals(2, step.types.size)
        assertNull(step.selectedType)
    }

    @Test
    fun `referral validation failure returns to reason with field feedback`() {
        coEvery { repo.createRequest(any(), any(), any(), any(), any(), any()) } returns Result.failure(
            ApiDomainError(
                httpStatus = 422,
                code = "VALIDATION",
                message = "Invalid referral source.",
                fieldErrors = mapOf("referring_source" to listOf("The referral source is invalid.")),
            ),
        )

        enterReview(type = referralType)
        vm.submit()
        vm.handleSubmissionError()

        val step = vm.step.value as RequestStep.Reason
        assertEquals("Blurred vision", step.reason)
        assertEquals("Dr. Smith", step.referringSource)
        assertEquals(
            "The clinic couldn't verify this referral. Please check it.",
            step.referringSourceError,
        )
    }

    @Test
    fun `unknown submission error uses patient safe copy`() {
        coEvery { repo.createRequest(any(), any(), any(), any(), any(), any()) } returns
            Result.failure(ApiDomainError(500, "INTERNAL_ERROR", "Database stack trace"))

        enterReview()
        vm.submit()

        assertEquals(
            "We couldn't send your request. Please try again.",
            (vm.step.value as RequestStep.SubmissionError).errorMessage,
        )
    }

    // ── Back navigation ──

    @Test
    fun `backToSchedule preserves draft`() {
        enterReason()
        vm.updateReason("My reason")
        vm.backToSchedule()
        val schedule = vm.step.value as RequestStep.Schedule
        assertEquals("My reason", schedule.reasonDraft)
        assertEquals(fakeSlot1, schedule.primarySlot)
    }

    @Test
    fun `backFromReview returns to Reason for a linked patient`() {
        enterReview()
        vm.backFromReview()
        assertEquals("Blurred vision", (vm.step.value as RequestStep.Reason).reason)
    }

    @Test
    fun `editFromReview jumps straight to the schedule step`() {
        enterReview()
        vm.editFromReview(RequestStepId.SCHEDULE)
        val schedule = vm.step.value as RequestStep.Schedule
        assertEquals(fakeSlot1, schedule.primarySlot)
        assertEquals("Blurred vision", schedule.reasonDraft)
    }

    @Test
    fun `editFromReview jumps straight to the reason step`() {
        enterReview()
        vm.editFromReview(RequestStepId.REASON)
        assertEquals("Blurred vision", (vm.step.value as RequestStep.Reason).reason)
    }

    @Test
    fun `steps holding typed work report unsaved input`() {
        val reason = enterReason()
        assertFalse(reason.hasUnsavedInput)
        vm.updateReason("Something")
        assertTrue((vm.step.value as RequestStep.Reason).hasUnsavedInput)
    }
}
