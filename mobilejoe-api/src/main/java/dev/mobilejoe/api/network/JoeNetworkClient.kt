// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.network

import android.content.Context
import android.util.Log
import dev.mobilejoe.api.alert.Alert
import dev.mobilejoe.api.faq.Faq
import dev.mobilejoe.api.feature.FeaturePlan
import dev.mobilejoe.api.identity.Identity
import dev.mobilejoe.api.identity.IdentityManager
import dev.mobilejoe.api.identity.PreferencesIdentityGateway
import dev.mobilejoe.api.identity.requireValidAppUserId
import dev.mobilejoe.api.identity.util.JoeIdFactory
import dev.mobilejoe.api.network.JoeNetworkClient.apiKey
import dev.mobilejoe.api.network.JoeNetworkClient.appendCommonHeaders
import dev.mobilejoe.api.network.JoeNetworkClient.configure
import dev.mobilejoe.api.network.JoeNetworkClient.identify
import dev.mobilejoe.api.network.JoeNetworkClient.identityManager
import dev.mobilejoe.api.network.JoeNetworkClient.identityMutex
import dev.mobilejoe.api.network.JoeNetworkClient.isDebugMode
import dev.mobilejoe.api.network.JoeNetworkClient.logout
import dev.mobilejoe.api.network.JoeNetworkClient.systemInfo
import dev.mobilejoe.api.system.SystemInfo
import dev.mobilejoe.api.system.isDebuggable
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.http.HeadersBuilder
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import dev.mobilejoe.api.json as jsonConfig

/** Host of the MobileJoe backend API. */
internal val apiHost = "mbj-api.com"

/** API version path segment used to build every request URL. */
internal val apiVersion = "v1"

/** Default `feature_plans` list endpoint URL, used to start a fresh pagination sequence. */
internal val featurePlansUrl = "https://$apiHost/$apiVersion/feature_plans"

/**
 * Default [NetworkClient] implementation backed by Ktor, backing [dev.mobilejoe.api.Joe].
 *
 * As a singleton `object`, this instance holds the SDK's entire network/identity state
 * ([identityManager], [apiKey], [systemInfo], [isDebugMode]) for the lifetime of the process.
 * [configure] must be called (and succeed) before any of the `get`/`post`-prefixed endpoint
 * functions are used, since every one of them calls [appendCommonHeaders], which requires
 * [apiKey] and an established identity to be present.
 */
internal object JoeNetworkClient : NetworkClient {

    /** The underlying HTTP client used for every request; see [buildJoeHttpClient]. */
    private var httpClient = buildJoeHttpClient(Android.create())

    /** Owns [Identity] creation/persistence; `null` until [configure] has been called once. */
    private var identityManager: IdentityManager? = null

    /**
     * The API key set by [configure]; `null` until configuration has succeeded.
     *
     * `@Volatile` because it's written under [identityMutex] but read under [configurationMutex]
     * in [configure] — the two locks don't synchronize with each other, so the field needs its
     * own visibility guarantee.
     */
    @Volatile
    private var apiKey: String? = null

    /** Device/app metadata sent with every request; `null` until [configure] has been called. */
    private var systemInfo: SystemInfo? = null

    /** Backing field for [isDebugMode]. */
    private var _isDebugMode: Boolean = false

    /** @see NetworkClient.isDebugMode */
    override val isDebugMode: Boolean
        get() = _isDebugMode

    /** The identity currently in effect, or `null` if [identityManager] hasn't been set up yet. */
    val currentIdentity: Identity?
        get() = requireIdentityManager().currentIdentity

    /**
     * Serializes [configure], [identify], and [logout] so identity resolution and commit can't
     * interleave across concurrent calls.
     */
    private val identityMutex = Mutex()

    /**
     * Guards [configure] to ensure only one configuration attempt is in-flight at a time, and that
     * the first successful one wins.
     */
    private val configurationMutex = Mutex()

    /** The shared in-flight (or just-finished) configuration attempt, if any; see [configure]. */
    private var configurationDeferred: Deferred<Unit>? = null

    /** Owns [configurationDeferred] so it outlives any single caller's own coroutine/dispatcher. */
    private val configureScope = CoroutineScope(SupervisorJob())

