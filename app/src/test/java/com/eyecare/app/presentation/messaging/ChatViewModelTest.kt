package com.eyecare.app.presentation.messaging

import app.cash.turbine.test
import com.eyecare.app.domain.model.Conversation
import com.eyecare.app.domain.model.Message
import com.eyecare.app.domain.model.User
import com.eyecare.app.domain.repository.AppointmentRepository
import com.eyecare.app.domain.repository.AuthRepository
import com.eyecare.app.domain.repository.OrderRepository
import com.eyecare.app.domain.repository.ChatRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: ChatRepository
    private lateinit var authRepo: AuthRepository
    private lateinit var appointmentRepo: AppointmentRepository
    private lateinit var orderRepo: OrderRepository

    private val fakeConversation = Conversation(1, null, null, "General", "2026-10-24T10:00:00Z")
    private val fakeMessage = Message(1, 1, 42, "Hello", null, "2026-10-24T10:00:00Z", emptyList())

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mockk()
        authRepo = mockk { coEvery { getUser() } returns Result.success(User(42, "Test", "t@t.com", "customer")) }
        appointmentRepo = mockk { coEvery { getAppointments() } returns Result.success(emptyList()) }
        orderRepo = mockk { coEvery { getOrders() } returns Result.success(emptyList()) }
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun vm() = ChatViewModel(repo, authRepo, appointmentRepo, orderRepo)

    @Test
    fun `initial state is Loading then loads conversation and messages`() = runTest {
        coEvery { repo.getOrCreateConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages(1) } returns Result.success(listOf(fakeMessage))
        val vm = vm()

        vm.uiState.test {
            assertInstanceOf(ChatUiState.Loading::class.java, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            val state = awaitItem() as ChatUiState.Success
            assertEquals(1, state.messages.size)
            assertEquals("Hello", state.messages[0].body)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `send message appends it to list`() = runTest {
        coEvery { repo.getOrCreateConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages(1) } returns Result.success(emptyList())
        coEvery { repo.sendMessage(1, "Hi there") } returns Result.success(
            fakeMessage.copy(id = 2, body = "Hi there")
        )
        val vm = vm()

        vm.uiState.test {
            awaitItem() // Loading
            dispatcher.scheduler.advanceUntilIdle()
            awaitItem() // Success with empty messages

            vm.sendMessage("Hi there")
            dispatcher.scheduler.advanceUntilIdle()

            val sending = awaitItem() as ChatUiState.Success
            if (sending.isSending) {
                // consume the isSending=true state, then get final
                dispatcher.scheduler.advanceUntilIdle()
                val state = awaitItem() as ChatUiState.Success
                assertEquals(1, state.messages.size)
                assertEquals("Hi there", state.messages[0].body)
            } else {
                assertEquals(1, sending.messages.size)
                assertEquals("Hi there", sending.messages[0].body)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `send empty message does nothing`() = runTest {
        coEvery { repo.getOrCreateConversation() } returns Result.success(fakeConversation)
        coEvery { repo.getMessages(1) } returns Result.success(emptyList())
        val vm = vm()
        dispatcher.scheduler.advanceUntilIdle()

        val stateBefore = vm.uiState.value
        vm.sendMessage("   ")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(stateBefore, vm.uiState.value) // unchanged
    }

    @Test
    fun `repo error emits Error state`() = runTest {
        coEvery { repo.getOrCreateConversation() } returns Result.failure(RuntimeException("offline"))
        val vm = vm()

        vm.uiState.test {
            awaitItem() // Loading
            dispatcher.scheduler.advanceUntilIdle()
            assertInstanceOf(ChatUiState.Error::class.java, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}

