// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.faq

import dev.mobilejoe.api.network.JoeNetworkClient
import dev.mobilejoe.api.network.NetworkClient

/**
 * [FaqsGateway] backed directly by [NetworkClient]'s FAQ endpoints, with no additional caching
 * or throttling.
 *
 * @param networkClient The network client used to fetch FAQs. Defaults to the shared
 * [JoeNetworkClient] singleton.
 */
internal class RemoteFaqsGateway(
    private val networkClient: NetworkClient = JoeNetworkClient
) : FaqsGateway {

    /**
     * Fetches the full FAQ list over the network, with no local caching or pagination.
     *
     * @return The full list of FAQs from [NetworkClient.getFaqs].
     * @throws Exception if the underlying request fails.
     */
    override suspend fun load(): List<Faq> {
        return networkClient.getFaqs()
    }

    /**
     * Fetches a single FAQ's detail over the network.
     *
     * @param id The identifier of the FAQ to fetch.
     * @return The requested FAQ.
     * @throws Exception if the underlying request fails.
     */
    override suspend fun load(id: Int): Faq {
        return networkClient.getFaq(faqId = id)
    }
}
