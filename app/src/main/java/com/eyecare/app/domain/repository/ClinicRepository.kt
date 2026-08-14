package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.ClinicHoursDay

interface ClinicRepository {
    suspend fun getClinicHours(): Result<List<ClinicHoursDay>>
}
