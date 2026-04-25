package com.phoneagent.data.remote

import com.phoneagent.data.remote.models.GrokRequest
import com.phoneagent.data.remote.models.GrokResponse
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface GrokApiService {
    @Headers("Content-Type: application/json")
    @POST("chat/completions")
    suspend fun sendRequest(@Body request: GrokRequest): GrokResponse
}
