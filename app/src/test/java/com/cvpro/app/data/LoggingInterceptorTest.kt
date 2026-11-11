package com.cvpro.app.data

import com.cvpro.app.data.remote.LoggingInterceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class LoggingInterceptorTest {
    private val privateBody = """{"name":"Private Name","email":"private@example.org","phone":"private-phone","experience":"private-experience"}"""

    @Test fun debugLogsOnlyStatusAndNeverReadsPrivateBodiesOrLogsUrlsAndHeaders() {
        for (path in listOf("/cvs/private-cv", "/skills-gap/evaluate", "/skills-gap/recommendations",
            "/profile", "/future-private-api")) {
            for (status in listOf(200, 401, 500)) {
                val messages = mutableListOf<String>()
                val body = ObservedBody(privateBody)
                val client = OkHttpClient.Builder()
                    .addInterceptor(LoggingInterceptor(true, messages::add))
                    .addInterceptor { chain ->
                        assertEquals("Bearer private-token", chain.request().header("Authorization"))
                        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                            .code(status).message("private-status-description")
                            .header("Set-Cookie", "private-cookie").body(body).build()
                    }.build()
                val request = Request.Builder().url("https://private.example$path?email=private@example.org")
                    .header("Authorization", "Bearer private-token").build()

                client.newCall(request).execute().use { response ->
                    assertEquals(listOf("HTTP $status"), messages)
                    assertEquals(0, body.reads)
                    assertEquals(status, response.code)
                    assertEquals("private-cookie", response.header("Set-Cookie"))
                    assertEquals(privateBody, response.body!!.string())
                    assertTrue(body.reads > 0)
                }
            }
        }
    }

    @Test fun disabledLoggingLeavesResponsesUntouchedAndEmitsNothing() {
        val messages = mutableListOf<String>()
        val body = ObservedBody(privateBody)
        val client = OkHttpClient.Builder()
            .addInterceptor(LoggingInterceptor(false, messages::add))
            .addInterceptor { chain ->
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(200).message("OK").body(body).build()
            }.build()
        client.newCall(Request.Builder().url("https://private.example/cvs/private-cv").build()).execute().use {
            assertTrue(messages.isEmpty())
            assertEquals(0, body.reads)
            assertEquals(privateBody, it.body!!.string())
        }
    }

    @Test fun networkFailurePropagatesWithoutLoggingExceptionDetails() {
        val messages = mutableListOf<String>()
        val failure = IOException("private-token private-url private-email")
        val client = OkHttpClient.Builder()
            .addInterceptor(LoggingInterceptor(true, messages::add))
            .addInterceptor { throw failure }.build()
        try {
            client.newCall(Request.Builder().url("https://private.example/cvs/private-cv").build()).execute()
            fail("Se esperaba la excepción de transporte")
        } catch (error: IOException) {
            assertSame(failure, error)
        }
        assertTrue(messages.isEmpty())
    }
}

private class ObservedBody(private val content: String) : ResponseBody() {
    var reads = 0
        private set
    private val observedSource = object : ForwardingSource(Buffer().writeUtf8(content)) {
        override fun read(sink: Buffer, byteCount: Long): Long {
            reads++
            return super.read(sink, byteCount)
        }
    }.buffer()

    override fun contentType(): MediaType? = null
    override fun contentLength(): Long = content.toByteArray(Charsets.UTF_8).size.toLong()
    override fun source(): BufferedSource = observedSource
}
