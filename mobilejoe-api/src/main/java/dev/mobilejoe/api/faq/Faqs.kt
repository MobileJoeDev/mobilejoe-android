// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.faq

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Public entry point for observing and loading FAQs.
 *
 * Wraps a [FaqsGateway] and exposes both the full FAQ list and a single selected FAQ as hot
 * [Flow]s, letting multiple UI observers share the same underlying state.
 *
 * Use [load] to fetch the full list, and the id-taking overload of
 * [load] to fetch a single FAQ's detail into [single].
 *
 * @param faqsGateway The gateway used to fetch FAQs. Defaults to a [RemoteFaqsGateway] backed by
 * the real network client; callers wire in a fake/mock for tests.
 */
class Faqs(
    private val faqsGateway: FaqsGateway = RemoteFaqsGateway()
) {

    /** Backing store for [all]; `null` until the first call to [load]. */
    private val _all = MutableStateFlow<List<Faq>?>(null)

    /** The current FAQ list, or `null` before it has been loaded for the first time. */
    val all: Flow<List<Faq>?> = _all

    /** Backing store for [single]; `null` until [load] is called with an id. */
    private val _single = MutableStateFlow<Faq?>(null)

    /** The FAQ most recently requested via [load], or `null` if none has been loaded yet. */
    val single: Flow<Faq?> = _single

    /**
     * Fetches FAQs from [faqsGateway] and publishes them to [all]. This will fetch the whole
     * list. FAQs don't provide a pagination.
     *
     * @throws Exception if the underlying gateway request fails. On failure, [all] is left
     * unchanged so any previously loaded FAQs remain visible to observers.
     */
    suspend fun load() {
        val faqs = faqsGateway.load()
        _all.emit(faqs)
    }

    /**
     * Loads a single FAQ by [id] from [faqsGateway] and publishes it to [single].
     *
     * [single] is cleared to `null` before the request starts, so observers see a brief
     * "not loaded yet" state while the new FAQ is being fetched.
     *
     * @param id The identifier of the FAQ to load.
     * @throws Exception if the underlying gateway request fails. On failure, [single] is left at
     * `null` (its value was already cleared before the request started).
     */
    suspend fun load(id: Int) {
        _single.emit(null)

        val feature = faqsGateway.load(id)
        _single.emit(feature)
    }

    companion object {

        fun preview(): Faqs = Faqs(InMemoryFaqsGateway(InMemoryFaqsGateway.seedData))

        fun previewEmpty(): Faqs = Faqs(InMemoryFaqsGateway())

        fun previewFailed(): Faqs =
            Faqs(InMemoryFaqsGateway(loadError = IllegalStateException("Failed loading")))
    }
}
