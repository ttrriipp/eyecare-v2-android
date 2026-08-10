package com.eyecare.app.data.remote.api

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppointmentRequestApiServiceTest {

    @Test
    fun `service has getAppointmentTypes method`() {
        val method = AppointmentRequestApiService::class.java.declaredMethods.firstOrNull { it.name == "getAppointmentTypes" }
        assertTrue(method != null, "getAppointmentTypes method must exist")
    }

    @Test
    fun `service has availability method`() {
        val method = AppointmentRequestApiService::class.java.declaredMethods.firstOrNull { it.name == "getAvailability" }
        assertTrue(method != null, "getAvailability method must exist")
    }

    @Test
    fun `service has getRequests method`() {
        val method = AppointmentRequestApiService::class.java.declaredMethods.firstOrNull { it.name == "getRequests" }
        assertTrue(method != null, "getRequests method must exist")
    }

    @Test
    fun `service has createRequest method`() {
        val method = AppointmentRequestApiService::class.java.declaredMethods.firstOrNull { it.name == "createRequest" }
        assertTrue(method != null, "createRequest method must exist")
    }

    @Test
    fun `service has getRequest method`() {
        val method = AppointmentRequestApiService::class.java.declaredMethods.firstOrNull { it.name == "getRequest" }
        assertTrue(method != null, "getRequest method must exist")
    }

    @Test
    fun `service has cancelRequest method`() {
        val method = AppointmentRequestApiService::class.java.declaredMethods.firstOrNull { it.name == "cancelRequest" }
        assertTrue(method != null, "cancelRequest method must exist")
    }
}
