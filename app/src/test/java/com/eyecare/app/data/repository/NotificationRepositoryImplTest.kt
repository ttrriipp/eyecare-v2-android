package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.NotificationApiService
import com.eyecare.app.data.remote.dto.NotificationDtos
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.MobileDestination
import com.eyecare.app.domain.model.NotificationKind
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Retrofit

class NotificationRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: NotificationRepositoryImpl
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @BeforeEach
    fun setup() {
        server = MockWebServer()
        server.start()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        repository = NotificationRepositoryImpl(retrofit.create(NotificationApiService::class.java))
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    private fun notificationJson(
        id: String = "550e8400-e29b-41d4-a716-446655440000",
        kind: String = "new_message",
        title: String = "New Message",
        body: String = "Dr. Santos sent a message.",
        mobileAction: String? = """{"type":"conversation"}""",
        readAt: String? = "null",
        createdAt: String = "2026-08-15T10:00:00+08:00",
    ): String {
        val action = mobileAction ?: "null"
        return """{"id":"$id","kind":"$kind","title":"$title","body":"$body","mobile_action":$action,"read_at":$readAt,"created_at":"$createdAt"}"""
    }

    private fun enqueueNotificationList(
        vararg notifications: String,
        page: Int = 1,
        lastPage: Int = 1,
        total: Int = notifications.size,
    ) {
        val data = notifications.joinToString(",")
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"data":[$data],"links":{"first":"...","last":"...","prev":null,"next":null},"meta":{"current_page":$page,"last_page":$lastPage,"per_page":20,"total":$total}}""",
            ),
        )
    }

    private fun enqueueUnreadCount(count: Int) {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"unread_count":$count}"""),
        )
    }

    private fun enqueueMessageResponse(message: String = "Done.") {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"message":"$message"}"""),
        )
    }

    private fun enqueueError(code: Int, body: String) {
        server.enqueue(MockResponse().setResponseCode(code).setBody(body))
    }

    // --- Page list ---

    @Test
    fun `getNotifications maps first page correctly`() = runTest {
        enqueueNotificationList(notificationJson())
        val result = repository.getNotifications(page = 1).getOrThrow()

        assertEquals(1, result.notifications.size)
        assertEquals(1, result.currentPage)
        assertEquals(1, result.lastPage)
        assertEquals(1, result.total)

        val n = result.notifications[0]
        assertEquals("550e8400-e29b-41d4-a716-446655440000", n.id)
        assertEquals(NotificationKind.NEW_MESSAGE, n.kind)
        assertEquals("New Message", n.title)
        assertEquals("Dr. Santos sent a message.", n.body)
        assertEquals(MobileDestination.CONVERSATION, n.mobileAction)
        assertNull(n.readAt)
        assertEquals("2026-08-15T10:00:00+08:00", n.createdAt)
    }

    @Test
    fun `getNotifications maps later page with correct metadata`() = runTest {
        enqueueNotificationList(
            notificationJson(id = "6ba7b810-9dad-11d1-80b4-00c04fd430c8"),
            page = 2,
            lastPage = 3,
            total = 50,
        )
        val result = repository.getNotifications(page = 2).getOrThrow()

        assertEquals(2, result.currentPage)
        assertEquals(3, result.lastPage)
        assertEquals(50, result.total)
    }

    @Test
    fun `getNotifications sends page and per_page query parameters`() = runTest {
        enqueueNotificationList(notificationJson())
        repository.getNotifications(page = 3).getOrThrow()

        val request = server.takeRequest()
        assertTrue(request.path!!.contains("page=3"))
        assertTrue(request.path!!.contains("per_page=20"))
    }

    // --- Unread count ---

    @Test
    fun `getUnreadCount returns count`() = runTest {
        enqueueUnreadCount(5)
        val count = repository.getUnreadCount().getOrThrow()
        assertEquals(5, count)
    }

    @Test
    fun `getUnreadCount returns zero`() = runTest {
        enqueueUnreadCount(0)
        val count = repository.getUnreadCount().getOrThrow()
        assertEquals(0, count)
    }

    // --- UUID mark-one ---

    @Test
    fun `markOneRead sends UUID as string path segment`() = runTest {
        val uuid = "550e8400-e29b-41d4-a716-446655440000"
        enqueueMessageResponse("Notification marked as read.")
        repository.markOneRead(uuid).getOrThrow()

        val request = server.takeRequest()
        assertTrue(request.path!!.contains("notifications/$uuid/read"))
        assertEquals("PATCH", request.method)
    }

    @Test
    fun `markOneRead does not coerce UUID to integer`() = runTest {
        val uuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
        enqueueMessageResponse("Notification marked as read.")
        repository.markOneRead(uuid).getOrThrow()

        val request = server.takeRequest()
        assertTrue(request.path!!.contains(uuid))
        assertFalse(request.path!!.matches(Regex(".*/notifications/\\d+/read.*")))
    }

    // --- Mark-all static path ---

    @Test
    fun `markAllRead uses static read-all path`() = runTest {
        enqueueMessageResponse("All notifications marked as read.")
        repository.markAllRead().getOrThrow()

        val request = server.takeRequest()
        assertTrue(request.path!!.contains("notifications/read-all"))
        assertEquals("PATCH", request.method)
    }

    // --- Unknown kind/action mapping ---

    @Test
    fun `unknown kind maps to UNKNOWN`() = runTest {
        enqueueNotificationList(notificationJson(kind = "unknown_future_kind"))
        val result = repository.getNotifications().getOrThrow()
        assertEquals(NotificationKind.UNKNOWN, result.notifications[0].kind)
    }

    @Test
    fun `unknown mobile action type maps to UNKNOWN`() = runTest {
        enqueueNotificationList(
            notificationJson(mobileAction = """{"type":"unknown_future_action"}"""),
        )
        val result = repository.getNotifications().getOrThrow()
        assertEquals(MobileDestination.UNKNOWN, result.notifications[0].mobileAction)
    }

    @Test
    fun `null mobile action maps to null`() = runTest {
        enqueueNotificationList(notificationJson(mobileAction = null))
        val result = repository.getNotifications().getOrThrow()
        assertNull(result.notifications[0].mobileAction)
    }

    @Test
    fun `null read_at maps to null`() = runTest {
        enqueueNotificationList(notificationJson(readAt = null))
        val result = repository.getNotifications().getOrThrow()
        assertNull(result.notifications[0].readAt)
    }

    @Test
    fun `non-null read_at maps correctly`() = runTest {
        enqueueNotificationList(notificationJson(readAt = "\"2026-08-14T09:00:00+08:00\""))
        val result = repository.getNotifications().getOrThrow()
        assertEquals("2026-08-14T09:00:00+08:00", result.notifications[0].readAt)
    }

    // --- Error propagation ---

    @Test
    fun `403 error propagates as ApiDomainError`() = runTest {
        enqueueError(
            403,
            """{"message":"This action is unauthorized."}""",
        )
        val result = repository.getNotifications()
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is ApiDomainError)
        assertEquals(403, (error as ApiDomainError).httpStatus)
    }

    @Test
    fun `422 error propagates as ApiDomainError`() = runTest {
        enqueueError(
            422,
            """{"message":"The given data was invalid.","errors":{"page":["The page must be at least 1."]}}""",
        )
        val result = repository.getNotifications(page = 0)
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is ApiDomainError)
        assertEquals(422, (error as ApiDomainError).httpStatus)
    }

    @Test
    fun `429 error propagates as ApiDomainError`() = runTest {
        enqueueError(429, """{"message":"Too Many Attempts."}""")
        val result = repository.getNotifications()
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is ApiDomainError)
        assertEquals(429, (error as ApiDomainError).httpStatus)
    }

    @Test
    fun `markOneRead 403 error propagates`() = runTest {
        enqueueError(403, """{"message":"This action is unauthorized."}""")
        val result = repository.markOneRead("550e8400-e29b-41d4-a716-446655440000")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ApiDomainError)
    }

    @Test
    fun `markAllRead 403 error propagates`() = runTest {
        enqueueError(403, """{"message":"This action is unauthorized."}""")
        val result = repository.markAllRead()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ApiDomainError)
    }
}

private fun assertFalse(condition: Boolean) {
    org.junit.jupiter.api.Assertions.assertFalse(condition)
}
