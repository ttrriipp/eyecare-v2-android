package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.PatientIntakeApiService
import com.eyecare.app.domain.model.IntakeStatus
import com.eyecare.app.domain.repository.SaveIntakeRequest
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

class PatientIntakeRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: PatientIntakeRepositoryImpl
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
        repository = PatientIntakeRepositoryImpl(retrofit.create(PatientIntakeApiService::class.java), json)
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    @Test
    fun `getIntake returns null when no intake exists`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"data": null}"""))

        val result = repository.getIntake(1).getOrThrow()
        assertNull(result)
    }

    @Test
    fun `getIntake maps draft intake correctly`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """
            {"data":{"id":1,"patient_id":1,"appointment_id":1,"status":"draft",
            "appointment_type":"New Patient","full_name":"Ana Reyes","date_of_birth":"1990-05-15",
            "gender":"female","occupation":"Teacher","address":"123 Main St","phone":"09171234567",
            "email":"ana@example.com","chief_complaint":"Blurred vision","submitted_at":null,"verified_at":null}}
            """.trimIndent(),
        ))

        val intake = repository.getIntake(1).getOrThrow()!!
        assertEquals(IntakeStatus.DRAFT, intake.status)
        assertEquals("Ana Reyes", intake.fullName)
        assertEquals("Blurred vision", intake.chiefComplaint)
        assertNull(intake.submittedAt)
    }

    @Test
    fun `saveIntake maps response correctly`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """
            {"data":{"id":1,"patient_id":1,"appointment_id":1,"status":"draft",
            "full_name":"Ana Reyes","chief_complaint":"Updated complaint","submitted_at":null,"verified_at":null}}
            """.trimIndent(),
        ))

        val result = repository.saveIntake(1, SaveIntakeRequest(fullName = "Ana Reyes", chiefComplaint = "Updated complaint"))
        assertTrue(result.isSuccess)
        assertEquals("Updated complaint", result.getOrThrow().chiefComplaint)
    }

    @Test
    fun `saveIntake handles 201 created response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody(
            """
            {"data":{"id":2,"patient_id":1,"appointment_id":1,"status":"draft",
            "full_name":"New Patient","submitted_at":null,"verified_at":null}}
            """.trimIndent(),
        ))

        val result = repository.saveIntake(1, SaveIntakeRequest(fullName = "New Patient"))
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().id)
    }

    @Test
    fun `submitIntake returns submitted intake`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """
            {"data":{"id":1,"patient_id":1,"appointment_id":1,"status":"submitted",
            "full_name":"Ana Reyes","submitted_at":"2026-07-27T10:00:00+08:00","verified_at":null}}
            """.trimIndent(),
        ))

        val result = repository.submitIntake(1).getOrThrow()
        assertEquals(IntakeStatus.SUBMITTED, result.status)
        assertEquals("2026-07-27T10:00:00+08:00", result.submittedAt)
    }

    @Test
    fun `submitIntake 422 maps to ValidationError`() = runTest {
        server.enqueue(MockResponse().setResponseCode(422).setBody(
            """{"message":"Only draft intakes can be submitted.","errors":{"status":["Only draft intakes can be submitted."]}}""",
        ))

        val result = repository.submitIntake(1)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IntakeError.ValidationError)
    }
}
