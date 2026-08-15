package com.eyecare.app.presentation.messaging

import com.eyecare.app.domain.model.Message
import com.eyecare.app.domain.model.SenderType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MessageTimelineTest {

    private fun msg(id: Int, createdAt: String, body: String = "msg$id") = Message(
        id = id,
        senderId = 1,
        senderType = SenderType.PATIENT,
        body = body,
        readAt = null,
        createdAt = createdAt,
        attachments = emptyList(),
    )

    @Test
    fun `newest-first input becomes chronological output`() {
        val newest = msg(3, "2026-08-15T12:00:00+08:00")
        val middle = msg(2, "2026-08-15T11:00:00+08:00")
        val oldest = msg(1, "2026-08-15T10:00:00+08:00")

        val state = MessageTimeline.fromMessages(listOf(newest, middle, oldest))

        assertEquals(listOf(1, 2, 3), state.chronological.map { it.id })
    }

    @Test
    fun `page overlap deduplication`() {
        val page1 = listOf(
            msg(1, "2026-08-15T10:00:00+08:00"),
            msg(2, "2026-08-15T11:00:00+08:00"),
        )
        val page2 = listOf(
            msg(2, "2026-08-15T11:00:00+08:00"),
            msg(3, "2026-08-15T12:00:00+08:00"),
        )

        val state = MessageTimeline.merge(
            MessageTimeline.fromMessages(page1),
            page2,
        )

        assertEquals(3, state.size)
        assertEquals(listOf(1, 2, 3), state.chronological.map { it.id })
    }

    @Test
    fun `replacement by ID updates message`() {
        val original = msg(1, "2026-08-15T10:00:00+08:00", body = "original")
        val updated = msg(1, "2026-08-15T10:00:00+08:00", body = "updated")

        val state = MessageTimeline.merge(
            MessageTimeline.fromMessages(listOf(original)),
            listOf(updated),
        )

        assertEquals(1, state.size)
        assertEquals("updated", state.chronological[0].body)
    }

    @Test
    fun `equal timestamps ordered by ascending ID`() {
        val a = msg(5, "2026-08-15T10:00:00+08:00")
        val b = msg(3, "2026-08-15T10:00:00+08:00")
        val c = msg(7, "2026-08-15T10:00:00+08:00")

        val state = MessageTimeline.fromMessages(listOf(a, b, c))

        assertEquals(listOf(3, 5, 7), state.chronological.map { it.id })
    }

    @Test
    fun `equivalent instants with different timezone offsets`() {
        val utc = msg(1, "2026-08-15T02:00:00Z")
        val plus8 = msg(2, "2026-08-15T10:00:00+08:00")
        val plus545 = msg(3, "2026-08-15T07:45:00+05:45")

        val state = MessageTimeline.fromMessages(listOf(plus8, plus545, utc))

        // utc 02:00Z = plus8 10:00+08 = 02:00Z, plus545 07:45+05:45 = 02:00Z
        // All same instant; tie broken by ascending ID
        assertEquals(listOf(1, 2, 3), state.chronological.map { it.id })
    }

    @Test
    fun `malformed timestamps fail predictably`() {
        val bad = msg(1, "not-a-timestamp")

        assertThrows(IllegalArgumentException::class.java) {
            MessageTimeline.fromMessages(listOf(bad))
        }
    }

    @Test
    fun `empty input returns empty state`() {
        val state = MessageTimeline.fromMessages(emptyList())

        assertTrue(state.isEmpty)
        assertEquals(0, state.size)
        assertEquals(emptyList<Message>(), state.chronological)
    }

    @Test
    fun `hasNewMessages detects new IDs`() {
        val current = MessageTimeline.fromMessages(
            listOf(msg(1, "2026-08-15T10:00:00+08:00")),
        )
        val polled = listOf(
            msg(1, "2026-08-15T10:00:00+08:00"),
            msg(2, "2026-08-15T11:00:00+08:00"),
        )

        assertTrue(MessageTimeline.hasNewMessages(current, polled))
    }

    @Test
    fun `hasNewMessages returns false for identical set`() {
        val current = MessageTimeline.fromMessages(
            listOf(msg(1, "2026-08-15T10:00:00+08:00")),
        )
        val polled = listOf(msg(1, "2026-08-15T10:00:00+08:00"))

        assertFalse(MessageTimeline.hasNewMessages(current, polled))
    }

    @Test
    fun `hasNewMessages returns false for empty polled page`() {
        val current = MessageTimeline.fromMessages(
            listOf(msg(1, "2026-08-15T10:00:00+08:00")),
        )

        assertFalse(MessageTimeline.hasNewMessages(current, emptyList()))
    }

    @Test
    fun `merge retains older loaded history`() {
        val older = (1..30).map {
            msg(it, "2026-08-15T10:${String.format("%02d", it)}:00+08:00")
        }
        val page1Refresh = (1..50).map {
            msg(it, "2026-08-15T10:${String.format("%02d", it)}:00+08:00")
        }

        val initial = MessageTimeline.fromMessages(older)
        val afterPoll = MessageTimeline.merge(initial, page1Refresh)

        assertEquals(50, afterPoll.size)
    }
}
