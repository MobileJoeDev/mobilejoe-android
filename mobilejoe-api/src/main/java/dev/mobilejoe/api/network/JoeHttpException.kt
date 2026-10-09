// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.network

/**
 * Thrown when the MobileJoe backend responds with a non-2xx status, for `identify` and every
 * content request.
 *
 * Carries only what's needed to tell failure modes apart (e.g. [statusCode] `== 404` for a
 * deleted/renamed resource, `403` for an expired trial, `429` for the vote rate limit). There is
 * no numeric error code and no localized message of its own; UI copy belongs in string resources,
 * keyed off [statusCode]. A network-level failure (no response at all) throws [java.io.IOException]
 * instead.
 *
 * @param statusCode The HTTP status code of the failed response (e.g. 404, 403, 429, 500).
 * @param method The HTTP method of the failed request (e.g. `"GET"`, `"POST"`).
 * @param path The request path that failed (e.g. `"/v1/faqs/123"`).
 */
class JoeHttpException(
    val statusCode: Int,
    val method: String,
    val path: String,
    cause: Throwable?,
) : RuntimeException("HTTP request responded with status $statusCode for $method $path", cause)
