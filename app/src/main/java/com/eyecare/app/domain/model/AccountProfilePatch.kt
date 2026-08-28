package com.eyecare.app.domain.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

sealed interface ProfileFieldChange<out T> {
    data object Unchanged : ProfileFieldChange<Nothing>
    data class Set<T>(val value: T) : ProfileFieldChange<T>
}

data class AccountProfilePatch(
    val firstName: ProfileFieldChange<String> = ProfileFieldChange.Unchanged,
    val middleName: ProfileFieldChange<String?> = ProfileFieldChange.Unchanged,
    val lastName: ProfileFieldChange<String> = ProfileFieldChange.Unchanged,
    val dateOfBirth: ProfileFieldChange<String> = ProfileFieldChange.Unchanged,
) {
    fun isEmpty(): Boolean =
        firstName is ProfileFieldChange.Unchanged &&
            middleName is ProfileFieldChange.Unchanged &&
            lastName is ProfileFieldChange.Unchanged &&
            dateOfBirth is ProfileFieldChange.Unchanged

    fun hasDateOfBirthChange(): Boolean =
        dateOfBirth is ProfileFieldChange.Set

    fun toJsonString(): String = Json.encodeToString(AccountProfilePatchSerializer, this)

    companion object {
        val Empty = AccountProfilePatch()
    }
}

object AccountProfilePatchSerializer : KSerializer<AccountProfilePatch> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("AccountProfilePatch") {
        element<String?>("first_name", isOptional = true)
        element<String?>("middle_name", isOptional = true)
        element<String?>("last_name", isOptional = true)
        element<String?>("date_of_birth", isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: AccountProfilePatch) {
        val jsonEncoder = encoder as? kotlinx.serialization.json.JsonEncoder
            ?: throw SerializationException("AccountProfilePatchSerializer only works with JSON")
        jsonEncoder.encodeJsonElement(value.toJsonElement())
    }

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): AccountProfilePatch {
        throw SerializationException("AccountProfilePatchSerializer does not support deserialization")
    }
}

private fun AccountProfilePatch.toJsonElement(): JsonObject = buildJsonObject {
    when (firstName) {
        is ProfileFieldChange.Set -> put("first_name", JsonPrimitive(firstName.value))
        is ProfileFieldChange.Unchanged -> {}
    }
    when (middleName) {
        is ProfileFieldChange.Set -> put("middle_name", middleName.value?.let { JsonPrimitive(it) } ?: JsonNull)
        is ProfileFieldChange.Unchanged -> {}
    }
    when (lastName) {
        is ProfileFieldChange.Set -> put("last_name", JsonPrimitive(lastName.value))
        is ProfileFieldChange.Unchanged -> {}
    }
    when (dateOfBirth) {
        is ProfileFieldChange.Set -> put("date_of_birth", JsonPrimitive(dateOfBirth.value))
        is ProfileFieldChange.Unchanged -> {}
    }
}
