package com.eyecare.app.presentation.messaging

import com.eyecare.app.domain.model.Message
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object MessageTimeline {

    data class State(
        val byId: Map<Int, Message> = emptyMap(),
        val chronological: List<Message> = emptyList(),
    ) {
        val size: Int get() = byId.size
        val isEmpty: Boolean get() = byId.isEmpty()
    }

    fun merge(current: State, incoming: List<Message>): State {
        if (incoming.isEmpty()) return current
        val merged = current.byId.toMutableMap()
        for (msg in incoming) {
            merged[msg.id] = msg
        }
        return State(
            byId = merged,
            chronological = orderByInstant(merged.values),
        )
    }

    fun fromMessages(messages: List<Message>): State = merge(State(), messages)

    fun hasNewMessages(current: State, polledPage: List<Message>): Boolean {
        if (polledPage.isEmpty()) return false
        val currentIds = current.byId.keys
        return polledPage.any { it.id !in currentIds }
    }

    fun orderByInstant(messages: Collection<Message>): List<Message> {
        data class Tagged(val message: Message, val instant: Instant)

        return messages
            .map { Tagged(it, parseInstant(it.createdAt)) }
            .sortedWith(compareBy<Tagged> { it.instant }.thenBy { it.message.id })
            .map { it.message }
    }

    fun parseInstant(isoTimestamp: String): Instant {
        return try {
            OffsetDateTime.parse(
                isoTimestamp,
                DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            ).toInstant()
        } catch (_: DateTimeParseException) {
            try {
                OffsetDateTime.parse(isoTimestamp).toInstant()
            } catch (_: DateTimeParseException) {
                throw IllegalArgumentException(
                    "Malformed timestamp: '$isoTimestamp'. Expected ISO-8601 with offset.",
                )
            }
        }
    }
}
