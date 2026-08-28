package com.eyecare.app.presentation.account

import com.eyecare.app.domain.model.AccountProfilePatch
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.ProfileFieldChange
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class ProfileDraft(
    val firstName: String = "",
    val middleName: String = "",
    val lastName: String = "",
    val dateOfBirth: String = "",
) {
    companion object {
        fun fromAccount(account: PatientAccount) = ProfileDraft(
            firstName = account.firstName.orEmpty(),
            middleName = account.middleName.orEmpty(),
            lastName = account.lastName.orEmpty(),
            dateOfBirth = account.dateOfBirth.orEmpty(),
        )
    }
}

data class ProfileValidation(
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val dateOfBirthError: String? = null,
    val formError: String? = null,
) {
    val isValid: Boolean get() = firstNameError == null && lastNameError == null && dateOfBirthError == null && formError == null
}

object AccountProfileEditor {

    private val MANILA_ZONE = ZoneId.of("Asia/Manila")
    private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private const val MAX_NAME_LENGTH = 255

    fun normalize(draft: ProfileDraft): ProfileDraft = draft.copy(
        firstName = draft.firstName.trim(),
        middleName = draft.middleName.trim(),
        lastName = draft.lastName.trim(),
        dateOfBirth = draft.dateOfBirth.trim(),
    )

    fun validate(draft: ProfileDraft): ProfileValidation {
        val normalized = normalize(draft)
        val firstNameError = when {
            normalized.firstName.isBlank() -> "First name is required"
            normalized.firstName.length > MAX_NAME_LENGTH -> "First name must be at most $MAX_NAME_LENGTH characters"
            else -> null
        }
        val lastNameError = when {
            normalized.lastName.isBlank() -> "Last name is required"
            normalized.lastName.length > MAX_NAME_LENGTH -> "Last name must be at most $MAX_NAME_LENGTH characters"
            else -> null
        }
        val dateOfBirthError = if (normalized.dateOfBirth.isBlank()) {
            null
        } else {
            validateDob(normalized.dateOfBirth)
        }
        return ProfileValidation(
            firstNameError = firstNameError,
            lastNameError = lastNameError,
            dateOfBirthError = dateOfBirthError,
        )
    }

    fun isDirty(draft: ProfileDraft, account: PatientAccount): Boolean {
        val normalized = normalize(draft)
        val accountFirst = account.firstName.orEmpty()
        val accountMiddle = account.middleName.orEmpty()
        val accountLast = account.lastName.orEmpty()
        val accountDob = account.dateOfBirth.orEmpty()
        return normalized.firstName != accountFirst ||
            normalized.middleName != accountMiddle ||
            normalized.lastName != accountLast ||
            normalized.dateOfBirth != accountDob
    }

    fun computePatch(draft: ProfileDraft, account: PatientAccount): AccountProfilePatch {
        val normalized = normalize(draft)
        val accountFirst = account.firstName.orEmpty()
        val accountMiddle = account.middleName.orEmpty()
        val accountLast = account.lastName.orEmpty()
        val accountDob = account.dateOfBirth.orEmpty()

        val firstName = if (normalized.firstName != accountFirst) {
            ProfileFieldChange.Set(normalized.firstName)
        } else {
            ProfileFieldChange.Unchanged
        }

        val middleName = if (normalized.middleName != accountMiddle) {
            val value = normalized.middleName.ifBlank { null }
            ProfileFieldChange.Set(value)
        } else {
            ProfileFieldChange.Unchanged
        }

        val lastName = if (normalized.lastName != accountLast) {
            ProfileFieldChange.Set(normalized.lastName)
        } else {
            ProfileFieldChange.Unchanged
        }

        val dateOfBirth = if (normalized.dateOfBirth != accountDob && normalized.dateOfBirth.isNotBlank()) {
            ProfileFieldChange.Set(normalized.dateOfBirth)
        } else {
            ProfileFieldChange.Unchanged
        }

        return AccountProfilePatch(
            firstName = firstName,
            middleName = middleName,
            lastName = lastName,
            dateOfBirth = dateOfBirth,
        )
    }

    private fun validateDob(dob: String): String? {
        val parsed = try {
            LocalDate.parse(dob, DATE_FORMAT)
        } catch (_: DateTimeParseException) {
            return "Enter a valid date (YYYY-MM-DD)"
        }
        val today = LocalDate.now(MANILA_ZONE)
        if (!parsed.isBefore(today)) {
            return "Date of birth must be before today"
        }
        return null
    }
}
