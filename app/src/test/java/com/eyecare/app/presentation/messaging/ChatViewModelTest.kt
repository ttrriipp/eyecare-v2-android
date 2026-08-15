package com.eyecare.app.presentation.messaging

import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import com.eyecare.app.domain.model.Conversation
import com.eyecare.app.domain.model.ConversationAccessLevel
import com.eyecare.app.domain.model.ConversationCapabilities
import com.eyecare.app.domain.model.Message
import com.eyecare.app.domain.model.MessagePage
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.domain.model.SenderType
import com.eyecare.app.domain.repository.AuthRepository
import com.eyecare.app.domain.repository.ChatRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: ChatRepository
    private lateinit var authRepo: AuthRepository

    private val fakeConversation = Conversation(
        id = 1,
        patientId = null,
        unreadCount = 0,
        createdAt = "2026-10-24T10:00:00Z",
        accessLevel = ConversationAccessLevel.LINKED_PATIENT,
        capabilities = ConversationCapabilities(canUploadAttachments = true),
    )
    private val fakeMessage = Message(1, 42, SenderType.PATIENT, "Hello", null, "2026-10-24T10:00:00Z", emptyList())
    private val fakeAccount = PatientAccount(
        id = 42,
        name = "Test Patient",
        firstName = "Test",
        middleName = null,
        lastName = "Patient",
        email = null,
        phone = null,
        role = "patient",
        dateOfBirth = null,
        linkStatus = PatientLinkStatus.LINKED,
        privacyPolicyVersion = null,
        privacyAcceptedAt = null,
        linkedPatient = null,
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mockk()
        authRepo = mockk()
        coEvery { authRepo.getMe() } returns Result.success(fakeAccount)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun vm() = ChatViewModel(repo, authRepo)

    @Test
    fun `initial state is Loading then loads conversation and messages`() = runTest {
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages() } returns Result.success(MessagePage(listOf(fakeMessage), null, false))
        val vm = vm()

        try {
            vm.uiState.test {
                assertInstanceOf(ChatUiState.Loading::class.java, awaitItem())
                dispatcher.scheduler.runCurrent()
                val state = awaitItem() as ChatUiState.Success
                assertEquals(1, state.messages.size)
                assertEquals("Hello", state.messages[0].body)
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `initial chronological order from timeline`() = runTest {
        val newest = fakeMessage.copy(id = 3, body = "newest", createdAt = "2026-10-24T12:00:00Z")
        val middle = fakeMessage.copy(id = 2, body = "middle", createdAt = "2026-10-24T11:00:00Z")
        val oldest = fakeMessage.copy(id = 1, body = "oldest", createdAt = "2026-10-24T10:00:00Z")
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages() } returns Result.success(
            MessagePage(listOf(newest, middle, oldest), null, false),
        )
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            val state = vm.uiState.value as ChatUiState.Success
            assertEquals("oldest", state.messages[0].body)
            assertEquals("middle", state.messages[1].body)
            assertEquals("newest", state.messages[2].body)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `send message merges by ID`() = runTest {
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages() } returns Result.success(MessagePage(emptyList(), null, false))
        coEvery { repo.sendMessage("Hi there") } returns Result.success(
            fakeMessage.copy(id = 2, body = "Hi there")
        )
        val vm = vm()

        try {
            vm.uiState.test {
                awaitItem() // Loading
                dispatcher.scheduler.runCurrent()
                awaitItem() // Success with empty messages

                vm.sendMessage("Hi there")
                dispatcher.scheduler.runCurrent()

                val sending = awaitItem() as ChatUiState.Success
                if (sending.isSending) {
                    dispatcher.scheduler.runCurrent()
                    val state = awaitItem() as ChatUiState.Success
                    assertEquals(1, state.messages.size)
                    assertEquals("Hi there", state.messages[0].body)
                } else {
                    assertEquals(1, sending.messages.size)
                    assertEquals("Hi there", sending.messages[0].body)
                }
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `send empty message does nothing`() = runTest {
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages() } returns Result.success(MessagePage(emptyList(), null, false))
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            val stateBefore = vm.uiState.value
            vm.sendMessage("   ")
            dispatcher.scheduler.runCurrent()
            assertEquals(stateBefore, vm.uiState.value)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `cursor success stores nextCursor and hasMore`() = runTest {
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages(null) } returns Result.success(
            MessagePage(listOf(fakeMessage), "cursor-abc", true),
        )
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            val state = vm.uiState.value as ChatUiState.Success
            assertEquals("cursor-abc", state.nextCursor)
            assertTrue(state.hasMore)
            assertFalse(state.isLoadingOlder)
            assertNull(state.olderPageError)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `load older advances cursor on success`() = runTest {
        val olderMsg = fakeMessage.copy(id = 0, body = "older", createdAt = "2026-10-24T09:00:00Z")
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages(null) } returns Result.success(
            MessagePage(listOf(fakeMessage), "cursor-1", true),
        )
        coEvery { repo.getMessages("cursor-1") } returns Result.success(
            MessagePage(listOf(olderMsg), null, false),
        )
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            val initial = vm.uiState.value as ChatUiState.Success
            assertTrue(initial.hasMore)

            vm.loadOlder()
            dispatcher.scheduler.runCurrent()

            val after = vm.uiState.value as ChatUiState.Success
            assertEquals(2, after.messages.size)
            assertEquals("older", after.messages[0].body)
            assertFalse(after.hasMore)
            assertNull(after.nextCursor)
            assertFalse(after.isLoadingOlder)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `load older failure preserves messages and exposes retry`() = runTest {
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages(null) } returns Result.success(
            MessagePage(listOf(fakeMessage), "cursor-1", true),
        )
        coEvery { repo.getMessages("cursor-1") } returns Result.failure(RuntimeException("offline"))
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            vm.loadOlder()
            dispatcher.scheduler.runCurrent()

            val state = vm.uiState.value as ChatUiState.Success
            assertEquals(1, state.messages.size)
            assertEquals("Hello", state.messages[0].body)
            assertEquals("Failed to load older messages. Tap to retry.", state.olderPageError)
            assertTrue(state.hasMore)
            assertEquals("cursor-1", state.nextCursor)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `concurrent older-page guard`() = runTest {
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages(null) } returns Result.success(
            MessagePage(listOf(fakeMessage), "cursor-1", true),
        )
        coEvery { repo.getMessages("cursor-1") } returns Result.success(
            MessagePage(emptyList(), null, false),
        )
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            vm.loadOlder()
            // Second call while first is in-flight should be no-op
            vm.loadOlder()
            dispatcher.scheduler.runCurrent()

            // Only one call should have been made; state is consistent
            val state = vm.uiState.value as ChatUiState.Success
            assertFalse(state.isLoadingOlder)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `fixed-size polling detects new messages by ID`() = runTest {
        val msg1 = fakeMessage.copy(id = 1, createdAt = "2026-10-24T10:00:00Z")
        val msg2 = fakeMessage.copy(id = 2, body = "new", createdAt = "2026-10-24T11:00:00Z")
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages() } returnsMany listOf(
            Result.success(MessagePage(listOf(msg1), null, false)),
            Result.success(MessagePage(listOf(msg1, msg2), null, false)),
        )
        val vm = vm()
        vm.setScreenVisible(true)

        try {
            vm.uiState.test {
                awaitItem() // Loading
                dispatcher.scheduler.runCurrent()
                val initial = awaitItem() as ChatUiState.Success
                assertEquals(1, initial.messages.size)

                dispatcher.scheduler.advanceTimeBy(5_001)
                dispatcher.scheduler.runCurrent()

                val polled = awaitItem() as ChatUiState.Success
                assertEquals(2, polled.messages.size)
                assertEquals("new", polled.messages[1].body)
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `dedupe across poll and send`() = runTest {
        val sentMsg = fakeMessage.copy(id = 2, body = "sent", createdAt = "2026-10-24T11:00:00Z")
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages() } returnsMany listOf(
            Result.success(MessagePage(listOf(fakeMessage), null, false)),
            // Poll returns the same sent message
            Result.success(MessagePage(listOf(fakeMessage, sentMsg), null, false)),
        )
        coEvery { repo.sendMessage("sent") } returns Result.success(sentMsg)
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            val initial = vm.uiState.value as ChatUiState.Success
            assertEquals(1, initial.messages.size)

            // Send message
            vm.sendMessage("sent")
            dispatcher.scheduler.runCurrent()

            // Poll happens
            dispatcher.scheduler.advanceTimeBy(5_001)
            dispatcher.scheduler.runCurrent()

            val state = vm.uiState.value as ChatUiState.Success
            // Should have exactly 2 messages, not 3 (no duplicate for id=2)
            assertEquals(2, state.messages.size)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `retained older history after poll`() = runTest {
        val olderMsg = fakeMessage.copy(id = 0, body = "older", createdAt = "2026-10-24T09:00:00Z")
        val page1Msg = fakeMessage.copy(id = 1, createdAt = "2026-10-24T10:00:00Z")
        val newMsg = fakeMessage.copy(id = 2, body = "new", createdAt = "2026-10-24T11:00:00Z")

        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        // Initial load returns older page
        coEvery { repo.getMessages(null) } returnsMany listOf(
            Result.success(MessagePage(listOf(olderMsg), "cursor-1", true)),
            // After poll (page 1 refresh)
            Result.success(MessagePage(listOf(page1Msg, newMsg), "cursor-1", true)),
        )
        coEvery { repo.getMessages("cursor-1") } returns Result.success(
            MessagePage(listOf(page1Msg), null, false),
        )
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            val initial = vm.uiState.value as ChatUiState.Success
            assertEquals(1, initial.messages.size) // older only

            // Load older page
            vm.loadOlder()
            dispatcher.scheduler.runCurrent()
            val withOlder = vm.uiState.value as ChatUiState.Success
            assertEquals(2, withOlder.messages.size)

            // Poll refreshes page 1, merges new message
            vm.setScreenVisible(true)
            dispatcher.scheduler.advanceTimeBy(5_001)
            dispatcher.scheduler.runCurrent()

            val afterPoll = vm.uiState.value as ChatUiState.Success
            // older (id=0) is retained, page1 (id=1) and new (id=2) are merged
            assertEquals(3, afterPoll.messages.size)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `repo error emits Error state`() = runTest {
        coEvery { repo.getConversation() } returns Result.failure(RuntimeException("offline"))
        val vm = vm()

        try {
            vm.uiState.test {
                awaitItem() // Loading
                dispatcher.scheduler.runCurrent()
                assertInstanceOf(ChatUiState.Error::class.java, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `conversation access level is preserved in state`() = runTest {
        val generalInquiryConversation = fakeConversation.copy(
            accessLevel = ConversationAccessLevel.GENERAL_INQUIRY,
            capabilities = ConversationCapabilities(canUploadAttachments = false),
        )
        coEvery { repo.getConversation() } returns Result.success(generalInquiryConversation)
        coEvery { repo.getMessages() } returns Result.success(MessagePage(emptyList(), null, false))
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            val state = vm.uiState.value as ChatUiState.Success
            assertEquals(ConversationAccessLevel.GENERAL_INQUIRY, state.conversation.accessLevel)
            assertEquals(false, state.conversation.capabilities.canUploadAttachments)
        } finally {
            vm.viewModelScope.cancel()
        }
    }
}