    /**
     * Sets up [systemInfo] and [identityManager] for [applicationContext], identifies the
     * device/user with the backend, and only then commits [apiKey] and [isDebugMode].
     *
     * A failed identify call leaves the client unconfigured (i.e. [apiKey]/[isDebugMode] are not
     * updated) rather than partially configured.
     *
     * Safe to call more than once: it only takes effect the first time, and calls made at the
     * same time simply wait for one another instead of running at once.
     *
     * @param applicationContext The host app's context.
     * @param apiKey The API key issued for the host app.
     * @param externalId The host app's own user identifier, or `null` to keep the persisted
     * identity (anonymous on first launch).
     * @param isDebugMode Whether requests should be tagged as debug traffic.
     * @throws IllegalArgumentException if [externalId] is empty or blank; nothing is set up then.
     * @throws JoeHttpException if identification responds with a non-2xx status.
     * @throws java.io.IOException if identification fails due to a network problem.
     */
    suspend fun configure(
        applicationContext: Context,
        apiKey: String,
        externalId: String?,
        isDebugMode: Boolean = false
    ) {
        val task = configurationMutex.withLock {
            if (this.apiKey != null) {
                Log.w(
                    "Joe", "🦫 Joe says: SDK already configured. Nothing changed. " +
                            "Use `identify` to set a new `appUserId`."
                )
                return
            }

            // CoroutineStart.LAZY so the attempt can't start running (and try to acquire
            // identityMutex) before this withLock block returns and releases configurationMutex.
            configurationDeferred ?: configureScope.async(start = CoroutineStart.LAZY) {
                identityMutex.withLock {
                    try {
                        requireValidAppUserId(externalId)

                        configureSystemInfo(applicationContext)
                        configureIdentityManager(applicationContext)

                        identifyAndCommit(
                            externalId = externalId,
                            apiKey = apiKey,
                            isDebugMode = isDebugMode
                        )

                        this@JoeNetworkClient.apiKey = apiKey
                        this@JoeNetworkClient._isDebugMode = isDebugMode

                        if (applicationContext.isDebuggable) {
                            Log.d("Joe", "🦫 Joe says: SDK configured successfully.")
                        }
                    } finally {
                        configurationMutex.withLock {
                            configurationDeferred = null
                        }
                    }
                }
            }.also { configurationDeferred = it }
        }

        // Wait for shared or just-created one in-flight runs across concurrent callers.
        task.await()
    }

    /**
     * Populates [systemInfo] from [applicationContext].
     *
     * @param applicationContext The host app's context.
     */
    private fun configureSystemInfo(applicationContext: Context) {
        systemInfo = SystemInfo.of(applicationContext)
    }

    /**
     * Lazily creates [identityManager] for [applicationContext]; a no-op if it already exists,
     * so re-configuring doesn't discard in-memory identity state.
     *
     * @param applicationContext The host app's context, used to construct the preferences-backed
     * identity gateway.
     */
    private fun configureIdentityManager(applicationContext: Context) {
        if (identityManager != null) {
            return
        }

        val identityGateway = PreferencesIdentityGateway(applicationContext)
        val idFactory = JoeIdFactory
        this.identityManager = IdentityManager(identityGateway, idFactory)
    }

    /**
     * Re-identifies with [externalId] under [identityMutex], serializing this call against
     * [configure] and any other concurrent [identify]/[logout] call.
     *
     * @param externalId The host app's own user identifier, or `null` to keep the persisted
     * identity.
     * @throws IllegalArgumentException if [externalId] is empty or blank.
     * @throws JoeHttpException if the backend responds with a non-2xx status; the new account is
     * not applied locally.
     * @throws java.io.IOException if the request fails due to a network problem; the new account
     * is not applied locally.
     */
    override suspend fun identify(externalId: String?) = identityMutex.withLock {
        identifyAndCommit(externalId)
    }

    /**
     * Resolves the identity for [externalId], posts it to the backend, and commits it locally
     * only once the backend has accepted it.
     *
     * Switching from one external id to a different one logs out first, so on failure the
     * identity is the fresh anonymous one, never the previous account's. Otherwise a failed
     * request leaves the previous identity untouched. A `null` [externalId] keeps the persisted
     * identity, including its external id.
     *
     * @param externalId The host app's own user identifier, or `null` to keep the persisted
     * identity.
     * @param apiKey The API key to use for the identify request. Defaults to the currently
     * configured [apiKey].
     * @param isDebugMode Whether to tag the identify request as debug traffic. Defaults to the
     * currently configured [isDebugMode].
     * @throws IllegalArgumentException if [externalId] is empty or blank.
     * @throws Exception if the request fails.
     */
    private suspend fun identifyAndCommit(
        externalId: String?,
        apiKey: String? = this.apiKey,
        isDebugMode: Boolean = this.isDebugMode,
    ) {
        val identity = requireIdentityManager().prepareIdentity(externalId)
        identifyInternal(identity = identity, apiKey = apiKey, isDebugMode = isDebugMode)
        requireIdentityManager().commit(identity)
    }

