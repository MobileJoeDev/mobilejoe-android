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
import dev.mobilejoe.api.identity.JoeStorageException
import dev.mobilejoe.api.identity.PreferencesIdentityGateway
import dev.mobilejoe.api.identity.util.JoeIdFactory
import dev.mobilejoe.api.network.JoeNetworkClient.apiKey
import dev.mobilejoe.api.network.JoeNetworkClient.appendCommonHeaders
import dev.mobilejoe.api.network.JoeNetworkClient.buildJoeHttpClient
import dev.mobilejoe.api.network.JoeNetworkClient.configure
import dev.mobilejoe.api.network.JoeNetworkClient.httpClient
import dev.mobilejoe.api.network.JoeNetworkClient.identityManager
import dev.mobilejoe.api.network.JoeNetworkClient.isDebugMode
import dev.mobilejoe.api.network.JoeNetworkClient.rejectApiKey
import dev.mobilejoe.api.network.JoeNetworkClient.scope
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
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
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
 * [configure] must be called before any of the `get`/`post`-prefixed endpoint functions are
 * used, since every one of them calls [appendCommonHeaders], which requires [apiKey] and an
 * identity. Both are available as soon as [configure] returns: the identity is sent to the
 * backend in the background, and no request waits for that.
 *
 * Safe to use from any thread. [configure] and [rejectApiKey] run under this object's monitor,
 * and the state they write is `@Volatile` for the requests reading it.
 */
internal object JoeNetworkClient : NetworkClient {

    /**
     * Runs the background `identify` requests and builds [httpClient], independent of any
     * caller's own coroutine. A `var` so a test can swap in its own scope.
     */
    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * The underlying HTTP client used for every request; see [buildJoeHttpClient].
     *
     * Built in [scope] on first use, so neither [configure] nor a content request started on the
     * main thread pays for it there: callers suspend until it is ready. A `var` so a test can
     * swap in a client backed by another engine.
     */
    private var httpClient: Deferred<HttpClient> = scope.async(start = CoroutineStart.LAZY) {
        buildJoeHttpClient(Android.create())
    }

    /** Owns [Identity] creation/persistence; `null` until [configure] has been called once. */
    @Volatile
    private var identityManager: IdentityManager? = null

    /** The API key set by [configure]; `null` until then. The first [configure] call wins. */
    @Volatile
    private var apiKey: String? = null

    /**
     * Whether the backend rejected [apiKey]; see [rejectApiKey].
     *
     * Never reset, on purpose: the first [configure] call wins, so this process can't get another
     * key, and retrying the rejected one only repeats the `401`. A valid key takes effect on the
     * next app start.
     */
    @Volatile
    private var isApiKeyRejected = false

    /** Device/app metadata sent with every request; `null` until [configure] has been called. */
    @Volatile
    private var systemInfo: SystemInfo? = null

    /** Backing field for [isDebugMode]. */
    @Volatile
    private var _isDebugMode: Boolean = false

    /** @see NetworkClient.isDebugMode */
    override val isDebugMode: Boolean
        get() = _isDebugMode

    /** The identity currently in effect, or `null` if a logout did not complete. */
    val currentIdentity: Identity?
        get() = requireIdentityManager().currentIdentity

