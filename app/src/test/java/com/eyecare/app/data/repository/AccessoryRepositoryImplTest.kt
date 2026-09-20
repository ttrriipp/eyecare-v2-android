package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.AccessoryApiService
import com.eyecare.app.domain.model.AccessoryAvailability
import com.eyecare.app.domain.model.AccessoryQuery
import com.eyecare.app.domain.model.ApiDomainError
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
import retrofit2.Retrofit
import java.math.BigDecimal

class AccessoryRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: AccessoryRepositoryImpl
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
        repository = AccessoryRepositoryImpl(retrofit.create(AccessoryApiService::class.java))
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    private fun accessoryJson(
        id: Int = 1,
        name: String = "Daily Care Kit",
        brand: String? = "Acme",
        category: String? = "Lens Care",
        rating: Double? = 4.5,
        ratingCount: Int = 12,
        variants: String = """[{"id":101,"name":"90 mL","price":"350.00","attributes":{},"images":[],"availability":"available"}]""",
    ): String {
        val brandJson = brand?.let { "\"$it\"" } ?: "null"
        val categoryJson = category?.let { "\"$it\"" } ?: "null"
        val ratingJson = rating?.toString() ?: "null"
        return """{"id":$id,"name":"$name","slug":"slug-$id","description":"Test","brand":$brandJson,"category":$categoryJson,"images":[],"average_rating":$ratingJson,"rating_count":$ratingCount,"variants":$variants}"""
    }

    private fun enqueueList(vararg accessories: String, lastPage: Int = 1, total: Int = accessories.size) {
        val data = accessories.joinToString(",")
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"data":[$data],"meta":{"current_page":1,"last_page":$lastPage,"per_page":15,"total":$total}}""",
        ))
    }

    private fun enqueueSingle(json: String) {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"data":$json}"""))
    }

    @Test
    fun `getAccessories returns paginated list`() = runTest {
        enqueueList(accessoryJson())
        val result = repository.getAccessories(AccessoryQuery()).getOrThrow()
        assertEquals(1, result.data.size)
        val a = result.data[0]
        assertEquals(1, a.id)
        assertEquals("Daily Care Kit", a.name)
        assertEquals("Acme", a.brand)
        assertEquals("Lens Care", a.category)
        assertEquals(BigDecimal("350.00"), a.variants[0].price)
    }

    @Test
    fun `getAccessory returns single accessory`() = runTest {
        enqueueSingle(accessoryJson(id = 42))
        val a = repository.getAccessory(42).getOrThrow()
        assertEquals(42, a.id)
        val request = server.takeRequest()
        assertTrue(request.path!!.contains("accessories/42"))
    }

    @Test
    fun `unknown availability maps to UNKNOWN`() = runTest {
        enqueueSingle(accessoryJson(variants = """[{"id":1,"name":"V","price":"100.00","attributes":{},"images":[],"availability":"future_value"}]"""))
        val a = repository.getAccessory(1).getOrThrow()
        assertEquals(AccessoryAvailability.UNKNOWN, a.variants[0].availability)
    }

    @Test
    fun `unavailable variant is not orderable`() = runTest {
        enqueueSingle(accessoryJson(variants = """[{"id":1,"name":"V","price":"100.00","attributes":{},"images":[],"availability":"unavailable"}]"""))
        val a = repository.getAccessory(1).getOrThrow()
        assertEquals(AccessoryAvailability.UNAVAILABLE, a.variants[0].availability)
        assertTrue(!a.variants[0].availability.isOrderable)
    }

    @Test
    fun `low_stock variant is orderable`() = runTest {
        enqueueSingle(accessoryJson(variants = """[{"id":1,"name":"V","price":"100.00","attributes":{},"images":[],"availability":"low_stock"}]"""))
        val a = repository.getAccessory(1).getOrThrow()
        assertEquals(AccessoryAvailability.LOW_STOCK, a.variants[0].availability)
        assertTrue(a.variants[0].availability.isOrderable)
    }

    @Test
    fun `null brand and category map to null`() = runTest {
        val json = """{"id":1,"name":"Test","slug":"test","description":"Test","brand":null,"category":null,"images":[],"average_rating":null,"rating_count":0,"variants":[]}"""
        enqueueSingle(json)
        val a = repository.getAccessory(1).getOrThrow()
        assertNull(a.brand)
        assertNull(a.category)
    }

    @Test
    fun `heterogeneous attributes convert to string map`() = runTest {
        enqueueSingle(accessoryJson(variants = """[{"id":1,"name":"V","price":"100.00","attributes":{"volume_ml":90,"package_size":"90 mL","is_organic":true},"images":[],"availability":"available"}]"""))
        val a = repository.getAccessory(1).getOrThrow()
        val attrs = a.variants[0].attributes
        assertEquals("90", attrs["volume_ml"])
        assertEquals("90 mL", attrs["package_size"])
        assertEquals("true", attrs["is_organic"])
    }

    @Test
    fun `preserves server ordering`() = runTest {
        enqueueList(accessoryJson(id = 3), accessoryJson(id = 1), lastPage = 2, total = 2)
        val result = repository.getAccessories(AccessoryQuery()).getOrThrow()
        assertEquals(3, result.data[0].id)
        assertEquals(1, result.data[1].id)
        assertEquals(2, result.lastPage)
    }

    @Test
    fun `404 maps to ApiDomainError`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody(
            """{"message":"Not found"}""",
        ))
        val result = repository.getAccessory(999)
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is ApiDomainError)
    }
}