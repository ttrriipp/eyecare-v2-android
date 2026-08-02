package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.ContactOtpRequest
import com.eyecare.app.data.remote.dto.ContactResponse
import com.eyecare.app.data.remote.dto.ContactVerifyRequest
import com.eyecare.app.data.remote.dto.ContactsResponse
import com.eyecare.app.data.remote.dto.InvitationAcceptRequest
import com.eyecare.app.data.remote.dto.InvitationAcceptResponse
import com.eyecare.app.data.remote.dto.InvitationOtpRequest
import com.eyecare.app.data.remote.dto.LinkStateResponse
import com.eyecare.app.data.remote.dto.PatientLinkRequestResponse
import com.eyecare.app.data.remote.dto.PasswordChangeRequest
import com.eyecare.app.data.remote.dto.PasswordChangeResponse
import com.eyecare.app.data.remote.dto.RegistrationOtpResponse
import com.eyecare.app.data.remote.dto.StepUpOtpRequest
import com.eyecare.app.data.remote.dto.StepUpOtpResponse
import com.eyecare.app.data.remote.dto.StepUpVerifyRequest
import com.eyecare.app.data.remote.dto.StepUpVerifyResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface AccountApiService {

    @GET("account/contacts")
    suspend fun getContacts(): ContactsResponse

    @POST("account/contacts/otp")
    suspend fun requestContactOtp(
        @Body request: ContactOtpRequest,
        @Header("X-Step-Up-Token") stepUpToken: String,
    ): RegistrationOtpResponse

    @POST("account/contacts/verify")
    suspend fun verifyContactOtp(@Body request: ContactVerifyRequest): ContactResponse

    @PATCH("account/contacts/{contact}/primary")
    suspend fun makeContactPrimary(
        @Path("contact") contactId: Int,
        @Header("X-Step-Up-Token") stepUpToken: String,
    ): ContactsResponse

    @DELETE("account/contacts/{contact}")
    suspend fun removeContact(
        @Path("contact") contactId: Int,
        @Header("X-Step-Up-Token") stepUpToken: String,
    )

    @POST("auth/step-up/otp")
    suspend fun requestStepUpOtp(@Body request: StepUpOtpRequest): StepUpOtpResponse

    @POST("auth/step-up/verify")
    suspend fun verifyStepUpOtp(@Body request: StepUpVerifyRequest): StepUpVerifyResponse

    @POST("auth/password")
    suspend fun changePassword(
        @Body request: PasswordChangeRequest,
        @Header("X-Step-Up-Token") stepUpToken: String,
    ): PasswordChangeResponse

    @GET("account/link")
    suspend fun getLinkState(): LinkStateResponse

    @POST("patient-link-requests")
    suspend fun submitPatientLinkRequest(): PatientLinkRequestResponse

    @GET("patient-link-requests/current")
    suspend fun getCurrentPatientLinkRequest(): Response<PatientLinkRequestResponse>

    @POST("patient-invitations/acceptance/otp")
    suspend fun requestInvitationOtp(@Body request: InvitationOtpRequest): RegistrationOtpResponse

    @POST("patient-invitations/accept")
    suspend fun acceptInvitation(@Body request: InvitationAcceptRequest): InvitationAcceptResponse
}
