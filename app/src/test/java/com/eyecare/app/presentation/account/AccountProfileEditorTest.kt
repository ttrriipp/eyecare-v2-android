package com.eyecare.app.presentation.account

import com.eyecare.app.domain.model.LinkedPatient
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.domain.model.ProfileFieldChange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AccountProfileEditorTest {

    private val account = PatientAccount(
        id = 1,
        name = "Ana Reyes",
        firstName = "Ana",
        middleName = null,
        lastName = "Reyes",
        email = "ana@example.com",
        phone = "09171234567",
        role = "patient",
        dateOfBirth = "1990-05-15",
        linkStatus = PatientLinkStatus.LINKED,
        privacyPolicyVersion = "2026-08",
        privacyAcceptedAt = "2026-07-27T10:00:00+08:00",
        linkedPatient = LinkedPatient(
            patientNumber = "PAT-2026-000001",
            fullName = "Ana Reyes",
            dateOfBirth = "1990-05-15",
            gender = "female",
            occupation = "Teacher",
            address = "123 Main St",
            phone = "09171234567",
            contactEmail = "ana@example.com",
        ),
    )

    @Test
    fun `fromAccount initializes draft from account fields`() {
        val draft = ProfileDraft.fromAccount(account)
        assertEquals("Ana", draft.firstName)
        assertEquals("", draft.middleName)
        assertEquals("Reyes", draft.lastName)
        assertEquals("1990-05-15", draft.dateOfBirth)
    }

    @Test
    fun `fromAccount handles null middle name as empty`() {
        val draft = ProfileDraft.fromAccount(account)
        assertEquals("", draft.middleName)
    }

    @Test
    fun `normalize trims whitespace`() {
        val draft = ProfileDraft(firstName = "  Ana  ", middleName = "  Rose  ", lastName = "  Reyes  ", dateOfBirth = "  1990-05-15  ")
        val normalized = AccountProfileEditor.normalize(draft)
        assertEquals("Ana", normalized.firstName)
        assertEquals("Rose", normalized.middleName)
        assertEquals("Reyes", normalized.lastName)
        assertEquals("1990-05-15", normalized.dateOfBirth)
    }

    @Test
    fun `validate requires first name`() {
        val draft = ProfileDraft(firstName = "", lastName = "Reyes")
        val validation = AccountProfileEditor.validate(draft)
        assertEquals("First name is required", validation.firstNameError)
        assertNull(validation.lastNameError)
    }

    @Test
    fun `validate requires last name`() {
        val draft = ProfileDraft(firstName = "Ana", lastName = "")
        val validation = AccountProfileEditor.validate(draft)
        assertNull(validation.firstNameError)
        assertEquals("Last name is required", validation.lastNameError)
    }

    @Test
    fun `validate rejects names over 255 characters`() {
        val longName = "A".repeat(256)
        val draft = ProfileDraft(firstName = longName, lastName = "Reyes")
        val validation = AccountProfileEditor.validate(draft)
        assertEquals("First name must be at most 255 characters", validation.firstNameError)
    }

    @Test
    fun `validate rejects middle names over 255 characters`() {
        val longMiddleName = "A".repeat(256)
        val draft = ProfileDraft(firstName = "Ana", middleName = longMiddleName, lastName = "Reyes")
        val validation = AccountProfileEditor.validate(draft)
        assertEquals("Middle name must be at most 255 characters", validation.middleNameError)
    }

    @Test
    fun `validate accepts blank DOB`() {
        val draft = ProfileDraft(firstName = "Ana", lastName = "Reyes", dateOfBirth = "")
        val validation = AccountProfileEditor.validate(draft)
        assertNull(validation.dateOfBirthError)
    }

    @Test
    fun `validate rejects invalid DOB format`() {
        val draft = ProfileDraft(firstName = "Ana", lastName = "Reyes", dateOfBirth = "not-a-date")
        val validation = AccountProfileEditor.validate(draft)
        assertEquals("Enter a valid date (YYYY-MM-DD)", validation.dateOfBirthError)
    }

    @Test
    fun `validate rejects impossible DOB date`() {
        val draft = ProfileDraft(firstName = "Ana", lastName = "Reyes", dateOfBirth = "2020-02-30")
        val validation = AccountProfileEditor.validate(draft)
        assertEquals("Enter a valid date (YYYY-MM-DD)", validation.dateOfBirthError)
    }

    @Test
    fun `validate rejects DOB today or future`() {
        val today = LocalDate.now(ZoneId.of("Asia/Manila"))
        val draft = ProfileDraft(firstName = "Ana", lastName = "Reyes", dateOfBirth = today.toString())
        val validation = AccountProfileEditor.validate(draft)
        assertEquals("Date of birth must be before today", validation.dateOfBirthError)
    }

    @Test
    fun `validate accepts valid past DOB`() {
        val draft = ProfileDraft(firstName = "Ana", lastName = "Reyes", dateOfBirth = "1990-05-15")
        val validation = AccountProfileEditor.validate(draft)
        assertNull(validation.dateOfBirthError)
        assertTrue(validation.isValid)
    }

    @Test
    fun `isDirty returns false for unchanged draft`() {
        val draft = ProfileDraft.fromAccount(account)
        assertFalse(AccountProfileEditor.isDirty(draft, account))
    }

    @Test
    fun `isDirty returns true for changed first name`() {
        val draft = ProfileDraft.fromAccount(account).copy(firstName = "Maria")
        assertTrue(AccountProfileEditor.isDirty(draft, account))
    }

    @Test
    fun `isDirty returns true for changed middle name`() {
        val draft = ProfileDraft.fromAccount(account).copy(middleName = "Rose")
        assertTrue(AccountProfileEditor.isDirty(draft, account))
    }

    @Test
    fun `isDirty returns true for changed DOB`() {
        val draft = ProfileDraft.fromAccount(account).copy(dateOfBirth = "1995-01-01")
        assertTrue(AccountProfileEditor.isDirty(draft, account))
    }

    @Test
    fun `computePatch returns empty patch for unchanged draft`() {
        val draft = ProfileDraft.fromAccount(account)
        val patch = AccountProfileEditor.computePatch(draft, account)
        assertTrue(patch.isEmpty())
    }

    @Test
    fun `computePatch includes only changed fields`() {
        val draft = ProfileDraft.fromAccount(account).copy(firstName = "Maria")
        val patch = AccountProfileEditor.computePatch(draft, account)
        assertEquals(ProfileFieldChange.Set("Maria"), patch.firstName)
        assertEquals(ProfileFieldChange.Unchanged, patch.middleName)
        assertEquals(ProfileFieldChange.Unchanged, patch.lastName)
        assertEquals(ProfileFieldChange.Unchanged, patch.dateOfBirth)
    }

    @Test
    fun `computePatch sets middle name to null when cleared`() {
        val accountWithMiddle = account.copy(middleName = "Rose")
        val draft = ProfileDraft.fromAccount(accountWithMiddle).copy(middleName = "")
        val patch = AccountProfileEditor.computePatch(draft, accountWithMiddle)
        assertEquals(ProfileFieldChange.Set(null), patch.middleName)
    }

    @Test
    fun `computePatch includes DOB when changed`() {
        val draft = ProfileDraft.fromAccount(account).copy(dateOfBirth = "1995-01-01")
        val patch = AccountProfileEditor.computePatch(draft, account)
        assertEquals(ProfileFieldChange.Set("1995-01-01"), patch.dateOfBirth)
        assertTrue(patch.hasDateOfBirthChange())
    }

    @Test
    fun `computePatch does not include DOB when unchanged`() {
        val draft = ProfileDraft.fromAccount(account)
        val patch = AccountProfileEditor.computePatch(draft, account)
        assertEquals(ProfileFieldChange.Unchanged, patch.dateOfBirth)
        assertFalse(patch.hasDateOfBirthChange())
    }
}
