package com.eyecare.app.presentation.messaging

import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import com.eyecare.app.domain.model.Conversation
import com.eyecare.app.domain.model.Message
import com.eyecare.app.domain.model.User
import com.eyecare.app.domain.repository.AppointmentV1Repository
import com.eyecare.app.domain.repository.AuthRepository
import com.eyecare.app.domain.repository.OrderRepository
import com.eyecare.app.domain.repository.ChatRepository
import com.eyecare.app.domain.repository.PaginatedResult
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
    private lateinit var appointmentRepo: AppointmentV1Repository
    private lateinit var orderRepo: OrderRepository

    private val fakeConversation = Conversation(1, null, 0, "2026-10-24T10:00:00Z")
    private val fakeMessage = Message(1, 1, 42, "Hello", null, "2026-10-24T10:00:00Z", emptyList())

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mockk()
        authRepo = mockk { coEvery { getMe() } returns Result.success(User(42, "Test", "t@t.com", null, "customer", null, null, null, null, null, null, null)) }
        appointmentRepo = mockk { coEvery { getAppointments(any()) } returns Result.success(PaginatedResult(emptyList(), 1, 1, 0)) }
        orderRepo = mockk { coEvery { getOrders() } returns Result.success(emptyList()) }
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun vm() = ChatViewModel(repo, authRepo, appointmentRepo, orderRepo)

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
}
