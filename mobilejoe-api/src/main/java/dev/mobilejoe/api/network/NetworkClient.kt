// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.network

import dev.mobilejoe.api.alert.Alert
import dev.mobilejoe.api.faq.Faq
import dev.mobilejoe.api.feature.FeaturePlan
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse

/**
 * Low-level HTTP contract for talking to the MobileJoe backend and managing identity/config.
 *
 * This is the single seam between the rest of the SDK and the network: [Joe][dev.mobilejoe.api.Joe]
 * and the domain gateways (`RemoteAlertsGateway`, `RemoteFaqsGateway`, `RemoteFeaturePlansGateway`)
 * only ever talk to a [NetworkClient], never to Ktor directly, so a fake implementation is enough
 * to test any of them in isolation. See [JoeNetworkClient] for the default implementation.
 */
interface NetworkClient {

    /** Whether this client is currently configured for debug-mode requests. */
    val isDebugMode: Boolean

    /**
     * Switches to the identity for [externalId] right away and sends it to the backend in the
     * background. Switching from a different external id logs out first.
     *
     * @param externalId The host app's own user identifier, or `null` to keep the persisted
     * identity.
     * @throws IllegalArgumentException if [externalId] is empty or blank.
     * @throws IllegalStateException if the client is not configured.
     * @throws dev.mobilejoe.api.identity.JoeStorageException if the identity could not be
     * persisted.
     */
    fun identify(externalId: String?)

    /**
     * Switches to a fresh, anonymous identity right away and sends it to the backend in the
     * background.
     *
     * @throws IllegalStateException if the client is not configured.
     * @throws dev.mobilejoe.api.identity.JoeStorageException if the identity could not be deleted
     * or persisted.
     */
    fun logout()

    /**
     * Fetches a page of feature plans.
     *
     * @param requestUrl The URL to request: either the default first-page URL, or a `next` link
     * from a previous [Response].
     * @param statuses The backend status keys to filter by, or `null` for no filter.
     * @param sorting The backend sort key to apply, or `null` for the backend default.
     * @param search The free-text search query to apply, or `null` for none.
     * @return The requested page of feature plans, plus the `next` page link if any.
     * @throws JoeHttpException if the backend responds with a non-2xx status.
     * @throws java.io.IOException if the request fails due to a network problem.
     */
    suspend fun getFeaturePlans(
        requestUrl: String = featurePlansUrl,
        statuses: List<String>? = null,
        sorting: String? = null,
        search: String? = null,
    ): Response<List<FeaturePlan>>

    /**
     * Fetches a single feature plan.
     *
     * @param featurePlanId The identifier of the feature plan to fetch.
     * @return The requested feature plan.
     * @throws JoeHttpException if the backend responds with a non-2xx status (e.g. `404` if the
     * feature plan no longer exists).
     * @throws java.io.IOException if the request fails due to a network problem.
     */
    suspend fun getFeaturePlan(featurePlanId: Int): FeaturePlan

    /**
     * Casts a vote for a feature plan.
     *
     * @param featurePlanId The identifier of the feature plan to vote for.
     * @return The updated feature plan.
     * @throws JoeHttpException if the backend responds with a non-2xx status (e.g. `429` if the
     * vote rate limit was exceeded).
     * @throws java.io.IOException if the request fails due to a network problem.
     */
    suspend fun postVoteFeaturePlans(featurePlanId: Int): FeaturePlan

    /**
     * Fetches the current list of alerts.
     *
     * @return The current alerts.
     * @throws JoeHttpException if the backend responds with a non-2xx status.
     * @throws java.io.IOException if the request fails due to a network problem.
     */
    suspend fun getAlerts(): List<Alert>

    /**
     * Fetches the full list of FAQs.
     *
     * @return The current FAQs.
     * @throws JoeHttpException if the backend responds with a non-2xx status.
     * @throws java.io.IOException if the request fails due to a network problem.
     */
    suspend fun getFaqs(): List<Faq>

    /**
     * Fetches a single FAQ.
     *
     * @param faqId The identifier of the FAQ to fetch.
     * @return The requested FAQ.
     * @throws JoeHttpException if the backend responds with a non-2xx status (e.g. `404` if the
     * FAQ no longer exists).
     * @throws java.io.IOException if the request fails due to a network problem.
     */
    suspend fun getFaq(faqId: Int): Faq

    /**
     * A decoded API response body, together with pagination metadata parsed from its headers.
     *
     * @param T The type of the decoded response body.
     * @param data The decoded response body.
     * @param nextUrl The URL of the next page, parsed from a `Link: <url>; rel="next"` response
     * header, or `null` if there is no further page.
     */
    data class Response<T>(
        val data: T,
        val nextUrl: String? = null
    ) {
        companion object {
            /**
             * Decodes [httpResponse]'s body as [T] and extracts its `next` pagination link (if
             * any) from the `Link` response header.
             *
             * @param T The type to decode the response body as.
             * @param httpResponse The raw HTTP response to parse.
             * @return A [Response] wrapping the decoded body and, if present, the `next` link.
             * @throws Exception if the body cannot be decoded as [T].
             */
            suspend inline fun <reified T> of(httpResponse: HttpResponse): Response<T> {
                val links = httpResponse.headers.getAll("link")
                    .orEmpty()
                    .joinToString(",")
                    .split(",")
                    .map { it.trim() }
                    .mapNotNull { link ->
                        link.substringBefore(";", "")
                            .removeSurrounding("<", ">")
                            .takeIf(String::isNotEmpty)
                            ?.let { url ->
                                Link(
                                    url = url,
                                    rel = link.substringAfter("rel=\"").substringBefore("\"")
                                )
                            }
                    }

                return Response(
                    data = httpResponse.body<T>(),
                    nextUrl = links.firstOrNull { it.rel == "next" }?.url
                )
            }
        }
    }

    /**
     * A single entry parsed out of an HTTP `Link` response header.
     *
     * @param url The linked URL.
     * @param rel The link's `rel` attribute (e.g. `"next"`).
     */
    data class Link(
        val url: String,
        val rel: String,
    )
}
