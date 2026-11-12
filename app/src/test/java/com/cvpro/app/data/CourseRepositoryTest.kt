package com.cvpro.app.data

import com.cvpro.app.data.remote.api.CourseApi
import com.cvpro.app.data.repository.ApiCourseRepository
import com.cvpro.app.domain.model.*
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CourseRepositoryTest {
    private fun repository(server: MockWebServer) = ApiCourseRepository(Retrofit.Builder()
        .baseUrl(server.url("/api/v1/")).addConverterFactory(GsonConverterFactory.create()).build().create(CourseApi::class.java))

    @Test fun exactFiltersAndPaginationUseTheAuthenticatedGatewayRelativePath() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(pageJson()))
            val result = repository(server).getCourses(1, 5, CourseFilters("Microsoft Learn", false, "C++"))
            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/courses", request.requestUrl!!.encodedPath)
            assertEquals(setOf("page", "pageSize", "platform", "isPaid", "skillName"), request.requestUrl!!.queryParameterNames)
            assertEquals("1", request.requestUrl!!.queryParameter("page"))
            assertEquals("5", request.requestUrl!!.queryParameter("pageSize"))
            assertEquals("Microsoft Learn", request.requestUrl!!.queryParameter("platform"))
            assertEquals("false", request.requestUrl!!.queryParameter("isPaid"))
            assertEquals("C++", request.requestUrl!!.queryParameter("skillName"))
            assertNull(request.getHeader("X-User-Id"))
            assertNull(result.items.single().durationHours)
            assertEquals(42L, result.totalRecords)
            assertTrue(result.hasMore)
        }
    }
    @Test fun courseClientReusesAuthInterceptorWithoutConstructingIdentityHeaders() = runTest {
        val auth = object : com.cvpro.app.domain.repository.AuthRepository {
            override val currentUser = kotlinx.coroutines.flow.MutableStateFlow<com.cvpro.app.domain.model.User?>(null)
            override val idToken = kotlinx.coroutines.flow.MutableStateFlow<String?>("opaque-test-token")
            override val sessionExpired = kotlinx.coroutines.flow.MutableStateFlow(false)
            override fun onUnauthorized() {}
            override fun onExpiryNoticeShown() {}
            override suspend fun signIn(activityContext: android.content.Context): com.cvpro.app.domain.repository.SignInResult = error("unused")
            override fun classicSignInIntent(activityContext: android.content.Context): android.content.Intent = error("unused")
            override fun completeClassicSignIn(data: android.content.Intent?): com.cvpro.app.domain.repository.SignInResult = error("unused")
            override suspend fun signOut() {}
        }
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(pageJson()))
            val retrofit = Retrofit.Builder().baseUrl(server.url("/api/v1/"))
                .client(okhttp3.OkHttpClient.Builder().addInterceptor(com.cvpro.app.data.remote.AuthInterceptor(auth)).build())
                .addConverterFactory(GsonConverterFactory.create()).build()
            ApiCourseRepository(retrofit.create(CourseApi::class.java)).getCourses(1, 5, CourseFilters())
            val request = server.takeRequest()
            assertEquals("Bearer opaque-test-token", request.getHeader("Authorization"))
            assertEquals("android", request.getHeader("X-Client"))
            assertNull(request.getHeader("X-User-Id"))
            assertNull(request.getHeader("X-ARR-ClientCert"))
        }
    }
    @Test fun nullableFiltersAreOmittedAndZeroDurationIsDifferentFromUnknown() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(pageJson().replace("\"durationHours\":null", "\"durationHours\":0")))
            val result = repository(server).getCourses(1, 5, CourseFilters())
            assertEquals(setOf("page", "pageSize"), server.takeRequest().requestUrl!!.queryParameterNames)
            assertEquals(0, result.items.single().durationHours)
        }
    }
    @Test fun emptyPagesPreserveCountsAndNeverImplyRouteUnavailable() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"items":[],"page":2,"pageSize":5,"totalRecords":5}"""))
            val result = repository(server).getCourses(2, 5, CourseFilters())
            assertEquals(5L, result.totalRecords)
            assertTrue(result.items.isEmpty())
            assertFalse(result.hasMore)
        }
    }
    @Test fun dataDefectsAreExcludedWithoutChangingTotalRecords() = runTest {
        for ((title, url) in listOf("[DEMO] Python" to "https://learn.microsoft.com/training/", "[test] Python" to "https://learn.microsoft.com/training/", "Python" to "https://sub.example.org/course")) {
            MockWebServer().use { server ->
                server.enqueue(MockResponse().setBody(pageJson(title, url)))
                val result = repository(server).getCourses(1, 5, CourseFilters())
                assertTrue(result.items.isEmpty())
                assertEquals(1, result.excludedRecords)
                assertEquals(42L, result.totalRecords)
                assertTrue(result.hasMore)
            }
        }
        assertFalse(safeCourseUrl("javascript:alert(1)"))
        assertFalse(safeCourseUrl("https://person:secret@learn.microsoft.com/"))
        assertFalse(isRealCourse("Course", "[DEMO] Provider", "https://learn.microsoft.com/", emptyList()))
    }
    @Test fun authenticationRouteDatabaseAndMalformedResponsesRemainDistinct() = runTest {
        for ((status, failure) in listOf(401 to CourseFailure.AUTHENTICATION, 403 to CourseFailure.AUTHENTICATION,
            404 to CourseFailure.ROUTE_UNAVAILABLE, 405 to CourseFailure.ROUTE_UNAVAILABLE, 503 to CourseFailure.UNAVAILABLE)) {
            MockWebServer().use { server ->
                server.enqueue(MockResponse().setResponseCode(status))
                try { repository(server).getCourses(1, 5, CourseFilters()); fail("Expected $status") }
                catch (error: CourseException) { assertEquals(failure, error.failure) }
            }
        }
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("{}"))
            try { repository(server).getCourses(1, 5, CourseFilters()); fail("Expected invalid response") }
            catch (error: CourseException) { assertEquals(CourseFailure.INVALID_RESPONSE, error.failure) }
        }
    }
    @Test fun deficitEligibilityRequiresEveryBackendCondition() {
        val eligible = JobLevelAssessment("Python", SkillLevel.BEGINNER, SkillLevel.ADVANCED, "description", "Python avanzado", "explicit", null, "documented_skill_below_required", 2)
        assertTrue(eligible.canSuggestLevelCourses())
        for (assessment in listOf(eligible.copy(status = "documented_skill_meets_required"), eligible.copy(detectionStatus = "unknown"),
            eligible.copy(detectionStatus = "conflict", reason = "contradictory"), eligible.copy(requiredLevel = null),
            eligible.copy(deficitSteps = 0), eligible.copy(deficitSteps = null), eligible.copy(status = "not_documented", cvLevel = null)))
            assertFalse(assessment.canSuggestLevelCourses())
    }
    private fun pageJson(title: String = "Python", url: String = "https://learn.microsoft.com/training/") = """
        {"items":[{"id":"10","title":"$title","platform":"Microsoft Learn","url":"$url","logoUrl":null,
        "durationHours":null,"isPaid":false,"skills":["Python"]}],"page":1,"pageSize":5,"totalRecords":42}
    """.trimIndent()
}
