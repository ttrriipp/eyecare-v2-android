package com.eyecare.app.presentation.notifications

import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import com.eyecare.app.domain.model.AppNotification
import com.eyecare.app.domain.model.MobileDestination
import com.eyecare.app.domain.model.NotificationKind
import com.eyecare.app.domain.model.NotificationPage
import com.eyecare.app.domain.repository.NotificationRepository
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: NotificationRepository

    private fun notification(
        id: String = "uuid-1",
        kind: NotificationKind = NotificationKind.NEW_MESSAGE,
        title: String = "New Message",
        body: String = "Dr. Santos sent a message.",
        mobileAction: MobileDestination? = MobileDestination.CONVERSATION,
        readAt: String? = null,
        createdAt: String = "2026-08-15T10:00:00+08:00",
    ) = AppNotification(
        id = id,
        kind = kind,
        title = title,
        body = body,
        mobileAction = mobileAction,
        readAt = readAt,
        createdAt = createdAt,
    )

    private fun page(
        notifications: List<AppNotification>,
        currentPage: Int = 1,
        lastPage: Int = 1,
        total: Int = notifications.size,
    ) = NotificationPage(
        notifications = notifications,
        currentPage = currentPage,
        lastPage = lastPage,
        total = total,
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mockk()
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun vm() = NotificationListViewModel(repo)

    // --- Paging ---

    @Test
    fun `page 1 loading produces Success state`() = runTest {
        val n1 = notification(id = "uuid-1")
        coEvery { repo.getNotifications(page = 1) } returns Result.success(page(listOf(n1)))
        val vm = vm()

        try {
            vm.uiState.test {
                assertInstanceOf(NotificationListUiState.Loading::class.java, awaitItem())
                dispatcher.scheduler.runCurrent()
                val success = awaitItem() as NotificationListUiState.Success
                assertEquals(1, success.notifications.size)
                assertEquals("uuid-1", success.notifications[0].id)
                assertFalse(success.isLoadingMore)
                assertFalse(success.canLoadMore)
                assertNull(success.inlineError)
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `later page appends notifications`() = runTest {
        val n1 = notification(id = "uuid-1")
        val n2 = notification(id = "uuid-2")
        coEvery { repo.getNotifications(page = 1) } returns Result.success(
            page(listOf(n1), currentPage = 1, lastPage = 2),
        )
        coEvery { repo.getNotifications(page = 2) } returns Result.success(
            page(listOf(n2), currentPage = 2, lastPage = 2),
        )
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            vm.loadMore()
            dispatcher.scheduler.runCurrent()

            val state = vm.uiState.value as NotificationListUiState.Success
            assertEquals(2, state.notifications.size)
            assertEquals("uuid-1", state.notifications[0].id)
            assertEquals("uuid-2", state.notifications[1].id)
            assertFalse(state.canLoadMore)
            assertFalse(state.isLoadingMore)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `refresh replaces page 1`() = runTest {
        val n1 = notification(id = "uuid-1")
        val n2 = notification(id = "uuid-2")
        coEvery { repo.getNotifications(page = 1) } returnsMany listOf(
            Result.success(page(listOf(n1))),
            Result.success(page(listOf(n1, n2))),
        )
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            val initial = vm.uiState.value as NotificationListUiState.Success
            assertEquals(1, initial.notifications.size)

            vm.refresh()
            dispatcher.scheduler.runCurrent()

            val refreshed = vm.uiState.value as NotificationListUiState.Success
            assertEquals(2, refreshed.notifications.size)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    // --- UUID deduplication ---

    @Test
    fun `duplicate UUIDs across pages are deduped`() = runTest {
        val n1 = notification(id = "uuid-1")
        val n1Dup = notification(id = "uuid-1", body = "updated body")
        val n2 = notification(id = "uuid-2")
        coEvery { repo.getNotifications(page = 1) } returns Result.success(
            page(listOf(n1), currentPage = 1, lastPage = 2),
        )
        coEvery { repo.getNotifications(page = 2) } returns Result.success(
            page(listOf(n1Dup, n2), currentPage = 2, lastPage = 2),
        )
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            vm.loadMore()
            dispatcher.scheduler.runCurrent()

            val state = vm.uiState.value as NotificationListUiState.Success
            assertEquals(2, state.notifications.size)
            assertEquals("uuid-1", state.notifications[0].id)
            assertEquals("uuid-2", state.notifications[1].id)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    // --- Per-UUID double-tap prevention ---

    @Test
    fun `double tap on same UUID is prevented by single-flight`() = runTest {
        val n1 = notification(id = "uuid-1", readAt = null)
        coEvery { repo.getNotifications(page = 1) } returns Result.success(page(listOf(n1)))
        coEvery { repo.markOneRead("uuid-1") } coAnswers {
            kotlinx.coroutines.delay(5_000)
            Result.success(Unit)
        }
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            vm.markOneRead(n1)
            dispatcher.scheduler.runCurrent()
            vm.markOneRead(n1)
            dispatcher.scheduler.runCurrent()

            coVerify(exactly = 1) { repo.markOneRead("uuid-1") }
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    // --- Optimistic mark-one success/failure ---

    @Test
    fun `mark-one success removes UUID from mutationInFlight`() = runTest {
        val n1 = notification(id = "uuid-1", readAt = null)
        coEvery { repo.getNotifications(page = 1) } returns Result.success(page(listOf(n1)))
        coEvery { repo.markOneRead("uuid-1") } returns Result.success(Unit)
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            vm.markOneRead(n1)
            dispatcher.scheduler.runCurrent()

            val state = vm.uiState.value as NotificationListUiState.Success
            assertTrue(state.mutationInFlight.isEmpty())
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `mark-one failure reconciles readAt back to original`() = runTest {
        val n1 = notification(id = "uuid-1", readAt = null)
        coEvery { repo.getNotifications(page = 1) } returns Result.success(page(listOf(n1)))
        coEvery { repo.markOneRead("uuid-1") } returns Result.failure(RuntimeException("server error"))
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            vm.markOneRead(n1)
            dispatcher.scheduler.runCurrent()

            val state = vm.uiState.value as NotificationListUiState.Success
            assertTrue(state.mutationInFlight.isEmpty())
            assertNull(state.notifications[0].readAt)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `already-read notification does not re-mutate`() = runTest {
        val n1 = notification(id = "uuid-1", readAt = "2026-08-15T09:00:00+08:00")
        coEvery { repo.getNotifications(page = 1) } returns Result.success(page(listOf(n1)))
        coEvery { repo.markOneRead("uuid-1") } returns Result.success(Unit)
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            vm.markOneRead(n1)
            dispatcher.scheduler.runCurrent()

            coVerify(exactly = 0) { repo.markOneRead(any()) }
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    // --- Mark-all ---

    @Test
    fun `mark-all single-flight prevents concurrent calls`() = runTest {
        val n1 = notification(id = "uuid-1", readAt = null)
        val n2 = notification(id = "uuid-2", readAt = null)
        coEvery { repo.getNotifications(page = 1) } returns Result.success(page(listOf(n1, n2)))
        coEvery { repo.markAllRead() } coAnswers {
            kotlinx.coroutines.delay(5_000)
            Result.success(Unit)
        }
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            vm.markAllRead()
            dispatcher.scheduler.runCurrent()
            vm.markAllRead()
            dispatcher.scheduler.runCurrent()

            coVerify(exactly = 1) { repo.markAllRead() }
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `mark-all success marks all notifications as read`() = runTest {
        val n1 = notification(id = "uuid-1", readAt = null)
        val n2 = notification(id = "uuid-2", readAt = null)
        coEvery { repo.getNotifications(page = 1) } returns Result.success(page(listOf(n1, n2)))
        coEvery { repo.markAllRead() } returns Result.success(Unit)
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            vm.markAllRead()
            dispatcher.scheduler.runCurrent()

            val state = vm.uiState.value as NotificationListUiState.Success
            assertTrue(state.notifications.all { it.readAt != null })
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `mark-all failure shows patient-safe error`() = runTest {
        val n1 = notification(id = "uuid-1", readAt = null)
        coEvery { repo.getNotifications(page = 1) } returns Result.success(page(listOf(n1)))
        coEvery { repo.markAllRead() } returns Result.failure(RuntimeException("offline"))
        val vm = vm()

        try {
            dispatcher.scheduler.runCurrent()
            vm.markAllRead()
            dispatcher.scheduler.runCurrent()

            val state = vm.uiState.value as NotificationListUiState.Success
            assertEquals("Failed to mark all as read. Please try again.", state.inlineError)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    // --- Action mapping ---

    @Test
    fun `unknown action does not emit Navigate effect`() = runTest {
        val n1 = notification(id = "uuid-1", readAt = null, mobileAction = MobileDestination.UNKNOWN)
        coEvery { repo.getNotifications(page = 1) } returns Result.success(page(listOf(n1)))
        coEvery { repo.markOneRead("uuid-1") } returns Result.success(Unit)
        val vm = vm()

        try {
            vm.effects.test {
                dispatcher.scheduler.runCurrent()
                vm.markOneRead(n1)
                dispatcher.scheduler.runCurrent()

                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `conversation action emits Navigate effect`() = runTest {
        val n1 = notification(id = "uuid-1", readAt = null, mobileAction = MobileDestination.CONVERSATION)
        coEvery { repo.getNotifications(page = 1) } returns Result.success(page(listOf(n1)))
        coEvery { repo.markOneRead("uuid-1") } returns Result.success(Unit)
        val vm = vm()

        try {
            vm.effects.test {
                dispatcher.scheduler.runCurrent()
                vm.markOneRead(n1)
                dispatcher.scheduler.runCurrent()

                val effect = awaitItem()
                assertInstanceOf(NotificationEffect.Navigate::class.java, effect)
                assertEquals(MobileDestination.CONVERSATION, (effect as NotificationEffect.Navigate).destination)
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `tap on already-read notification with conversation action emits Navigate`() = runTest {
        val n1 = notification(id = "uuid-1", readAt = "2026-08-15T09:00:00+08:00", mobileAction = MobileDestination.CONVERSATION)
        coEvery { repo.getNotifications(page = 1) } returns Result.success(page(listOf(n1)))
        val vm = vm()

        try {
            vm.effects.test {
                dispatcher.scheduler.runCurrent()
                vm.onNotificationTap(n1)
                dispatcher.scheduler.runCurrent()

                val effect = awaitItem()
                assertInstanceOf(NotificationEffect.Navigate::class.java, effect)
                assertEquals(MobileDestination.CONVERSATION, (effect as NotificationEffect.Navigate).destination)
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun `load failure produces Error state with patient-safe message`() = runTest {
        coEvery { repo.getNotifications(page = 1) } returns Result.failure(RuntimeException("offline"))
        val vm = vm()

        try {
            vm.uiState.test {
                awaitItem() // Loading
                dispatcher.scheduler.runCurrent()
                val error = awaitItem() as NotificationListUiState.Error
                assertEquals("Unable to load notifications. Please try again.", error.patientSafeMessage)
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            vm.viewModelScope.cancel()
        }
    }
}