    /**
     * Posts [identity] to the `identify` endpoint.
     *
     * @param identity The identity to identify with.
     * @param apiKey The API key to authenticate the request with. Defaults to the currently
     * configured [apiKey].
     * @param isDebugMode Whether to tag the request as debug traffic. Defaults to the currently
     * configured [isDebugMode].
     * @throws JoeHttpException if the backend responds with a non-2xx status.
     * @throws java.io.IOException if the request fails due to a network problem.
     */
    private suspend fun identifyInternal(
        identity: Identity,
        apiKey: String? = this.apiKey,
        isDebugMode: Boolean = this.isDebugMode,
    ) {
        httpClient.post {
            url {
                protocol = URLProtocol.HTTPS
                host = apiHost
                pathSegments = listOf(apiVersion, "identify")
            }
            headers {
                appendCommonHeaders(
                    this,
                    apiKey = apiKey,
                    isDebugMode = isDebugMode,
                    identity = identity,
                )
            }
        }
    }

    /**
     * Clears the current identity and establishes a fresh anonymous one, under [identityMutex].
     *
     * @throws Exception if the SDK has not been configured yet, i.e. [identityManager] is
     * `null`.
     */
    override suspend fun logout() {
        identityMutex.withLock {
            requireIdentityManager().logout()
        }
    }

    /**
     * Issues an HTTP `GET` for a page of feature plans, either against [requestUrl] directly
     * (when following a `next` link) or with [statuses]/[sorting]/[search] as query parameters
     * (when starting fresh).
     *
     * @param requestUrl The URL to request: either the default first-page URL, or a `next` link
     * from a previous [NetworkClient.Response].
     * @param statuses The backend status keys to filter by, or `null` for no filter.
     * @param sorting The backend sort key to apply, or `null` for the backend default.
     * @param search The free-text search query to apply, or `null` for none.
     * @return The requested page of feature plans, plus the `next` page link if any.
     * @throws IllegalStateException if the SDK has not been configured (no API key/identity).
     * @throws JoeHttpException if the backend responds with a non-2xx status.
     * @throws java.io.IOException if the request fails due to a network problem.
     * @throws Exception if the response body can't be decoded.
     */
    override suspend fun getFeaturePlans(
        requestUrl: String,
        statuses: List<String>?,
        sorting: String?,
        search: String?,
    ): NetworkClient.Response<List<FeaturePlan>> {
        val response = httpClient.get {
            url(requestUrl)
                .apply {
                    statuses?.forEach { status -> parameter("statuses[]", status) }
                    sorting?.let { sortng -> parameter("sort", sortng) }
                    search?.let { srch -> parameter("search", srch) }
                }

            headers { appendCommonHeaders(this) }
        }

        return NetworkClient.Response.of(response)
    }

    /**
     * Issues an HTTP `GET` for a single feature plan's detail.
     *
     * @param featurePlanId The identifier of the feature plan to fetch.
     * @return The requested feature plan.
     * @throws IllegalStateException if the SDK has not been configured (no API key/identity).
     * @throws JoeHttpException if the backend responds with a non-2xx status.
     * @throws java.io.IOException if the request fails due to a network problem.
     * @throws Exception if the response body can't be decoded.
     */
    override suspend fun getFeaturePlan(featurePlanId: Int): FeaturePlan {
        return httpClient.get {
            url {
                protocol = URLProtocol.HTTPS
                host = apiHost
                pathSegments = listOf(apiVersion, "feature_plans", featurePlanId.toString())
            }

            headers { appendCommonHeaders(this) }
        }.body()
    }

    /**
     * Issues an HTTP `POST` to cast a vote for a feature plan.
     *
     * @param featurePlanId The identifier of the feature plan to vote for.
     * @return The updated feature plan.
     * @throws IllegalStateException if the SDK has not been configured (no API key/identity).
     * @throws JoeHttpException if the backend responds with a non-2xx status.
     * @throws java.io.IOException if the request fails due to a network problem.
     * @throws Exception if the response body can't be decoded.
     */
    override suspend fun postVoteFeaturePlans(featurePlanId: Int): FeaturePlan {
        return httpClient.post {
            url {
                protocol = URLProtocol.HTTPS
                host = apiHost
                pathSegments =
                    listOf(apiVersion, "feature_plans", featurePlanId.toString(), "vote")
            }

            headers { appendCommonHeaders(this) }
        }.body()
    }

    /**
     * Issues an HTTP `GET` for the current alerts.
     *
     * @return The current alerts.
     * @throws IllegalStateException if the SDK has not been configured (no API key/identity).
     * @throws JoeHttpException if the backend responds with a non-2xx status.
     * @throws java.io.IOException if the request fails due to a network problem.
     * @throws Exception if the response body can't be decoded.
     */
    override suspend fun getAlerts(): List<Alert> {
        return httpClient.get {
            url {
                protocol = URLProtocol.HTTPS
                host = apiHost
                pathSegments = listOf(apiVersion, "alerts")
            }

            headers { appendCommonHeaders(this) }
        }.body()
    }