    /**
     * Sets up [systemInfo] and [identityManager] for [applicationContext], switches to the
     * identity for [externalId], and commits [apiKey] and [isDebugMode]. Content requests work as
     * soon as this returns; the identity is sent to the backend in the background.
     *
     * Reads the persisted identity from disk, and writes it if it changed, on the calling thread.
     *
     * Never throws for a storage failure, since an unchecked exception here would crash the host
     * app at startup: the failure is logged and the client stays unconfigured, so requests fail
     * until a later call succeeds, at the latest on the next app start.
     *
     * Safe to call more than once: it only takes effect the first time it succeeds.
     *
     * @param applicationContext The host app's context.
     * @param apiKey The API key issued for the host app.
     * @param externalId The host app's own user identifier, or `null` to keep the persisted
     * identity (anonymous on first launch).
     * @param isDebugMode Whether requests should be tagged as debug traffic.
     * @throws IllegalArgumentException if [externalId] is empty or blank; nothing is set up then.
     */
    @Synchronized
    fun configure(
        applicationContext: Context,
        apiKey: String,
        externalId: String?,
        isDebugMode: Boolean = false
    ) {
        if (this.apiKey != null) {
            Log.w(
                "Joe", "🦫 Joe says: SDK already configured. Nothing changed. " +
                        "Use `identify` to set a new `appUserId`."
            )
            return
        }

        configureSystemInfo(applicationContext)
        val identity = try {
            configureIdentityManager(applicationContext)
            requireIdentityManager().identifyAtStart(externalId)
        } catch (e: JoeStorageException) {
            Log.e("Joe", "🦫 Joe says: Failed to store the identity. The SDK is not configured.", e)
            return
        }

        this._isDebugMode = isDebugMode
        this.apiKey = apiKey

        if (applicationContext.isDebuggable) {
            Log.d("Joe", "🦫 Joe says: SDK configured successfully.")
        }

        sendIdentityInBackground(identity)
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
     * @throws JoeStorageException if the identity storage can't be opened.
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
     * Switches to the identity for [externalId] right away and sends it to the backend in the
     * background. Sends nothing if the current identity already belongs to [externalId].
     *
     * @param externalId The host app's own user identifier, or `null` to keep the persisted
     * identity.
     * @throws IllegalArgumentException if [externalId] is empty or blank.
     * @throws IllegalStateException if [configure] has not been called yet.
     * @throws JoeStorageException if the identity could not be persisted.
     */
    override fun identify(externalId: String?) {
        requireIdentityManager().identify(externalId)?.let(::sendIdentityInBackground)
    }

    /**
     * Switches to a fresh anonymous identity right away and sends it to the backend in the
     * background.
     *
     * @throws IllegalStateException if [configure] has not been called yet.
     * @throws JoeStorageException if the identity could not be deleted or persisted.
     */
    override fun logout() {
        sendIdentityInBackground(requireIdentityManager().logout())
    }

    /**
     * Launches [sendIdentity] for [identity] in [scope] and returns right away.
     *
     * The request may start before or after the caller's next statement. An identity that was
     * replaced before its request is sent is skipped, including while the HTTP client is still
     * being built on first use. `configure` directly followed by `identify` therefore sends one
     * request in most cases, and two if the first one is already on its way. The backend handles
     * both.
     *
     * @param identity The identity to send.
     */
    private fun sendIdentityInBackground(identity: Identity) {
        scope.launch { sendIdentity(identity) }
    }

    /**
     * Posts [identity] to the `identify` endpoint, unless the SDK is not configured, the API key
     * was rejected, or [identity] is no longer the current one.
     *
     * Never throws for a failed request: the failure is logged, and the identity is sent again
     * on the next app start. Content requests keep working meanwhile, because the backend
     * resolves the app user from the identity headers.
     *
     * @param identity The identity to send.
     */
    private suspend fun sendIdentity(identity: Identity) {
        try {
            // Building the client on first use takes the longest, so check afterwards whether
            // the identity is still worth sending.
            val client = httpClient.await()
            val isEnabled = apiKey != null && !isApiKeyRejected
            if (!isEnabled || !requireIdentityManager().isCurrent(identity)) {
                return
            }

            client.post {
                url {
                    protocol = URLProtocol.HTTPS
                    host = apiHost
                    pathSegments = listOf(apiVersion, "identify")
                }

                headers { appendCommonHeaders(this, identity) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A rejected API key is already logged by rejectApiKey, whether this request or
            // another one in flight got the 401.
            if (isApiKeyRejected) {
                return
            }

            Log.w("Joe", "🦫 Joe says: Failed to identify. Retrying on next app start.", e)
        }
    }

    /**
     * Disables the SDK after the backend rejected [apiKey]: every later request fails before it
     * is sent. Only the first call logs, so requests already in flight don't repeat the message.
     */
    @Synchronized
    private fun rejectApiKey() {
        if (isApiKeyRejected) {
            return
        }

        isApiKeyRejected = true
        Log.e(
            "Joe", "🦫 Joe says: Invalid API key. " +
                    "The SDK is disabled until the app restarts with a valid key."
        )
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
        val response = httpClient.await().get {
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
        return httpClient.await().get {
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
        return httpClient.await().post {
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
        return httpClient.await().get {
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
        return httpClient.await().get {
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
        return httpClient.await().get {
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
     * Checks the API key before the identity, so a request without [configure] reports the
     * missing configuration.
     *
     * @param headers The header builder to append to.
     * @param identity The identity to send, or `null` for the current identity.
     * @throws IllegalStateException if the SDK is not configured, the API key was rejected, or
     * there is no identity.
     */
    private fun appendCommonHeaders(headers: HeadersBuilder, identity: Identity? = null) {
        val apiKey = checkNotNull(apiKey) { NOT_CONFIGURED_MESSAGE }
        check(!isApiKeyRejected) {
            "Invalid API key. The SDK is disabled until the app restarts with a valid key."
        }
        headers.append("Authorization", "Bearer $apiKey")

        val identity = checkNotNull(identity ?: identityManager?.currentIdentity) {
            "Identity is not set. A logout did not complete; call `Joe.logout()` again."
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
    private fun requireIdentityManager() = checkNotNull(identityManager) { NOT_CONFIGURED_MESSAGE }

    /**
     * Error message for every call that needs [configure] first. Also covers a [configure] that
     * logged a storage failure and left the SDK unconfigured.
     */
    private const val NOT_CONFIGURED_MESSAGE =
        "The SDK is not configured. Call `Joe.configure(...)` before making any call. " +
                "If you did, check Logcat for the tag `Joe`."

    /**
     * Builds the Ktor [HttpClient] used for every request: JSON (de)serialization, and mapping
     * any non-2xx response into a [JoeHttpException] rather than a misleading JSON-decoding
     * failure. A `401` becomes a [JoeInvalidApiKeyException] and disables the SDK, for `identify`
     * and content requests alike; see [rejectApiKey].
     *
     * An `internal` member (rather than inlined into [httpClient]'s initializer) so a test can
     * build an equivalent client against a different [engine] (e.g. Ktor's `MockEngine`) without
     * re-implementing this configuration.
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
                when {
                    cause !is ResponseException ->
                        throw cause

                    cause.response.status == HttpStatusCode.Unauthorized -> {
                        rejectApiKey()
                        throw JoeInvalidApiKeyException(
                            method = request.method.value,
                            path = request.url.encodedPath,
                            cause = cause,
                        )
                    }

                    else ->
                        throw JoeHttpException(
                            statusCode = cause.response.status.value,
                            method = request.method.value,
                            path = request.url.encodedPath,
                            cause = cause,
                        )
                }
            }
        }
    }
}
