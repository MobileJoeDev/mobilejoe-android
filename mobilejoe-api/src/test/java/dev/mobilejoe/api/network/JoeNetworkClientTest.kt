// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.network

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.os.LocaleList
import android.util.Log
import dev.mobilejoe.api.identity.Identity
import dev.mobilejoe.api.identity.IdentityManager
import dev.mobilejoe.api.identity.InMemoryIdentityGateway
import dev.mobilejoe.api.identity.JoeStorageException
import dev.mobilejoe.api.identity.SequentialIdFactory
import dev.mobilejoe.api.runConcurrently
import dev.mobilejoe.api.system.SystemInfo
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.Collections
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JoeNetworkClientTest {

    private val originalHttpClient = getField<Deferred<*>>("httpClient")
    private val originalScope = getField<CoroutineScope>("scope")

    private val identityGateway = InMemoryIdentityGateway()

    /** Every request that reached the backend, in order. */
    private val requests = Collections.synchronizedList(mutableListOf<HttpRequestData>())

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        mockkObject(SystemInfo.Companion)
        every { SystemInfo.of(any()) } returns SystemInfo(
            clientVersionCode = 1,
            clientVersionName = "1.0",
            platformVersion = 1,
            version = "1.0",
            model = "Test",
            brand = "Test",
            locales = mockk(),
        )
        mockkStatic(LocaleList::toRegionalLanguageTags)
        every { any<LocaleList>().toRegionalLanguageTags() } returns emptyList()

        setField("identityManager", IdentityManager(identityGateway, SequentialIdFactory()))
        respondWith { respond(content = "[]", headers = jsonHeaders) }
    }

    @After
    fun tearDown() {
        setField("identityManager", null)
        setField("apiKey", null)
        setField("isApiKeyRejected", false)
        setField("systemInfo", null)
        setField("_isDebugMode", false)
        setField("httpClient", originalHttpClient)
        setField("scope", originalScope)
        unmockkAll()
    }

    @Test
    fun `Test configuration successfully message on debug runs`() = runTest {
        useBackgroundScope()

        JoeNetworkClient.configure(context(isDebuggable = true), "j_o_e", externalId = null)

        verify { Log.d("Joe", "🦫 Joe says: SDK configured successfully.") }
    }

    @Test
    fun `Test skip configuration successfully message on non-debug builds`() = runTest {
        useBackgroundScope()

        JoeNetworkClient.configure(context(), "j_o_e", externalId = null)

        verify(exactly = 0) { Log.d("Joe", any()) }
    }

    @Test
    fun `Test configure is usable before the identity is sent`() = runTest {
        useBackgroundScope()

        JoeNetworkClient.configure(context(), "j_o_e", externalId = "A", isDebugMode = true)

        val expectedIdentity = Identity("anonymous-1", "device-1", "external-A")
        assertEquals(expectedIdentity, JoeNetworkClient.currentIdentity)
        assertEquals(expectedIdentity, identityGateway.identity)
        assertEquals(true, JoeNetworkClient.isDebugMode)
        assertTrue(requests.isEmpty())

        JoeNetworkClient.getFaqs()

        assertTrue("/v1/faqs" in requestPaths())
    }

    @Test
    fun `Test configure sends the identity in the background`() = runTest {
        useBackgroundScope()

        JoeNetworkClient.configure(context(), "j_o_e", externalId = "A", isDebugMode = true)
        awaitBackgroundWork()

        val request = requests.single()
        assertEquals("POST", request.method.value)
        assertEquals("/v1/identify", request.url.encodedPath)
        assertEquals("Bearer j_o_e", request.headers["Authorization"])
        assertEquals("anonymous-1", request.headers["Identity-Anonymous-ID"])
        assertEquals("external-A", request.headers["Identity-External-ID"])
        assertEquals("device-1", request.headers["Identity-Device-ID"])
        assertEquals("true", request.headers["Debug-Mode"])
    }

    @Test
    fun `Test configure sends the persisted identity on every app start`() = runTest {
        useBackgroundScope()
        identityGateway.identity = Identity("anonymous-X", "device-X", "external-A")

        JoeNetworkClient.configure(context(), "j_o_e", externalId = "A")
        awaitBackgroundWork()

        assertEquals(listOf("/v1/identify"), requestPaths())
        assertEquals("anonymous-X", requests.single().headers["Identity-Anonymous-ID"])
        assertEquals(0, identityGateway.saveCount)
    }

    @Test
    fun `Test configure warns and ignores changed arguments after the first call`() = runTest {
        useBackgroundScope()

        JoeNetworkClient.configure(context(), "first-key", externalId = null, isDebugMode = false)
        JoeNetworkClient.configure(context(), "second-key", externalId = "B", isDebugMode = true)
        awaitBackgroundWork()

        verifyAlreadyConfiguredLogged(exactly = 1)
        assertEquals("first-key", getField<String?>("apiKey"))
        assertEquals(false, JoeNetworkClient.isDebugMode)
        assertEquals(Identity("anonymous-1", "device-1"), JoeNetworkClient.currentIdentity)
        assertEquals(listOf("/v1/identify"), requestPaths())
    }

    @Test
    fun `Test concurrent configure calls configure once`() = runTest {
        useBackgroundScope()
        identityGateway.saveDelayMillis = 20
        val context = context()

        runConcurrently(threads = 8) { index ->
            JoeNetworkClient.configure(context, "key-$index", externalId = null)
        }
        awaitBackgroundWork()

        val request = requests.single()
        assertEquals("/v1/identify", request.url.encodedPath)
        assertEquals("Bearer ${getField<String?>("apiKey")}", request.headers["Authorization"])
        assertEquals(Identity("anonymous-1", "device-1"), JoeNetworkClient.currentIdentity)
        assertEquals(1, identityGateway.saveCount)
        verifyAlreadyConfiguredLogged(exactly = 7)
    }

    @Test
    fun `Test configure with blank ID fails before any setup`() = runTest {
        useBackgroundScope()
        val storedIdentity = Identity("anonymous-X", "device-X", "external-A")
        identityGateway.identity = storedIdentity

        assertFailsWith<IllegalArgumentException> {
            JoeNetworkClient.configure(context(), "j_o_e", externalId = " ")
        }
        awaitBackgroundWork()

        assertNull(getField<String?>("apiKey"))
        assertTrue(requests.isEmpty())
        assertEquals(storedIdentity, identityGateway.identity)
        assertEquals(0, identityGateway.saveCount)
        assertEquals(0, identityGateway.deleteCount)
    }

    @Test
    fun `Test configure logs a storage failure and stays unconfigured`() = runTest {
        useBackgroundScope()
        identityGateway.failsSaving = { true }

        JoeNetworkClient.configure(context(isDebuggable = true), "j_o_e", externalId = null)
        awaitBackgroundWork()

        verifyStorageFailureLogged()
        verify(exactly = 0) { Log.d("Joe", any()) }
        assertNull(getField<String?>("apiKey"))
        assertTrue(requests.isEmpty())

        val exception = assertFailsWith<IllegalStateException> { JoeNetworkClient.getFaqs() }

        assertEquals(NOT_CONFIGURED_MESSAGE, exception.message)
    }

    @Test
    fun `Test configure logs locked identity storage and stays unconfigured`() = runTest {
        useBackgroundScope()
        setField("identityManager", null)
        val context = context()
        every { context.getSharedPreferences(any(), any()) } throws IllegalStateException("locked")

        JoeNetworkClient.configure(context, "j_o_e", externalId = null)
        awaitBackgroundWork()

        verifyStorageFailureLogged()
        assertNull(getField<String?>("apiKey"))
        assertTrue(requests.isEmpty())
        assertEquals(
            NOT_CONFIGURED_MESSAGE,
            assertFailsWith<IllegalStateException> { JoeNetworkClient.identify("A") }.message
        )
    }

    @Test
    fun `Test configure can be retried after a storage failure`() = runTest {
        useBackgroundScope()
        identityGateway.failsSaving = { true }
        JoeNetworkClient.configure(context(), "key-A", externalId = null)
        assertNull(getField<String?>("apiKey"))

        identityGateway.failsSaving = { false }
        JoeNetworkClient.configure(context(), "key-B", externalId = null)
        awaitBackgroundWork()

        assertEquals("key-B", getField<String?>("apiKey"))
        assertEquals(listOf("/v1/identify"), requestPaths())
    }

    @Test
    fun `Test configure creates the identity from the preferences on first launch`() = runTest {
        useBackgroundScope()
        setField("identityManager", null)
        val editor = mockk<SharedPreferences.Editor>(relaxed = true) {
            every { commit() } returns true
        }
        val sharedPreferences = mockk<SharedPreferences> {
            every { getString(any(), null) } returns null
            every { edit() } returns editor
        }
        val context = context()
        every { context.getSharedPreferences("dev.mobilejoe.identity", Context.MODE_PRIVATE) } returns sharedPreferences

        JoeNetworkClient.configure(context, "j_o_e", externalId = null)

        val identity = assertIs<Identity>(JoeNetworkClient.currentIdentity)
        verify { editor.putString("anonymous_id", identity.anonymousId) }
        verify { editor.putString("device_id", identity.deviceId) }
        verify { editor.commit() }
    }

    @Test
    fun `Test failed identify is logged and keeps the SDK enabled`() = runTest {
        useBackgroundScope()
        respondWith { request ->
            if (request.url.encodedPath == "/v1/identify") throw IOException("offline")
            respond(content = "[]", headers = jsonHeaders)
        }

        JoeNetworkClient.configure(context(), "j_o_e", externalId = "A")
        awaitBackgroundWork()

        verify {
            Log.w("Joe", "🦫 Joe says: Failed to identify. Retrying on next app start.", any<IOException>())
        }
        val expectedIdentity = Identity("anonymous-1", "device-1", "external-A")
        assertEquals(expectedIdentity, JoeNetworkClient.currentIdentity)
        assertEquals(expectedIdentity, identityGateway.identity)

        JoeNetworkClient.getFaqs()

        assertEquals(listOf("/v1/identify", "/v1/faqs"), requestPaths())
    }

    @Test
    fun `Test identify switches the identity before the request is sent`() = runTest {
        useBackgroundScope()
        identityGateway.identity = Identity("anonymous-X", "device-X", "external-A")
        JoeNetworkClient.configure(context(), "j_o_e", externalId = null)
        awaitBackgroundWork()

        JoeNetworkClient.identify("B")

        val expectedIdentity = Identity("anonymous-1", "device-1", "external-B")
        assertEquals(expectedIdentity, JoeNetworkClient.currentIdentity)
        assertEquals(expectedIdentity, identityGateway.identity)
        assertEquals(1, identityGateway.deleteCount)
        assertEquals(1, requests.size)

        awaitBackgroundWork()

        assertEquals(listOf("/v1/identify", "/v1/identify"), requestPaths())
        assertEquals("anonymous-1", requests.last().headers["Identity-Anonymous-ID"])
        assertEquals("external-B", requests.last().headers["Identity-External-ID"])
    }

    @Test
    fun `Test identity replaced while the HTTP client is built is not sent`() = runTest {
        useBackgroundScope()
        identityGateway.identity = Identity("anonymous-X", "device-X")
        val readyHttpClient = getField<Deferred<HttpClient>>("httpClient")
        val pendingHttpClient = CompletableDeferred<HttpClient>()
        setField("httpClient", pendingHttpClient)

        JoeNetworkClient.configure(context(), "j_o_e", externalId = null)
        runCurrent()
        JoeNetworkClient.identify("A")
        pendingHttpClient.complete(readyHttpClient.await())
        awaitBackgroundWork()

        assertEquals(listOf("/v1/identify"), requestPaths())
        assertEquals("anonymous-X", requests.single().headers["Identity-Anonymous-ID"])
        assertEquals("external-A", requests.single().headers["Identity-External-ID"])
    }

    @Test
    fun `Test identifying the current app user ID again sends nothing after a failure`() = runTest {
        useBackgroundScope()
        identityGateway.identity = Identity("anonymous-X", "device-X", "external-A")
        respondWith { respondError(HttpStatusCode.InternalServerError) }

        JoeNetworkClient.configure(context(), "j_o_e", externalId = null)
        JoeNetworkClient.identify("A")
        awaitBackgroundWork()
        JoeNetworkClient.identify("A")
        awaitBackgroundWork()

        assertEquals(listOf("/v1/identify"), requestPaths())
        assertEquals(0, identityGateway.deleteCount)
    }

    @Test
    fun `Test switching the app user ID in configure never sends the previous identity`() = runTest {
        useBackgroundScope()
        identityGateway.identity = Identity("anonymous-X", "device-X", "external-A")

        JoeNetworkClient.configure(context(), "j_o_e", externalId = "B")
        JoeNetworkClient.getAlerts()
        awaitBackgroundWork()

        assertEquals(2, requests.size)
        requests.forEach { request ->
            assertEquals("anonymous-1", request.headers["Identity-Anonymous-ID"])
            assertEquals("external-B", request.headers["Identity-External-ID"])
            assertEquals("device-1", request.headers["Identity-Device-ID"])
        }
    }

    @Test
    fun `Test logout sends the fresh anonymous identity`() = runTest {
        useBackgroundScope()
        JoeNetworkClient.configure(context(), "j_o_e", externalId = "A")
        awaitBackgroundWork()

        JoeNetworkClient.logout()

        val anonymousIdentity = Identity("anonymous-2", "device-2")
        assertEquals(anonymousIdentity, JoeNetworkClient.currentIdentity)
        assertEquals(anonymousIdentity, identityGateway.identity)

        awaitBackgroundWork()

        assertEquals(listOf("/v1/identify", "/v1/identify"), requestPaths())
        assertEquals("anonymous-2", requests.last().headers["Identity-Anonymous-ID"])
        assertNull(requests.last().headers["Identity-External-ID"])
        assertEquals("device-2", requests.last().headers["Identity-Device-ID"])
    }

    @Test
    fun `Test late identify response does not overwrite a later logout`() = runTest {
        useBackgroundScope()
        val identifyStarted = CompletableDeferred<Unit>()
        val releaseIdentify = CompletableDeferred<Unit>()
        respondWith {
            identifyStarted.complete(Unit)
            releaseIdentify.await()
            respond(content = "")
        }
        JoeNetworkClient.configure(context(), "j_o_e", externalId = "A")
        identifyStarted.await()

        JoeNetworkClient.logout()
        releaseIdentify.complete(Unit)
        awaitBackgroundWork()

        val anonymousIdentity = Identity("anonymous-2", "device-2")
        assertEquals(anonymousIdentity, JoeNetworkClient.currentIdentity)
        assertEquals(anonymousIdentity, identityGateway.identity)
        assertEquals("anonymous-2", requests.last().headers["Identity-Anonymous-ID"])
    }

    @Test
    fun `Test identify before configure fails`() = runTest {
        setField("identityManager", null)

        assertFailsWith<IllegalStateException> { JoeNetworkClient.identify("A") }
    }

    @Test
    fun `Test logout before configure fails`() = runTest {
        setField("identityManager", null)

        assertFailsWith<IllegalStateException> { JoeNetworkClient.logout() }
    }

    @Test
    fun `Test request before configure reports the missing API key`() = runTest {
        setField("identityManager", null)

        val exception = assertFailsWith<IllegalStateException> { JoeNetworkClient.getFaqs() }

        assertEquals(NOT_CONFIGURED_MESSAGE, exception.message)
    }

    @Test
    fun `Test 401 throws JoeInvalidApiKeyException and disables the SDK`() = runTest {
        configured()
        respondWith { respondError(HttpStatusCode.Unauthorized) }

        val rejection = assertFailsWith<JoeInvalidApiKeyException> { JoeNetworkClient.getFaqs() }

        assertEquals(401, rejection.statusCode)
        assertEquals("GET", rejection.method)
        assertEquals("/v1/faqs", rejection.path)

        val exception = assertFailsWith<IllegalStateException> { JoeNetworkClient.getAlerts() }

        assertEquals(
            "Invalid API key. The SDK is disabled until the app restarts with a valid key.",
            exception.message
        )
        assertEquals(listOf("/v1/faqs"), requestPaths())
        verifyInvalidApiKeyLogged(exactly = 1)
    }

    @Test
    fun `Test 401 for identify disables the SDK without a second log`() = runTest {
        useBackgroundScope()
        respondWith { respondError(HttpStatusCode.Unauthorized) }

        JoeNetworkClient.configure(context(), "j_o_e", externalId = null)
        awaitBackgroundWork()

        assertFailsWith<IllegalStateException> { JoeNetworkClient.getFaqs() }
        assertEquals(listOf("/v1/identify"), requestPaths())
        verifyInvalidApiKeyLogged(exactly = 1)
        verify(exactly = 0) { Log.w(any<String>(), any<String>(), any()) }
    }

    @Test
    fun `Test identify failing after another request's 401 is not logged`() = runTest {
        useBackgroundScope()
        val identifyStarted = CompletableDeferred<Unit>()
        val releaseIdentify = CompletableDeferred<Unit>()
        respondWith { request ->
            if (request.url.encodedPath == "/v1/identify") {
                identifyStarted.complete(Unit)
                releaseIdentify.await()
                throw IOException("offline")
            }
            respondError(HttpStatusCode.Unauthorized)
        }
        JoeNetworkClient.configure(context(), "j_o_e", externalId = null)
        identifyStarted.await()

        assertFailsWith<JoeInvalidApiKeyException> { JoeNetworkClient.getFaqs() }
        releaseIdentify.complete(Unit)
        awaitBackgroundWork()

        verifyInvalidApiKeyLogged(exactly = 1)
        verify(exactly = 0) { Log.w(any<String>(), any<String>(), any()) }
    }

    @Test
    fun `Test invalid API key is logged once for requests in flight`() = runTest {
        configured()
        val bothStarted = CompletableDeferred<Unit>()
        respondWith {
            if (requests.size == 2) bothStarted.complete(Unit)
            bothStarted.await()
            respondError(HttpStatusCode.Unauthorized)
        }

        val faqs = async { runCatching { JoeNetworkClient.getFaqs() } }
        val alerts = async { runCatching { JoeNetworkClient.getAlerts() } }

        assertIs<JoeInvalidApiKeyException>(faqs.await().exceptionOrNull())
        assertIs<JoeInvalidApiKeyException>(alerts.await().exceptionOrNull())
        verifyInvalidApiKeyLogged(exactly = 1)
    }

    @Test
    fun `Test no identify request after a rejected API key`() = runTest {
        useBackgroundScope()
        configured()
        respondWith { respondError(HttpStatusCode.Unauthorized) }
        assertFailsWith<JoeInvalidApiKeyException> { JoeNetworkClient.getFaqs() }

        JoeNetworkClient.logout()
        awaitBackgroundWork()

        assertEquals(Identity("anonymous-2", "device-2"), JoeNetworkClient.currentIdentity)
        assertEquals(listOf("/v1/faqs"), requestPaths())
        verify(exactly = 0) { Log.w(any<String>(), any<String>(), any()) }
    }

    @Test
    fun `Test getFaqs throws JoeHttpException for a 403 response and keeps the SDK enabled`() = runTest {
        configured()
        respondWith { respondError(HttpStatusCode.Forbidden) }

        val exception = assertFailsWith<JoeHttpException> { JoeNetworkClient.getFaqs() }

        assertEquals(403, exception.statusCode)
        assertEquals("GET", exception.method)
        assertEquals("/v1/faqs", exception.path)

        assertFailsWith<JoeHttpException> { JoeNetworkClient.getFaqs() }

        assertEquals(listOf("/v1/faqs", "/v1/faqs"), requestPaths())
        verifyInvalidApiKeyLogged(exactly = 0)
    }

    @Test
    fun `Test getFaq throws JoeHttpException for a 404 response`() = runTest {
        configured()
        respondWith { respondError(HttpStatusCode.NotFound) }

        val exception = assertFailsWith<JoeHttpException> { JoeNetworkClient.getFaq(faqId = 42) }

        assertEquals(404, exception.statusCode)
        assertEquals("GET", exception.method)
        assertEquals("/v1/faqs/42", exception.path)
    }

    @Test
    fun `Test getFaqs network failure stays IOException`() = runTest {
        configured()
        respondWith { throw IOException("offline") }

        assertFailsWith<IOException> { JoeNetworkClient.getFaqs() }
    }

    @Test
    fun `Test configure sends a Preferred-Languages header matching the locale extension output`() = runTest {
        useBackgroundScope()
        every { any<LocaleList>().toRegionalLanguageTags() } returns listOf("de-DE", "en-US", "fr-FR")

        JoeNetworkClient.configure(context(), "j_o_e", externalId = null)
        awaitBackgroundWork()

        assertEquals("de-DE,en-US,fr-FR", requests.single().headers["Preferred-Languages"])
    }

    @Test
    fun `Test configure omits the Preferred-Languages header when no locale is valid`() = runTest {
        useBackgroundScope()
        every { any<LocaleList>().toRegionalLanguageTags() } returns emptyList()

        JoeNetworkClient.configure(context(), "j_o_e", externalId = null)
        awaitBackgroundWork()

        assertNull(requests.single().headers["Preferred-Languages"])
    }

    /**
     * Puts the client into the state after [JoeNetworkClient.configure], for tests that exercise
     * a content request directly: an API key and an anonymous identity, without a background
     * `identify` request.
     */
    private fun configured() {
        setField("apiKey", "test-key")
        checkNotNull(getField<IdentityManager?>("identityManager")).identifyAtStart(null)
    }

    /**
     * Runs the client's background `identify` requests on this test's scheduler, so they start
     * only once the test suspends, e.g. in [awaitBackgroundWork].
     */
    private fun TestScope.useBackgroundScope() {
        setField("scope", CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler)))
    }

    /** Suspends until every background `identify` request launched so far has finished. */
    private suspend fun awaitBackgroundWork() {
        getField<CoroutineScope>("scope").coroutineContext.job.children.toList().joinAll()
    }

    /** Routes every request to [handler], recording it in [requests] first. */
    private fun respondWith(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) {
        val engine = MockEngine { request ->
            requests += request
            handler(request)
        }
        setField("httpClient", CompletableDeferred(JoeNetworkClient.buildJoeHttpClient(engine)))
    }

    private fun requestPaths() = requests.map { request -> request.url.encodedPath }

    private fun verifyAlreadyConfiguredLogged(exactly: Int) {
        verify(exactly = exactly) {
            Log.w(
                "Joe",
                "🦫 Joe says: SDK already configured. Nothing changed. " +
                    "Use `identify` to set a new `appUserId`."
            )
        }
    }

    private fun verifyStorageFailureLogged() {
        verify {
            Log.e(
                "Joe",
                "🦫 Joe says: Failed to store the identity. The SDK is not configured.",
                any<JoeStorageException>()
            )
        }
    }

    private fun verifyInvalidApiKeyLogged(exactly: Int) {
        verify(exactly = exactly) {
            Log.e(
                "Joe",
                "🦫 Joe says: Invalid API key. " +
                    "The SDK is disabled until the app restarts with a valid key."
            )
        }
    }

    private fun context(isDebuggable: Boolean = false): Context = mockk {
        every { applicationInfo } returns ApplicationInfo().apply {
            flags = if (isDebuggable) ApplicationInfo.FLAG_DEBUGGABLE else 0
        }
    }

    private fun setField(name: String, value: Any?) {
        JoeNetworkClient::class.java.getDeclaredField(name).apply {
            isAccessible = true
            set(JoeNetworkClient, value)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getField(name: String): T {
        return JoeNetworkClient::class.java.getDeclaredField(name).apply {
            isAccessible = true
        }.get(JoeNetworkClient) as T
    }

    private companion object {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

        const val NOT_CONFIGURED_MESSAGE =
            "The SDK is not configured. Call `Joe.configure(...)` before making any call. " +
                "If you did, check Logcat for the tag `Joe`."
    }
}
