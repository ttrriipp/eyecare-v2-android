package com.eyecare.app.presentation.messaging

import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import com.eyecare.app.domain.model.Conversation
import com.eyecare.app.domain.model.ConversationAccessLevel
import com.eyecare.app.domain.model.ConversationCapabilities
import com.eyecare.app.domain.model.Message
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
import org.junit.jupiter.api.Assertions.assertInstanceOf
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
        coEvery { repo.getMessages() } returns Result.success(listOf(fakeMessage))
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
    fun `send message appends it to list`() = runTest {
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages() } returns Result.success(emptyList())
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
        coEvery { repo.getMessages() } returns Result.success(emptyList())
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
    fun `polling refreshes messages after interval`() = runTest {
        val newMessage = fakeMessage.copy(id = 2, body = "New reply from clinic")
        coEvery { repo.getConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages() } returnsMany listOf(
            Result.success(listOf(fakeMessage)),
            Result.success(listOf(fakeMessage, newMessage)),
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
                assertEquals("New reply from clinic", polled.messages[1].body)
                cancelAndIgnoreRemainingEvents()
            }
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
        coEvery { repo.getMessages() } returns Result.success(emptyList())
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
