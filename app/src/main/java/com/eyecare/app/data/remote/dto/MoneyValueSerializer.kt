package com.eyecare.app.data.remote.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import java.math.BigDecimal
import java.math.RoundingMode

object MoneyValueSerializer : KSerializer<BigDecimal> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MoneyValue", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BigDecimal {
        val input = when (val element = (decoder as JsonDecoder).decodeJsonElement()) {
            is JsonPrimitive -> element.content
            else -> throw IllegalArgumentException("Expected a JSON primitive for money value")
        }

        val trimmed = input.trim()
        require(trimmed.isNotBlank()) { "Money value must not be blank" }

        return try {
            BigDecimal(trimmed)
        } catch (_: NumberFormatException) {
            throw IllegalArgumentException("Invalid money value: $input")
        }
    }

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeString(value.setScale(2, RoundingMode.HALF_UP).toPlainString())
    }
}
