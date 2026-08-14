package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.ClinicHoursDtos
import retrofit2.http.GET

interface ClinicApiService {
    @GET("clinic-hours")
    suspend fun getClinicHours(): ClinicHoursDtos.ClinicHoursListResponse
}
