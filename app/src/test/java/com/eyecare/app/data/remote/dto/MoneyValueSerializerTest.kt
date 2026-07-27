package com.eyecare.app.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class MoneyValueSerializerTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun `decodes numeric string to BigDecimal`() {
        val result = decodeMoney("\"4500.00\"")
        assertEquals(BigDecimal("4500.00"), result)
    }

    @Test
    fun `decodes JSON number to BigDecimal`() {
        val result = decodeMoney("4500.00")
        assertEquals(BigDecimal("4500.00"), result)
    }

    @Test
    fun `string and number produce equal values`() {
        val fromString = decodeMoney("\"4500.00\"")
        val fromNumber = decodeMoney("4500.00")
        assertEquals(fromString, fromNumber)
    }

    @Test
    fun `decodes integer money value`() {
        val result = decodeMoney("5000")
        assertEquals(BigDecimal("5000"), result)
    }

    @Test
    fun `decodes zero`() {
        val result = decodeMoney("0")
        assertEquals(BigDecimal.ZERO, result)
    }

    @Test
    fun `decodes negative value`() {
        val result = decodeMoney("-100.50")
        assertEquals(BigDecimal("-100.50"), result)
    }

    @Test
    fun `rejects blank string`() {
        assertThrows(IllegalArgumentException::class.java) {
            decodeMoney("\"   \"")
        }
    }

    @Test
    fun `rejects empty string`() {
        assertThrows(IllegalArgumentException::class.java) {
            decodeMoney("\"\"")
        }
    }

    @Test
    fun `rejects non-numeric string`() {
        assertThrows(IllegalArgumentException::class.java) {
            decodeMoney("\"abc\"")
        }
    }

    @Test
    fun `serializes BigDecimal with two decimal places`() {
        val value = BigDecimal("4500")
        val encoded = json.encodeToString(MoneyValueSerializer, value)
        assertEquals("\"4500.00\"", encoded)
    }

    @Test
    fun `serializes BigDecimal rounding to two places`() {
        val value = BigDecimal("4500.1")
        val encoded = json.encodeToString(MoneyValueSerializer, value)
        assertEquals("\"4500.10\"", encoded)
    }

    private fun decodeMoney(raw: String): BigDecimal {
        return json.decodeFromString(MoneyValueSerializer, raw)
    }
}
