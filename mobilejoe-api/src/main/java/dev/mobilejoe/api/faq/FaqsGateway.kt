// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.faq

/**
 * Fetches FAQs from wherever they're sourced from (typically the MobileJoe backend).
 *
 * [Faqs] delegates all of its actual data access to an instance of this interface, so the gateway
 * is the layer to swap out for tests (fakes/mocks) or for an alternate data source. See
 * [RemoteFaqsGateway] for the default implementation.
 */
interface FaqsGateway {

    /**
     * Returns the full list of FAQs.
     *
     * @return The current list of FAQs.
     * @throws Exception if the fetch fails.
     */
    suspend fun load(): List<Faq>

    /**
     * Returns a single FAQ by its identifier.
     *
     * @param id The identifier of the FAQ to fetch.
     * @return The requested FAQ.
     * @throws Exception if the fetch fails, including when no FAQ with the given [id] exists.
     */
    suspend fun load(id: Int): Faq
}