    /**
     * Issues an HTTP `GET` for the full FAQ list.
     *
     * @return The current FAQs.
     * @throws IllegalStateException if the SDK has not been configured (no API key/identity).
     * @throws JoeHttpException if the backend responds with a non-2xx status.
     * @throws java.io.IOException if the request fails due to a network problem.
     * @throws Exception if the response body can't be decoded.
     */
    override suspend fun getFaqs(): List<Faq> {
        return httpClient.get {
            url {
                protocol = URLProtocol.HTTPS
                host = apiHost
                pathSegments = listOf(apiVersion, "faqs")
            }

            headers { appendCommonHeaders(this) }
        }.body()
    }

    /**
     * Issues an HTTP `GET` for a single FAQ's detail.
     *
     * @param faqId The identifier of the FAQ to fetch.
     * @return The requested FAQ.
     * @throws IllegalStateException if the SDK has not been configured (no API key/identity).
     * @throws JoeHttpException if the backend responds with a non-2xx status.
     * @throws java.io.IOException if the request fails due to a network problem.
     * @throws Exception if the response body can't be decoded.
     */
    override suspend fun getFaq(faqId: Int): Faq {
        return httpClient.get {
            url {
                protocol = URLProtocol.HTTPS
                host = apiHost
                pathSegments = listOf(apiVersion, "faqs", faqId.toString())
            }

            headers { appendCommonHeaders(this) }
        }.body()
    }

    /**
     * Appends the headers every authenticated request needs: bearer auth, identity ids, debug
     * mode flag, and device/app metadata.
     *
     * @param headers The header builder to append to.
     * @param apiKey The API key to authenticate with. Defaults to the currently configured
     * [apiKey].
     * @param isDebugMode Whether to tag the request as debug traffic. Defaults to the currently
     * configured [isDebugMode].
     * @param identity The identity to send. Defaults to the current identity.
     * @throws IllegalStateException if [apiKey] is `null` (SDK not configured) or if there is no
     * [identity] set.
     */
    private fun appendCommonHeaders(
        headers: HeadersBuilder,
        apiKey: String? = this.apiKey,
        isDebugMode: Boolean = this.isDebugMode,
        identity: Identity? = requireIdentityManager().currentIdentity,
    ) {
        checkNotNull(apiKey) {
            "API key is not set. Please call `MobileJoe.configure(...)` before making any call."
        }
        headers.append("Authorization", "Bearer $apiKey")

        checkNotNull(identity) {
            "Identity is not set. Please call `MobileJoe.configure(...)` before making any call."
        }
        headers.append("Identity-Anonymous-ID", identity.anonymousId)

        identity.externalId?.let { externalId ->
            headers.append("Identity-External-ID", externalId)
        }

        headers.append("Identity-Device-ID", identity.deviceId)

        headers.append("Debug-Mode", "$isDebugMode")

        headers.append("OS-Name", "Android")
        systemInfo?.let { deviceInfo ->
            headers.append("App-Build-Version", deviceInfo.clientVersionCode.toString())
            headers.append("App-Version", deviceInfo.clientVersionName)
            headers.append("Device-Model", deviceInfo.model)
            headers.append("Device-Brand", deviceInfo.brand)
            headers.append("OS-Version", deviceInfo.platformVersion.toString())
            headers.append("Framework-Version", deviceInfo.version)

            deviceInfo.locales
                .toRegionalLanguageTags()
                .takeIf(List<*>::isNotEmpty)
                ?.joinToString(",")
                ?.let { languageTags ->
                    headers.append("Preferred-Languages", languageTags)
                }
        }
    }

    /**
     * Asserts [identityManager] has already been set up by [configure], for call sites that
     * cannot proceed without an identity.
     *
     * @return [identityManager], asserted non-null.
     * @throws IllegalStateException if [configure] has not been called yet.
     */
    private fun requireIdentityManager() = checkNotNull(identityManager) {
        "Identity manager is not initialized. Please call `MobileJoe.configure(...)` before making any call."
    }

    /**
     * Builds the Ktor [HttpClient] used for every request: JSON (de)serialization, and mapping
     * any non-2xx response into a [JoeHttpException] rather than a misleading JSON-decoding
     * failure.
     *
     * An `internal` member (rather than inlined into [httpClient]'s field initializer) so a test
     * can build an equivalent client against a different [engine] (e.g. Ktor's `MockEngine`)
     * without re-implementing this configuration.
     *
     * @param engine The HTTP engine to run requests through.
     */
    internal fun buildJoeHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
        expectSuccess = true

        install(ContentNegotiation) {
            json(jsonConfig)
        }

        HttpResponseValidator {
            handleResponseExceptionWithRequest { cause, request ->
                when (cause) {
                    is ResponseException ->
                        throw JoeHttpException(
                            statusCode = cause.response.status.value,
                            method = request.method.value,
                            path = request.url.encodedPath,
                            cause = cause,
                        )

                    else ->
                        throw cause
                }
            }
        }
    }
}
