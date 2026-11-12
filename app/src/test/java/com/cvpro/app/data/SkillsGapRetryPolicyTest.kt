package com.cvpro.app.data

import com.cvpro.app.data.mapper.toEvaluationRequest
import com.cvpro.app.di.NetworkModule
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class SkillsGapRetryPolicyTest {
    @Test fun `a failed POST is not automatically retried by the analysis client`() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            val sharedClient = OkHttpClient.Builder().readTimeout(2, TimeUnit.SECONDS).build()
            val retrofit = Retrofit.Builder().baseUrl(server.url("/api/"))
                .addConverterFactory(GsonConverterFactory.create()).build()
            val api = NetworkModule.provideSkillsGapApi(retrofit, sharedClient)
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
            server.enqueue(MockResponse().setBody(evaluationJson))
            try {
                api.evaluateForJob(evaluationCv().toEvaluationRequest(evaluationOffer()))
                fail("An explicit retry is required")
            } catch (_: IOException) { /* No automatic retry of this POST. */ }
            assertEquals(1, server.requestCount)
            assertTrue(sharedClient.retryOnConnectionFailure) // Other services retain their existing policy.
        } finally { server.shutdown() }
    }
}
