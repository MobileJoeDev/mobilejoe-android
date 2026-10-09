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
import dev.mobilejoe.api.identity.SequentialIdFactory
import dev.mobilejoe.api.system.SystemInfo
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class JoeNetworkClientTest {

    private val originalHttpClient = getHttpClient()

    @After
    fun tearDown() {
        setIdentityManager(null)
        setApiKey(null)
        setSystemInfo(null)
        setHttpClient(originalHttpClient)
        unmockkAll()
    }

    @Test
    fun `Test configuration successfully message on debug runs`() = runTest {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        mockIdentifyCall()

        JoeNetworkClient.configure(preferencesContext(isDebuggable = true), "j_o_e", externalId = null)

        advanceUntilIdle()
        verify { Log.d("Joe", "🦫 Joe says: SDK configured successfully.") }

        unmockkAll()
    }

    @Test
    fun `Test skip configuration successfully message on non-debug builds`() = runTest {
        mockkStatic(Log::class)
        mockIdentifyCall()

        JoeNetworkClient.configure(preferencesContext(isDebuggable = false), "j_o_e", externalId = null)

        advanceUntilIdle()
        verify(exactly = 0) { Log.d("Joe", any()) }

        unmockkAll()
    }

    @Test
    fun `Test warns and skips reconfiguration when already configured`() = runTest {
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>()) } returns 0
        setApiKey("existing-key")

        JoeNetworkClient.configure(preferencesContext(isDebuggable = true), "new-key", externalId = null)

        advanceUntilIdle()
        verify {
            Log.w(
                "Joe",
                "🦫 Joe says: SDK already configured. Nothing changed. " +
                    "Use `identify` to set a new `appUserId`."
            )
        }
    }

    @Test
    fun `Test configure ignores changed arguments after successful configuration`() = runTest {
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>()) } returns 0
        mockIdentifyCall()

        JoeNetworkClient.configure(
            preferencesContext(isDebuggable = false), "first-key", externalId = null, isDebugMode = false
        )
        JoeNetworkClient.configure(
            preferencesContext(isDebuggable = false), "second-key", externalId = "external-B", isDebugMode = true
        )

        assertEquals("first-key", getApiKey())
        assertEquals(false, JoeNetworkClient.isDebugMode)
        coVerify(exactly = 1) {
            JoeNetworkClient["identifyInternal"](any<Identity>(), any<String>(), any<Boolean>())
        }
    }

    @Test
    fun `Test concurrent configure calls share one identify request`() = runTest {
        mockIdentifyCall()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        stubBlockingIdentifyInternal(started, release)

        val callA = async {
            JoeNetworkClient.configure(preferencesContext(isDebuggable = false), "j_o_e", externalId = null)
        }
        advanceUntilIdle()
        started.await()
        val callB = async {
            JoeNetworkClient.configure(preferencesContext(isDebuggable = false), "j_o_e", externalId = null)
        }
        advanceUntilIdle()
        release.complete(Unit)

        callA.await()
        callB.await()

        assertEquals("j_o_e", getApiKey())
        coVerify(exactly = 1) {
            JoeNetworkClient["identifyInternal"](any<Identity>(), any<String>(), any<Boolean>())
        }
    }

    @Test
    fun `Test concurrent configure calls apply only the first caller's arguments`() = runTest {
        val identityGateway = InMemoryIdentityGateway(null)
        setIdentityManager(IdentityManager(identityGateway, SequentialIdFactory()))
        mockIdentifyCall()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        stubBlockingIdentifyInternal(started, release)

        val callA = async {
            JoeNetworkClient.configure(preferencesContext(isDebuggable = false), "key-A", externalId = "A")
        }
        advanceUntilIdle()
        started.await()
        val callB = async {
            JoeNetworkClient.configure(preferencesContext(isDebuggable = false), "key-B", externalId = "B")
        }
        advanceUntilIdle()
        release.complete(Unit)

        callA.await()
        callB.await()

        val expectedIdentity = Identity("anonymous-1", "device-1", "external-A")
        assertEquals("key-A", getApiKey())
        assertEquals(expectedIdentity, JoeNetworkClient.currentIdentity)
        coVerify(exactly = 1) {
            JoeNetworkClient["identifyInternal"](any<Identity>(), any<String>(), any<Boolean>())
        }
    }

    @Test
    fun `Test concurrent configure calls share a failed identify request`() = runTest {
        mockIdentifyCall()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        stubBlockingIdentifyInternal(started, release, failure = IOException("offline"))

        supervisorScope {
            val callA = async {
                JoeNetworkClient.configure(preferencesContext(isDebuggable = false), "j_o_e", externalId = null)
            }
            advanceUntilIdle()
            started.await()
            val callB = async {
                JoeNetworkClient.configure(preferencesContext(isDebuggable = false), "j_o_e", externalId = null)
            }
            advanceUntilIdle()
            release.complete(Unit)

            assertFailsWith<IOException> { callA.await() }
            assertFailsWith<IOException> { callB.await() }
        }

        assertEquals(null, getApiKey())
        coVerify(exactly = 1) {
            JoeNetworkClient["identifyInternal"](any<Identity>(), any<String>(), any<Boolean>())
        }
    }

    @Test
    fun `Test configure after logout stays a no-op`() = runTest {
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>()) } returns 0
        val identityGateway = InMemoryIdentityGateway(null)
        setIdentityManager(IdentityManager(identityGateway, SequentialIdFactory()))
        mockIdentifyCall()

        JoeNetworkClient.configure(preferencesContext(isDebuggable = false), "first-key", externalId = "A")
        JoeNetworkClient.logout()
        JoeNetworkClient.configure(preferencesContext(isDebuggable = false), "second-key", externalId = "B")

        assertEquals("first-key", getApiKey())
        assertEquals(Identity("anonymous-2", "device-2", null), JoeNetworkClient.currentIdentity)
        coVerify(exactly = 1) {
            JoeNetworkClient["identifyInternal"](any<Identity>(), any<String>(), any<Boolean>())
        }
    }

    @Test
    fun `Test configure can be retried after a failed attempt`() = runTest {
        mockIdentifyCall(failure = IOException("offline"))

        assertFailsWith<IOException> {
            JoeNetworkClient.configure(preferencesContext(isDebuggable = false), "key-A", externalId = null)
        }
        assertEquals(null, getApiKey())

        stubIdentifyInternal()
        JoeNetworkClient.configure(preferencesContext(isDebuggable = false), "key-B", externalId = null)

        assertEquals("key-B", getApiKey())
        coVerify(exactly = 2) {
            JoeNetworkClient["identifyInternal"](any<Identity>(), any<String>(), any<Boolean>())
        }
    }

    @Test
    fun `Test identify commits identity after backend success`() = runTest {
        val identityGateway = InMemoryIdentityGateway(Identity("anonymous-X", "device-X", "external-A"))
        setIdentityManager(IdentityManager(identityGateway, SequentialIdFactory()))
        mockIdentifyCall()

        JoeNetworkClient.identify("B")

        val expectedIdentity = Identity("anonymous-1", "device-1", "external-B")
        coVerify {
            JoeNetworkClient["identifyInternal"](expectedIdentity, any<String>(), any<Boolean>())
        }
        assertEquals(expectedIdentity, JoeNetworkClient.currentIdentity)
        assertEquals(expectedIdentity, identityGateway.identity)
    }

    @Test
    fun `Test failed switch keeps the fresh anonymous identity`() = runTest {
        val identityGateway = InMemoryIdentityGateway(Identity("anonymous-X", "device-X", "external-A"))
        setIdentityManager(IdentityManager(identityGateway, SequentialIdFactory()))
        mockIdentifyCall(failure = IOException("offline"))

        assertFailsWith<IOException> { JoeNetworkClient.identify("B") }

        val anonymousIdentity = Identity("anonymous-1", "device-1")
        assertEquals(anonymousIdentity, JoeNetworkClient.currentIdentity)
        assertEquals(anonymousIdentity, identityGateway.identity)
    }

    @Test
    fun `Test failed identify keeps the previous identity`() = runTest {
        val previousIdentity = Identity("anonymous-X", "device-X")
        val identityGateway = InMemoryIdentityGateway(previousIdentity)
        setIdentityManager(IdentityManager(identityGateway, SequentialIdFactory()))
        mockIdentifyCall()
        JoeNetworkClient.identify(null)
        stubIdentifyInternal(failure = IOException("offline"))

        assertFailsWith<IOException> { JoeNetworkClient.identify("A") }

        assertEquals(previousIdentity, JoeNetworkClient.currentIdentity)
        assertEquals(previousIdentity, identityGateway.identity)
    }

    @Test
    fun `Test configure without ID keeps identified account`() = runTest {
        val accountA = Identity("anonymous-X", "device-X", "external-A")
        val identityGateway = InMemoryIdentityGateway(accountA)
        setIdentityManager(IdentityManager(identityGateway, SequentialIdFactory()))
        mockIdentifyCall()

        JoeNetworkClient.configure(preferencesContext(isDebuggable = false), "j_o_e", externalId = null)

        coVerify { JoeNetworkClient["identifyInternal"](accountA, any<String>(), any<Boolean>()) }
        assertEquals(accountA, JoeNetworkClient.currentIdentity)
        assertEquals(accountA, identityGateway.identity)
        assertEquals(0, identityGateway.deleteCount)

        JoeNetworkClient.identify("A")

        assertEquals(accountA, JoeNetworkClient.currentIdentity)
        assertEquals(0, identityGateway.deleteCount)
    }

    @Test
    fun `Test configure without ID then different account logs out first`() = runTest {
        val identityGateway = InMemoryIdentityGateway(Identity("anonymous-X", "device-X", "external-A"))
        setIdentityManager(IdentityManager(identityGateway, SequentialIdFactory()))
        mockIdentifyCall()
        JoeNetworkClient.configure(preferencesContext(isDebuggable = false), "j_o_e", externalId = null)

        JoeNetworkClient.identify("B")

        val expectedIdentity = Identity("anonymous-1", "device-1", "external-B")
        coVerify { JoeNetworkClient["identifyInternal"](expectedIdentity, any<String>(), any<Boolean>()) }
        assertEquals(expectedIdentity, JoeNetworkClient.currentIdentity)
        assertEquals(1, identityGateway.deleteCount)
    }

    @Test
    fun `Test configure with blank ID fails before any setup`() = runTest {
        val accountA = Identity("anonymous-X", "device-X", "external-A")
        val identityGateway = InMemoryIdentityGateway(accountA)
        setIdentityManager(IdentityManager(identityGateway, SequentialIdFactory()))
        mockIdentifyCall()

        assertFailsWith<IllegalArgumentException> {
            JoeNetworkClient.configure(preferencesContext(isDebuggable = false), "j_o_e", externalId = " ")
        }

        verify(exactly = 0) { SystemInfo.of(any()) }
        coVerify(exactly = 0) { JoeNetworkClient["identifyInternal"](any<Identity>(), any<String>(), any<Boolean>()) }
        assertEquals(accountA, identityGateway.identity)
        assertEquals(0, identityGateway.saveCount)
        assertEquals(0, identityGateway.deleteCount)
    }

    @Test
    fun `Test getFaqs throws JoeHttpException for a 401 response`() = runTest {
        setIdentityManager(configuredIdentityManager())
        setApiKey("test-key")
        setHttpClient(JoeNetworkClient.buildJoeHttpClient(MockEngine { respondError(HttpStatusCode.Unauthorized) }))

        val exception = assertFailsWith<JoeHttpException> { JoeNetworkClient.getFaqs() }

        assertEquals(401, exception.statusCode)
        assertEquals("GET", exception.method)
        assertEquals("/v1/faqs", exception.path)
    }

    @Test
    fun `Test getFaqs throws JoeHttpException for a 403 response`() = runTest {
        setIdentityManager(configuredIdentityManager())
        setApiKey("test-key")
        setHttpClient(JoeNetworkClient.buildJoeHttpClient(MockEngine { respondError(HttpStatusCode.Forbidden) }))

        val exception = assertFailsWith<JoeHttpException> { JoeNetworkClient.getFaqs() }

        assertEquals(403, exception.statusCode)
        assertEquals("GET", exception.method)
        assertEquals("/v1/faqs", exception.path)
    }

    @Test
    fun `Test getFaq throws JoeHttpException for a 404 response`() = runTest {
        setIdentityManager(configuredIdentityManager())
        setApiKey("test-key")
        setHttpClient(JoeNetworkClient.buildJoeHttpClient(MockEngine { respondError(HttpStatusCode.NotFound) }))

        val exception = assertFailsWith<JoeHttpException> { JoeNetworkClient.getFaq(faqId = 42) }

        assertEquals(404, exception.statusCode)
        assertEquals("GET", exception.method)
        assertEquals("/v1/faqs/42", exception.path)
    }

    @Test
    fun `Test identify throws JoeHttpException for a non-2xx response`() = runTest {
        setIdentityManager(IdentityManager(InMemoryIdentityGateway(Identity("anonymous-X", "device-X")), SequentialIdFactory()))
        setApiKey("test-key")
        setHttpClient(JoeNetworkClient.buildJoeHttpClient(MockEngine { respondError(HttpStatusCode.Forbidden) }))

        val exception = assertFailsWith<JoeHttpException> { JoeNetworkClient.identify("A") }

        assertEquals(403, exception.statusCode)
        assertEquals("POST", exception.method)
        assertEquals("/v1/identify", exception.path)
    }

    @Test
    fun `Test configure sends a Preferred-Languages header matching the locale extension output`() = runTest {
        val identityGateway = InMemoryIdentityGateway(null)
        setIdentityManager(IdentityManager(identityGateway, SequentialIdFactory()))
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
        every { any<LocaleList>().toRegionalLanguageTags() } returns listOf("de-DE", "en-US", "fr-FR")

        var languageCodeHeader: String? = null
        setHttpClient(JoeNetworkClient.buildJoeHttpClient(MockEngine { request ->
            languageCodeHeader = request.headers["Preferred-Languages"]
            respond(content = "", status = HttpStatusCode.OK)
        }))

        JoeNetworkClient.configure(preferencesContext(isDebuggable = false), "j_o_e", externalId = null)

        assertEquals("de-DE,en-US,fr-FR", languageCodeHeader)
    }

    @Test
    fun `Test configure omits the Preferred-Languages header when no locale is valid`() = runTest {
        val identityGateway = InMemoryIdentityGateway(null)
        setIdentityManager(IdentityManager(identityGateway, SequentialIdFactory()))
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

        var languageCodeHeader: String? = null
        setHttpClient(JoeNetworkClient.buildJoeHttpClient(MockEngine { request ->
            languageCodeHeader = request.headers["Preferred-Languages"]
            respond(content = "", status = HttpStatusCode.OK)
        }))

        JoeNetworkClient.configure(preferencesContext(isDebuggable = false), "j_o_e", externalId = null)

        assertEquals(null, languageCodeHeader)
    }

    @Test
    fun `Test getFaqs network failure stays IOException`() = runTest {
        setIdentityManager(configuredIdentityManager())
        setApiKey("test-key")
        setHttpClient(JoeNetworkClient.buildJoeHttpClient(MockEngine { throw IOException("offline") }))

        assertFailsWith<IOException> { JoeNetworkClient.getFaqs() }
    }

    /**
     * An [IdentityManager] with an already-committed identity, for tests that exercise a content
     * request directly without going through [JoeNetworkClient.configure]/`identify` first.
     */
    private fun configuredIdentityManager(): IdentityManager {
        val identity = Identity("anonymous-X", "device-X")
        return IdentityManager(InMemoryIdentityGateway(identity), SequentialIdFactory()).apply {
            commit(identity)
        }
    }

    private fun setIdentityManager(identityManager: IdentityManager?) {
        JoeNetworkClient::class.java.getDeclaredField("identityManager").apply {
            isAccessible = true
            set(JoeNetworkClient, identityManager)
        }
    }

    private fun setApiKey(apiKey: String?) {
        JoeNetworkClient::class.java.getDeclaredField("apiKey").apply {
            isAccessible = true
            set(JoeNetworkClient, apiKey)
        }
    }

    private fun getApiKey(): String? {
        return JoeNetworkClient::class.java.getDeclaredField("apiKey").apply {
            isAccessible = true
        }.get(JoeNetworkClient) as String?
    }

    private fun setSystemInfo(systemInfo: SystemInfo?) {
        JoeNetworkClient::class.java.getDeclaredField("systemInfo").apply {
            isAccessible = true
            set(JoeNetworkClient, systemInfo)
        }
    }

    private fun getHttpClient(): HttpClient {
        return JoeNetworkClient::class.java.getDeclaredField("httpClient").apply {
            isAccessible = true
        }.get(JoeNetworkClient) as HttpClient
    }

    private fun setHttpClient(httpClient: HttpClient) {
        JoeNetworkClient::class.java.getDeclaredField("httpClient").apply {
            isAccessible = true
            set(JoeNetworkClient, httpClient)
        }
    }

    private fun mockIdentifyCall(failure: Throwable? = null) {
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

        mockkObject(JoeNetworkClient, recordPrivateCalls = true)
        stubIdentifyInternal(failure)
    }

    private fun stubIdentifyInternal(failure: Throwable? = null) {
        val identifyCall = coEvery {
            JoeNetworkClient["identifyInternal"](any<Identity>(), any<String>(), any<Boolean>())
        }
        if (failure == null) identifyCall returns Unit else identifyCall throws failure
    }

    /**
     * Stubs `identifyInternal` to suspend until [release] completes, signalling [started] once
     * entered, so a test can prove two [JoeNetworkClient.configure] calls genuinely overlapped
     * instead of running back-to-back.
     */
    private fun stubBlockingIdentifyInternal(
        started: CompletableDeferred<Unit>,
        release: CompletableDeferred<Unit>,
        failure: Throwable? = null,
    ) {
        coEvery {
            JoeNetworkClient["identifyInternal"](any<Identity>(), any<String>(), any<Boolean>())
        } coAnswers {
            started.complete(Unit)
            release.await()
            if (failure != null) throw failure
        }
    }

    private fun preferencesContext(isDebuggable: Boolean): Context {
        val editor = mockk<SharedPreferences.Editor>(relaxed = true) {
            every { commit() } returns true
        }
        val sharedPreferences = mockk<SharedPreferences>(relaxed = true) {
            every { edit() } returns editor
        }

        return mockk {
            every { applicationInfo } returns ApplicationInfo().apply {
                flags = if (isDebuggable) ApplicationInfo.FLAG_DEBUGGABLE else 0
            }
            every { getSharedPreferences(any(), any()) } returns sharedPreferences
        }
    }
}
