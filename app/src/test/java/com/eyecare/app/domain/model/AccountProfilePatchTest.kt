package com.eyecare.app.domain.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AccountProfilePatchTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `unchanged fields emit no JSON key`() {
        val patch = AccountProfilePatch.Empty
        val element = json.parseToJsonElement(patch.toJsonString()).jsonObject
        assertTrue(element.isEmpty())
    }

    @Test
    fun `set first name emits first_name key`() {
        val patch = AccountProfilePatch(firstName = ProfileFieldChange.Set("Ana"))
        val element = json.parseToJsonElement(patch.toJsonString()).jsonObject
        assertEquals(JsonPrimitive("Ana"), element["first_name"])
        assertEquals(1, element.size)
    }

    @Test
    fun `set last name emits last_name key`() {
        val patch = AccountProfilePatch(lastName = ProfileFieldChange.Set("Santos"))
        val element = json.parseToJsonElement(patch.toJsonString()).jsonObject
        assertEquals(JsonPrimitive("Santos"), element["last_name"])
        assertEquals(1, element.size)
    }

    @Test
    fun `set middle name to null emits explicit null`() {
        val patch = AccountProfilePatch(middleName = ProfileFieldChange.Set(null))
        val element = json.parseToJsonElement(patch.toJsonString()).jsonObject
        assertEquals(JsonNull, element["middle_name"])
        assertEquals(1, element.size)
    }

    @Test
    fun `set middle name to value emits string`() {
        val patch = AccountProfilePatch(middleName = ProfileFieldChange.Set("Rose"))
        val element = json.parseToJsonElement(patch.toJsonString()).jsonObject
        assertEquals(JsonPrimitive("Rose"), element["middle_name"])
    }

    @Test
    fun `set date of birth emits date_of_birth key`() {
        val patch = AccountProfilePatch(dateOfBirth = ProfileFieldChange.Set("1990-05-15"))
        val element = json.parseToJsonElement(patch.toJsonString()).jsonObject
        assertEquals(JsonPrimitive("1990-05-15"), element["date_of_birth"])
        assertEquals(1, element.size)
    }

    @Test
    fun `mixed changed and unchanged fields only emit changed keys`() {
        val patch = AccountProfilePatch(
            firstName = ProfileFieldChange.Set("Ana"),
            middleName = ProfileFieldChange.Set(null),
            lastName = ProfileFieldChange.Unchanged,
            dateOfBirth = ProfileFieldChange.Set("1990-05-15"),
        )
        val element = json.parseToJsonElement(patch.toJsonString()).jsonObject
        assertEquals(JsonPrimitive("Ana"), element["first_name"])
        assertEquals(JsonNull, element["middle_name"])
        assertEquals(JsonPrimitive("1990-05-15"), element["date_of_birth"])
        assertNull(element["last_name"])
        assertEquals(3, element.size)
    }

    @Test
    fun `isEmpty returns true when all fields unchanged`() {
        assertTrue(AccountProfilePatch.Empty.isEmpty())
    }

    @Test
    fun `isEmpty returns false when any field changed`() {
        assertFalse(AccountProfilePatch(firstName = ProfileFieldChange.Set("A")).isEmpty())
        assertFalse(AccountProfilePatch(middleName = ProfileFieldChange.Set(null)).isEmpty())
        assertFalse(AccountProfilePatch(lastName = ProfileFieldChange.Set("B")).isEmpty())
        assertFalse(AccountProfilePatch(dateOfBirth = ProfileFieldChange.Set("2000-01-01")).isEmpty())
    }

    @Test
    fun `hasDateOfBirthChange returns true only when DOB is set`() {
        assertTrue(AccountProfilePatch(dateOfBirth = ProfileFieldChange.Set("2000-01-01")).hasDateOfBirthChange())
        assertFalse(AccountProfilePatch(firstName = ProfileFieldChange.Set("A")).hasDateOfBirthChange())
        assertFalse(AccountProfilePatch.Empty.hasDateOfBirthChange())
    }

    @Test
    fun `only four backend-allowlisted fields can be serialized`() {
        val patch = AccountProfilePatch(
            firstName = ProfileFieldChange.Set("A"),
            middleName = ProfileFieldChange.Set("B"),
            lastName = ProfileFieldChange.Set("C"),
            dateOfBirth = ProfileFieldChange.Set("2000-01-01"),
        )
        val element = json.parseToJsonElement(patch.toJsonString()).jsonObject
        val allowedKeys = setOf("first_name", "middle_name", "last_name", "date_of_birth")
        assertEquals(allowedKeys, element.keys)
    }
}
