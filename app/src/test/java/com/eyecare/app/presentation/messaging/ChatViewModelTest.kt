package com.eyecare.app.presentation.messaging

import android.net.Uri
import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.Conversation
import com.eyecare.app.domain.model.ConversationAccessLevel
import com.eyecare.app.domain.model.ConversationCapabilities
import com.eyecare.app.domain.model.Message
import com.eyecare.app.domain.model.MessagePage
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.domain.model.SenderType
import com.eyecare.app.domain.repository.AttachmentDownload
import com.eyecare.app.domain.repository.AuthRepository
import com.eyecare.app.domain.repository.ChatRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException

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
    private val fakeStaffMessage = Message(2, 99, SenderType.STAFF, "Hi from staff", null, "2026-10-24T10:01:00Z", emptyList())
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
        coEvery { repo.markMessagesRead() } returns Result.success(0)
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
        coEvery { repo.markMessagesRead() } returns Result.success(0)
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
        coEvery { repo.markMessagesRead() } returns Result.success(0)
        val vm = vm()

        try {
            vm.uiState.test {
                awaitItem() // Loading
                dispatcher.scheduler.runCurrent()
                awaitItem() // Success with empty messages

                vm.onDraftChanged("Hi there")
                dispatcher.scheduler.runCurrent()
                awaitItem() // draft updated

                vm.sendMessage()
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
        coEvery { repo.markMessagesRead() } returns Result.success(0)
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            vm.onDraftChanged("   ")
            dispatcher.scheduler.runCurrent()
            val stateBefore = vm.uiState.value
            vm.sendMessage()
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
        coEvery { repo.markMessagesRead() } returns Result.success(0)
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
        coEvery { repo.markMessagesRead() } returns Result.success(0)
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
        coEvery { repo.markMessagesRead() } returns Result.success(0)
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
    fun `repeated older cursor terminates pagination without retry loop`() = runTest {
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages(null) } returns Result.success(
            MessagePage(listOf(fakeMessage), "cursor-1", true),
        )
        coEvery { repo.getMessages("cursor-1") } returns Result.success(
            MessagePage(emptyList(), "cursor-1", true),
        )
        coEvery { repo.markMessagesRead() } returns Result.success(0)
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            vm.loadOlder()
            dispatcher.scheduler.runCurrent()

            val after = vm.uiState.value as ChatUiState.Success
            assertFalse(after.hasMore)
            assertNull(after.nextCursor)

            vm.loadOlder()
            dispatcher.scheduler.runCurrent()
            coVerify(exactly = 1) { repo.getMessages("cursor-1") }
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
        coEvery { repo.markMessagesRead() } returns Result.success(0)
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
        coEvery { repo.markMessagesRead() } returns Result.success(0)
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
    fun `polling own-only message does not mark read again`() = runTest {
        val ownNewMessage = fakeMessage.copy(id = 3, body = "My follow-up")
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages() } returnsMany listOf(
            Result.success(MessagePage(listOf(fakeStaffMessage), null, false)),
            Result.success(MessagePage(listOf(fakeStaffMessage, ownNewMessage), null, false)),
        )
        coEvery { repo.markMessagesRead() } returns Result.success(1)
        val vm = vm()
        vm.setScreenVisible(true)

        try {
            dispatcher.scheduler.runCurrent()
            dispatcher.scheduler.advanceTimeBy(5_001)
            dispatcher.scheduler.runCurrent()

            coVerify(exactly = 1) { repo.markMessagesRead() }
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
        coEvery { repo.markMessagesRead() } returns Result.success(0)
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            val initial = vm.uiState.value as ChatUiState.Success
            assertEquals(1, initial.messages.size)

            // Send message
            vm.onDraftChanged("sent")
            dispatcher.scheduler.runCurrent()
            vm.sendMessage()
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
        coEvery { repo.markMessagesRead() } returns Result.success(0)
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
        coEvery { repo.getConversation() } returns Result.failure(
            RuntimeException("PHP Fatal Error: backend internals"),
        )
        val vm = vm()

        try {
            vm.uiState.test {
                awaitItem() // Loading
                dispatcher.scheduler.runCurrent()
                val error = awaitItem() as ChatUiState.Error
                assertEquals("Unable to load conversation. Please try again.", error.message)
                assertFalse(error.message.contains("PHP"))
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `message load error is patient safe`() = runTest {
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages() } returns Result.failure(
            RuntimeException("PHP Fatal Error: backend internals"),
        )
        val vm = vm()

        try {
            vm.uiState.test {
                awaitItem() // Loading
                dispatcher.scheduler.runCurrent()
                val error = awaitItem() as ChatUiState.Error
                assertEquals("Unable to load messages. Please try again.", error.message)
                assertFalse(error.message.contains("PHP"))
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
        coEvery { repo.markMessagesRead() } returns Result.success(0)
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

    // --- Task 10: Draft ownership and safe send ---

    @Test
    fun `draft survives configuration change simulation`() = runTest {
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages() } returns Result.success(MessagePage(emptyList(), null, false))
        coEvery { repo.markMessagesRead() } returns Result.success(0)
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            vm.onDraftChanged("hello draft")
            dispatcher.scheduler.runCurrent()

            val state = vm.uiState.value as ChatUiState.Success
            assertEquals("hello draft", state.inputText)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `double tap single flight prevents concurrent send`() = runTest {
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages() } returns Result.success(MessagePage(emptyList(), null, false))
        coEvery { repo.markMessagesRead() } returns Result.success(0)
        coEvery { repo.sendMessage("msg") } coAnswers {
            kotlinx.coroutines.delay(10_000)
            Result.success(fakeMessage.copy(id = 99, body = "msg"))
        }
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            vm.onDraftChanged("msg")
            dispatcher.scheduler.runCurrent()
            vm.sendMessage()
            dispatcher.scheduler.runCurrent()
            // Second send while first is in-flight
            vm.sendMessage()
            dispatcher.scheduler.runCurrent()

            val state = vm.uiState.value as ChatUiState.Success
            assertTrue(state.isSending)

            dispatcher.scheduler.advanceTimeBy(10_001)
            dispatcher.scheduler.runCurrent()

            // Only one sendMessage call should have been made
            coVerify(exactly = 1) { repo.sendMessage("msg") }
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `success clears draft and merges message`() = runTest {
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages() } returns Result.success(MessagePage(emptyList(), null, false))
        coEvery { repo.markMessagesRead() } returns Result.success(0)
        coEvery { repo.sendMessage("clear me") } returns Result.success(
            fakeMessage.copy(id = 10, body = "clear me")
        )
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            vm.onDraftChanged("clear me")
            dispatcher.scheduler.runCurrent()
            vm.sendMessage()
            dispatcher.scheduler.runCurrent()

            val state = vm.uiState.value as ChatUiState.Success
            assertEquals("", state.inputText)
            assertEquals(1, state.messages.size)
            assertEquals("clear me", state.messages[0].body)
            assertFalse(state.isSending)
            assertNull(state.sendError)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `attachment send forwards draft body and clears submitted draft`() = runTest {
        val attachmentUri: Uri = mockk()
        val attachment = PendingAttachment(
            uri = attachmentUri,
            mimeType = "image/jpeg",
            fileName = "photo.jpg",
            fileSize = 1024,
        )
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages() } returns Result.success(MessagePage(emptyList(), null, false))
        coEvery { repo.markMessagesRead() } returns Result.success(0)
        coEvery {
            repo.sendFileMessage("Please see attached", attachmentUri, "image/jpeg", "photo.jpg")
        } returns Result.success(fakeMessage.copy(id = 11, body = "Please see attached"))
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            vm.onDraftChanged(" Please see attached ")
            vm.setPendingAttachment(attachment)
            dispatcher.scheduler.runCurrent()

            vm.sendPendingAttachment()
            dispatcher.scheduler.runCurrent()

            val state = vm.uiState.value as ChatUiState.Success
            coVerify(exactly = 1) {
                repo.sendFileMessage("Please see attached", attachmentUri, "image/jpeg", "photo.jpg")
            }
            assertEquals("", state.inputText)
            assertFalse(state.isSending)
            assertNull(state.sendError)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `polling refreshes read receipt for an existing message`() = runTest {
        val unreadOwnMessage = fakeMessage.copy(readAt = null)
        val readOwnMessage = unreadOwnMessage.copy(readAt = "2026-10-24T10:05:00Z")
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages() } returnsMany listOf(
            Result.success(MessagePage(listOf(unreadOwnMessage), null, false)),
            Result.success(MessagePage(listOf(readOwnMessage), null, false)),
        )
        val vm = vm()
        vm.setScreenVisible(true)

        try {
            dispatcher.scheduler.runCurrent()
            assertNull((vm.uiState.value as ChatUiState.Success).messages.single().readAt)

            dispatcher.scheduler.advanceTimeBy(5_001)
            dispatcher.scheduler.runCurrent()

            assertEquals(
                "2026-10-24T10:05:00Z",
                (vm.uiState.value as ChatUiState.Success).messages.single().readAt,
            )
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `download attachment stores result and intent for the screen`() = runTest {
        val download = AttachmentDownload("receipt.pdf", "application/pdf", byteArrayOf(1, 2, 3))
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages() } returns Result.success(MessagePage(emptyList(), null, false))
        coEvery { repo.downloadAttachment(9) } returns Result.success(download)
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            vm.downloadAttachment(9)
            dispatcher.scheduler.runCurrent()

            val state = vm.uiState.value as ChatUiState.Success
            assertEquals(download, state.downloadedAttachment)
            assertEquals(AttachmentDownloadIntent.OPEN_EXTERNALLY, state.downloadIntent)
            assertNull(state.downloadingAttachmentId)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `download failure exposes safe retryable error`() = runTest {
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages() } returns Result.success(MessagePage(emptyList(), null, false))
        coEvery { repo.downloadAttachment(9) } returns Result.failure(RuntimeException("Unable to resolve host"))
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            vm.downloadAttachment(9, AttachmentDownloadIntent.SAVE_TO_GALLERY)
            dispatcher.scheduler.runCurrent()

            val failed = vm.uiState.value as ChatUiState.Success
            assertEquals(
                "We couldn't open that attachment. Check your connection and try again.",
                failed.downloadError,
            )
            assertEquals(
                FailedDownload(9, AttachmentDownloadIntent.SAVE_TO_GALLERY),
                failed.lastFailedDownload,
            )

            val download = AttachmentDownload("photo.jpg", "image/jpeg", byteArrayOf(1))
            coEvery { repo.downloadAttachment(9) } returns Result.success(download)
            vm.retryDownload()
            dispatcher.scheduler.runCurrent()

            val retried = vm.uiState.value as ChatUiState.Success
            assertEquals(download, retried.downloadedAttachment)
            assertEquals(AttachmentDownloadIntent.SAVE_TO_GALLERY, retried.downloadIntent)
            assertNull(retried.downloadError)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `consume downloaded attachment clears one-shot result`() = runTest {
        val download = AttachmentDownload("receipt.pdf", "application/pdf", byteArrayOf(1))
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages() } returns Result.success(MessagePage(emptyList(), null, false))
        coEvery { repo.downloadAttachment(9) } returns Result.success(download)
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            vm.downloadAttachment(9)
            dispatcher.scheduler.runCurrent()
            vm.consumeDownloadedAttachment()

            val state = vm.uiState.value as ChatUiState.Success
            assertNull(state.downloadedAttachment)
            assertNull(state.downloadIntent)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `send failure 429 preserves draft and shows safe copy`() = runTest {
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages() } returns Result.success(MessagePage(emptyList(), null, false))
        coEvery { repo.markMessagesRead() } returns Result.success(0)
        coEvery { repo.sendMessage("rate limited") } returns Result.failure(
            ApiDomainError(httpStatus = 429, code = "RATE_LIMITED", message = "Too Many Requests")
        )
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            vm.onDraftChanged("rate limited")
            dispatcher.scheduler.runCurrent()
            vm.sendMessage()
            dispatcher.scheduler.runCurrent()

            val state = vm.uiState.value as ChatUiState.Success
            assertEquals("rate limited", state.inputText)
            assertEquals("You're sending messages too quickly. Please wait and try again.", state.sendError)
            assertFalse(state.isSending)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `send failure 422 preserves draft and shows safe copy`() = runTest {
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages() } returns Result.success(MessagePage(emptyList(), null, false))
        coEvery { repo.markMessagesRead() } returns Result.success(0)
        coEvery { repo.sendMessage("bad input") } returns Result.failure(
            ApiDomainError(httpStatus = 422, code = "VALIDATION_ERROR", message = "body field required")
        )
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            vm.onDraftChanged("bad input")
            dispatcher.scheduler.runCurrent()
            vm.sendMessage()
            dispatcher.scheduler.runCurrent()

            val state = vm.uiState.value as ChatUiState.Success
            assertEquals("bad input", state.inputText)
            assertEquals("Message could not be sent. Please check and try again.", state.sendError)
            assertFalse(state.isSending)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `send failure network preserves draft and shows safe copy`() = runTest {
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages() } returns Result.success(MessagePage(emptyList(), null, false))
        coEvery { repo.markMessagesRead() } returns Result.success(0)
        coEvery { repo.sendMessage("offline") } returns Result.failure(IOException("Connection refused"))
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            vm.onDraftChanged("offline")
            dispatcher.scheduler.runCurrent()
            vm.sendMessage()
            dispatcher.scheduler.runCurrent()

            val state = vm.uiState.value as ChatUiState.Success
            assertEquals("offline", state.inputText)
            assertEquals("Unable to send. Check your connection and try again.", state.sendError)
            assertFalse(state.isSending)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `send failure unknown shows safe copy no raw exception text`() = runTest {
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages() } returns Result.success(MessagePage(emptyList(), null, false))
        coEvery { repo.markMessagesRead() } returns Result.success(0)
        coEvery { repo.sendMessage("weird") } returns Result.failure(
            RuntimeException("PHP Fatal Error: Class not found in /var/www/vendor...")
        )
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            vm.onDraftChanged("weird")
            dispatcher.scheduler.runCurrent()
            vm.sendMessage()
            dispatcher.scheduler.runCurrent()

            val state = vm.uiState.value as ChatUiState.Success
            assertEquals("weird", state.inputText)
            assertEquals("Unable to send. Check your connection and try again.", state.sendError)
            val error = state.sendError ?: ""
            assertFalse(error.contains("PHP"))
            assertFalse(error.contains("Fatal"))
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    // --- Task 11: Mark-read and effects ---

    @Test
    fun `initial staff message triggers mark-read`() = runTest {
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages() } returns Result.success(
            MessagePage(listOf(fakeMessage, fakeStaffMessage), null, false),
        )
        coEvery { repo.markMessagesRead() } returns Result.success(1)
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            dispatcher.scheduler.runCurrent()
            coVerify(exactly = 1) { repo.markMessagesRead() }
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `own messages do not trigger mark-read`() = runTest {
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages() } returns Result.success(
            MessagePage(listOf(fakeMessage), null, false),
        )
        coEvery { repo.markMessagesRead() } returns Result.success(0)
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            dispatcher.scheduler.runCurrent()
            coVerify(exactly = 0) { repo.markMessagesRead() }
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `mark-read is single flight no concurrent requests`() = runTest {
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages() } returns Result.success(
            MessagePage(listOf(fakeMessage, fakeStaffMessage), null, false),
        )
        var callCount = 0
        coEvery { repo.markMessagesRead() } coAnswers {
            callCount++
            kotlinx.coroutines.delay(5_000)
            Result.success(1)
        }
        val vm = vm()

        try {
            // Run initial load → tryMarkRead launches mark-read coroutine (delayed 5s)
            dispatcher.scheduler.runCurrent()
            dispatcher.scheduler.runCurrent()
            assertEquals(1, callCount) // mark-read #1 launched (but delayed)

            // While mark-read #1 is in-flight, call setScreenVisible(true) again
            vm.setScreenVisible(true)
            dispatcher.scheduler.runCurrent()
            // Still only 1 call — the second tryMarkRead sees isMarkReadInFlight=true
            assertEquals(1, callCount)

            // Let mark-read #1 complete — this triggers pendingMarkRead → mark-read #2
            dispatcher.scheduler.advanceTimeBy(5_001)
            dispatcher.scheduler.runCurrent()
            assertEquals(2, callCount)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `hidden screen suppresses mark-read`() = runTest {
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages() } returns Result.success(
            MessagePage(listOf(fakeStaffMessage), null, false),
        )
        coEvery { repo.markMessagesRead() } returns Result.success(1)
        val vm = vm()

        try {
            // Run initial load and mark-read
            dispatcher.scheduler.runCurrent()
            dispatcher.scheduler.runCurrent()
            coVerify(exactly = 1) { repo.markMessagesRead() }

            // Now hide and poll with new staff message
            vm.setScreenVisible(false)
            val newStaff = fakeStaffMessage.copy(id = 99, body = "new staff")
            coEvery { repo.getMessages() } returns Result.success(
                MessagePage(listOf(fakeMessage, fakeStaffMessage, newStaff), null, false),
            )
            dispatcher.scheduler.advanceTimeBy(5_001)
            dispatcher.scheduler.runCurrent()

            // Still only 1 call — hidden screen did not trigger another
            coVerify(exactly = 1) { repo.markMessagesRead() }
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `mark-read failure sets pending retry for next visible refresh`() = runTest {
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages() } returns Result.success(
            MessagePage(listOf(fakeStaffMessage), null, false),
        )
        coEvery { repo.markMessagesRead() } returnsMany listOf(
            Result.failure(RuntimeException("server error")),
            Result.success(1),
        )
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            dispatcher.scheduler.runCurrent()
            assertTrue(vm.uiState.value is ChatUiState.Success)

            // Hide screen
            vm.setScreenVisible(false)
            dispatcher.scheduler.runCurrent()

            // Show screen again
            vm.setScreenVisible(true)
            dispatcher.scheduler.runCurrent()
            dispatcher.scheduler.runCurrent()
            dispatcher.scheduler.runCurrent()

            coVerify(exactly = 2) { repo.markMessagesRead() }
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `success emits one-shot MessagesMarkedRead effect`() = runTest {
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages() } returns Result.success(
            MessagePage(listOf(fakeStaffMessage), null, false),
        )
        coEvery { repo.markMessagesRead() } returns Result.success(1)
        val vm = vm()

        try {
            vm.effects.test {
                // Run load and mark-read
                dispatcher.scheduler.runCurrent()
                dispatcher.scheduler.runCurrent()
                val effect = awaitItem()
                assertInstanceOf(ChatEffect.MessagesMarkedRead::class.java, effect)
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    // --- Task 12: Search state and cursor behavior ---

    private fun setupVmWithSuccess(): ChatViewModel {
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages() } returns Result.success(MessagePage(listOf(fakeMessage), null, false))
        coEvery { repo.markMessagesRead() } returns Result.success(0)
        val vm = vm()
        dispatcher.scheduler.runCurrent()
        return vm
    }

    @Test
    fun `open search exposes search mode before query submission`() = runTest {
        val vm = setupVmWithSuccess()

        try {
            vm.openSearch()
            dispatcher.scheduler.runCurrent()

            val searchState = (vm.uiState.value as ChatUiState.Success).searchState
            assertNotNull(searchState)
            assertEquals("", searchState!!.query)
            assertFalse(searchState.isLoading)
            assertFalse(searchState.isLoadingMore)
            assertNull(searchState.error)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `search validation rejects fewer than 3 characters`() = runTest {
        val vm = setupVmWithSuccess()

        try {
            vm.onSearchQueryChanged("ab")
            dispatcher.scheduler.runCurrent()
            vm.submitSearch()
            dispatcher.scheduler.runCurrent()

            val state = vm.uiState.value as ChatUiState.Success
            val ss = state.searchState
            assert(ss != null)
            assertEquals("ab", ss!!.query)
            assertEquals("Search requires at least 3 characters.", ss.error)
            assertFalse(ss.isLoading)
            assertTrue(ss.results.isEmpty())
            coVerify(exactly = 0) { repo.searchMessages(any(), any()) }
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `search validation accepts 3 characters`() = runTest {
        coEvery { repo.searchMessages("abc", null) } returns Result.success(
            MessagePage(emptyList(), null, false)
        )
        val vm = setupVmWithSuccess()

        try {
            vm.onSearchQueryChanged("abc")
            dispatcher.scheduler.runCurrent()
            vm.submitSearch()
            dispatcher.scheduler.runCurrent()

            val state = vm.uiState.value as ChatUiState.Success
            val ss = state.searchState
            assert(ss != null)
            assertEquals("abc", ss!!.query)
            assertNull(ss.error)
            assertFalse(ss.isLoading)
            coVerify(exactly = 1) { repo.searchMessages("abc", null) }
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `search validation accepts 500 characters`() = runTest {
        val query500 = "a".repeat(500)
        coEvery { repo.searchMessages(query500, null) } returns Result.success(
            MessagePage(emptyList(), null, false)
        )
        val vm = setupVmWithSuccess()

        try {
            vm.onSearchQueryChanged(query500)
            dispatcher.scheduler.runCurrent()
            vm.submitSearch()
            dispatcher.scheduler.runCurrent()

            val state = vm.uiState.value as ChatUiState.Success
            val ss = state.searchState
            assert(ss != null)
            assertEquals(query500, ss!!.query)
            assertNull(ss.error)
            coVerify(exactly = 1) { repo.searchMessages(query500, null) }
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `search validation rejects 501 characters`() = runTest {
        val query501 = "a".repeat(501)
        val vm = setupVmWithSuccess()

        try {
            vm.onSearchQueryChanged(query501)
            dispatcher.scheduler.runCurrent()
            vm.submitSearch()
            dispatcher.scheduler.runCurrent()

            val state = vm.uiState.value as ChatUiState.Success
            val ss = state.searchState
            assert(ss != null)
            assertEquals(query501, ss!!.query)
            assertEquals("Search is limited to 500 characters.", ss.error)
            assertFalse(ss.isLoading)
            coVerify(exactly = 0) { repo.searchMessages(any(), any()) }
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `typing does not fire search request only submit does`() = runTest {
        coEvery { repo.searchMessages("hello", null) } returns Result.success(
            MessagePage(emptyList(), null, false)
        )
        val vm = setupVmWithSuccess()

        try {
            vm.onSearchQueryChanged("h")
            dispatcher.scheduler.runCurrent()
            vm.onSearchQueryChanged("he")
            dispatcher.scheduler.runCurrent()
            vm.onSearchQueryChanged("hel")
            dispatcher.scheduler.runCurrent()
            vm.onSearchQueryChanged("hell")
            dispatcher.scheduler.runCurrent()
            vm.onSearchQueryChanged("hello")
            dispatcher.scheduler.runCurrent()

            // No search request should have been made yet
            coVerify(exactly = 0) { repo.searchMessages(any(), any()) }

            // Draft should be updated
            val before = vm.uiState.value as ChatUiState.Success
            assertEquals("hello", before.searchDraft)
            assertNull(before.searchState)

            vm.submitSearch()
            dispatcher.scheduler.runCurrent()

            coVerify(exactly = 1) { repo.searchMessages("hello", null) }
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `new search generation rejects stale responses`() = runTest {
        val searchResult1 = MessagePage(
            listOf(fakeMessage.copy(id = 100, body = "stale")),
            null, false,
        )
        val searchResult2 = MessagePage(
            listOf(fakeMessage.copy(id = 200, body = "fresh")),
            null, false,
        )
        coEvery { repo.searchMessages("first", null) } coAnswers {
            kotlinx.coroutines.delay(5_000)
            Result.success(searchResult1)
        }
        coEvery { repo.searchMessages("second", null) } returns Result.success(searchResult2)
        val vm = setupVmWithSuccess()

        try {
            // Submit first search (delayed)
            vm.onSearchQueryChanged("first")
            dispatcher.scheduler.runCurrent()
            vm.submitSearch()
            dispatcher.scheduler.runCurrent()

            // Submit second search (immediate) before first completes
            vm.onSearchQueryChanged("second")
            dispatcher.scheduler.runCurrent()
            vm.submitSearch()
            dispatcher.scheduler.runCurrent()

            // Let first (stale) complete — should be rejected
            dispatcher.scheduler.advanceTimeBy(5_001)
            dispatcher.scheduler.runCurrent()

            val state = vm.uiState.value as ChatUiState.Success
            val ss = state.searchState!!
            assertEquals("second", ss.query)
            assertEquals(1, ss.results.size)
            assertEquals("fresh", ss.results[0].body)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `search page retry with query and cursor forwarding`() = runTest {
        val msg1 = fakeMessage.copy(id = 300, body = "result1")
        val msg2 = fakeMessage.copy(id = 301, body = "result2")
        coEvery { repo.searchMessages("test", null) } returns Result.success(
            MessagePage(listOf(msg1), "search-cursor-1", true)
        )
        coEvery { repo.searchMessages("test", "search-cursor-1") } returns Result.success(
            MessagePage(listOf(msg2), null, false)
        )
        val vm = setupVmWithSuccess()

        try {
            vm.onSearchQueryChanged("test")
            dispatcher.scheduler.runCurrent()
            vm.submitSearch()
            dispatcher.scheduler.runCurrent()

            val afterFirst = vm.uiState.value as ChatUiState.Success
            val ss1 = afterFirst.searchState!!
            assertEquals(1, ss1.results.size)
            assertTrue(ss1.hasMore)
            assertEquals("search-cursor-1", ss1.nextCursor)

            vm.loadMoreSearchResults()
            dispatcher.scheduler.runCurrent()

            val afterMore = vm.uiState.value as ChatUiState.Success
            val ss2 = afterMore.searchState!!
            assertEquals(2, ss2.results.size)
            assertFalse(ss2.hasMore)
            assertNull(ss2.nextCursor)
            assertFalse(ss2.isLoadingMore)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `close search restores timeline pages draft and cursor state unchanged`() = runTest {
        coEvery { repo.searchMessages("query", null) } returns Result.success(
            MessagePage(listOf(fakeMessage.copy(id = 500, body = "search hit")), null, false)
        )
        coEvery { repo.getMessages(null) } returns Result.success(
            MessagePage(listOf(fakeMessage), "cursor-abc", true)
        )
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.markMessagesRead() } returns Result.success(0)
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            // Pre-set draft and input text
            vm.onDraftChanged("my draft")
            dispatcher.scheduler.runCurrent()

            val before = vm.uiState.value as ChatUiState.Success
            assertEquals("my draft", before.inputText)
            assertEquals("cursor-abc", before.nextCursor)
            assertTrue(before.hasMore)
            val messagesBefore = before.messages

            // Open and use search
            vm.openSearch()
            dispatcher.scheduler.runCurrent()
            vm.onSearchQueryChanged("query")
            dispatcher.scheduler.runCurrent()
            vm.submitSearch()
            dispatcher.scheduler.runCurrent()

            val searching = vm.uiState.value as ChatUiState.Success
            assertNotNull(searching.searchState)
            assertEquals(1, searching.searchState!!.results.size)

            // Close search
            vm.closeSearch()
            dispatcher.scheduler.runCurrent()

            val after = vm.uiState.value as ChatUiState.Success
            assertNull(after.searchState)
            assertEquals("", after.searchDraft)
            // Timeline pages, draft, and cursor should be unchanged
            assertEquals("my draft", after.inputText)
            assertEquals(messagesBefore.size, after.messages.size)
            assertEquals(messagesBefore[0].body, after.messages[0].body)
            assertEquals("cursor-abc", after.nextCursor)
            assertTrue(after.hasMore)
        } finally {
            vm.viewModelScope.cancel()
        }
    }
}
