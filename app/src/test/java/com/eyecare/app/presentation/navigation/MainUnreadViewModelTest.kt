package com.eyecare.app.presentation.navigation

import com.eyecare.app.domain.model.Conversation
import com.eyecare.app.domain.model.ConversationAccessLevel
import com.eyecare.app.domain.model.ConversationCapabilities
import com.eyecare.app.domain.repository.ChatRepository
import com.eyecare.app.domain.repository.NotificationRepository
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainUnreadViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var chatRepository: ChatRepository
    private lateinit var notificationRepository: NotificationRepository

    private fun conversation(unreadCount: Int = 0) = Conversation(
        id = 1,
        patientId = 1,
        unreadCount = unreadCount,
        createdAt = "2026-01-01T00:00:00Z",
        accessLevel = ConversationAccessLevel.LINKED_PATIENT,
        capabilities = ConversationCapabilities.SAFE_DEFAULT,
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        chatRepository = mockk()
        notificationRepository = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = MainUnreadViewModel(chatRepository, notificationRepository)

    @Test
    fun `refresh loads differing counts`() = runTest {
        coEvery { chatRepository.getConversation() } returns Result.success(conversation(unreadCount = 3))
        coEvery { notificationRepository.getUnreadCount() } returns Result.success(5)

        val vm = viewModel()
        vm.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.state.value
        assertEquals(3, state.messageUnreadCount)
        assertEquals(5, state.notificationUnreadCount)
    }

    @Test
    fun `partial refresh failure preserves other count`() = runTest {
        coEvery { chatRepository.getConversation() } returns Result.failure(RuntimeException("network"))
        coEvery { notificationRepository.getUnreadCount() } returns Result.success(5)

        val vm = viewModel()
        vm.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, vm.state.value.messageUnreadCount)
        assertEquals(5, vm.state.value.notificationUnreadCount)
    }

    @Test
    fun `onMessagesMarkedRead zeros message count`() = runTest {
        coEvery { chatRepository.getConversation() } returns Result.success(conversation(unreadCount = 3))
        coEvery { notificationRepository.getUnreadCount() } returns Result.success(5)

        val vm = viewModel()
        vm.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        // After refresh, reconcile so message count becomes 0
        coEvery { chatRepository.getConversation() } returns Result.success(conversation(unreadCount = 0))
        vm.onMessagesMarkedRead()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, vm.state.value.messageUnreadCount)
        assertEquals(5, vm.state.value.notificationUnreadCount)
    }

    @Test
    fun `onNotificationRead decrements notification count`() = runTest {
        coEvery { chatRepository.getConversation() } returns Result.success(conversation(unreadCount = 0))
        coEvery { notificationRepository.getUnreadCount() } returns Result.success(5)

        val vm = viewModel()
        vm.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        // After decrement, reconcile with server
        coEvery { notificationRepository.getUnreadCount() } returns Result.success(4)
        vm.onNotificationRead()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(4, vm.state.value.notificationUnreadCount)
    }

    @Test
    fun `onNotificationRead clamps at zero`() = runTest {
        coEvery { chatRepository.getConversation() } returns Result.success(conversation(unreadCount = 0))
        coEvery { notificationRepository.getUnreadCount() } returns Result.success(0)

        val vm = viewModel()
        vm.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onNotificationRead()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, vm.state.value.notificationUnreadCount)
    }

    @Test
    fun `reconciliation after local update restores server count`() = runTest {
        coEvery { chatRepository.getConversation() } returns Result.success(conversation(unreadCount = 3))
        coEvery { notificationRepository.getUnreadCount() } returns Result.success(5)

        val vm = viewModel()
        vm.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        // Local zero, but server still has 2
        coEvery { chatRepository.getConversation() } returns Result.success(conversation(unreadCount = 2))
        vm.onMessagesMarkedRead()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, vm.state.value.messageUnreadCount)
    }

    @Test
    fun `no cross-counter overwrite`() = runTest {
        coEvery { chatRepository.getConversation() } returns Result.success(conversation(unreadCount = 3))
        coEvery { notificationRepository.getUnreadCount() } returns Result.success(5)

        val vm = viewModel()
        vm.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        // Zero message count should not affect notification count
        coEvery { chatRepository.getConversation() } returns Result.success(conversation(unreadCount = 0))
        vm.onMessagesMarkedRead()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, vm.state.value.messageUnreadCount)
        assertEquals(5, vm.state.value.notificationUnreadCount)
    }
}
