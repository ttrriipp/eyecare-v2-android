package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.ClinicApiService
import com.eyecare.app.data.remote.dto.ClinicHoursDtos
import com.eyecare.app.domain.model.ClinicHoursDay
import com.eyecare.app.domain.repository.ClinicRepository
import javax.inject.Inject

class ClinicRepositoryImpl @Inject constructor(
    private val api: ClinicApiService,
) : ClinicRepository {

    override suspend fun getClinicHours(): Result<List<ClinicHoursDay>> = runCatching {
        api.getClinicHours().data.map { it.toDomain() }
    }

    private fun ClinicHoursDtos.ClinicHoursDayDto.toDomain() = ClinicHoursDay(
        weekday = weekday,
        dayName = dayName,
        enabled = enabled,
        openTime = openTime,
        closeTime = closeTime,
    )
}
