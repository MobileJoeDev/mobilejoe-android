// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.feature

import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class InMemoryFeaturePlansGatewayTest {

    private val plans = listOf(
        FeaturePlan.empty().copy(
            id = 1,
            title = "Cloud Sync",
            score = 10,
            status = FeaturePlan.Status.PLANNED,
            createdAt = Instant.ofEpochSecond(100),
        ),
        FeaturePlan.empty().copy(
            id = 2,
            title = "Watch app",
            score = 30,
            status = FeaturePlan.Status.UNDER_REVIEW,
            createdAt = Instant.ofEpochSecond(300),
        ),
        FeaturePlan.empty().copy(
            id = 3,
            title = "Dark mode",
            score = 20,
            status = FeaturePlan.Status.COMPLETED,
            createdAt = Instant.ofEpochSecond(200),
        ),
    )

    private fun gateway() = InMemoryFeaturePlansGateway(InMemoryFeaturePlansGateway.Store(plans))

    @Test
    fun `Test load filters by status and sorts by newest`() = runTest {
        val result = gateway().load(
            FeaturePlans.StatusFilter.ALL,
            FeaturePlans.Sorting.BY_NEWEST,
            searchQuery = null,
        )

        assertEquals(listOf(2, 1), result.map { it.id })
    }

    @Test
    fun `Test load sorts by score`() = runTest {
        val result = gateway().load(
            FeaturePlans.StatusFilter.ALL,
            FeaturePlans.Sorting.BY_SCORE,
            searchQuery = null,
        )

        assertEquals(listOf(2, 1), result.map { it.id })
    }

    @Test
    fun `Test load searches titles case-insensitively`() = runTest {
        val result = gateway().load(
            FeaturePlans.StatusFilter.ALL,
            FeaturePlans.Sorting.BY_NEWEST,
            searchQuery = "cloud",
        )

        assertEquals(listOf(1), result.map { it.id })
    }

    @Test
    fun `Test load returns a single page until reset`() = runTest {
        val gateway = gateway()
        val filter = FeaturePlans.StatusFilter.COMPLETED
        val sorting = FeaturePlans.Sorting.BY_NEWEST

        assertEquals(listOf(3), gateway.load(filter, sorting, null).map { it.id })
        assertTrue(gateway.load(filter, sorting, null).isEmpty())

        gateway.reset()

        assertEquals(listOf(3), gateway.load(filter, sorting, null).map { it.id })
    }

    @Test
    fun `Test vote toggles vote and score in shared store`() = runTest {
        val store = InMemoryFeaturePlansGateway.Store(plans)
        val gateway = InMemoryFeaturePlansGateway(store)

        val voted = gateway.vote(plans[0])
        assertTrue(voted.isVoted)
        assertEquals(11, voted.score)

        val otherGateway = InMemoryFeaturePlansGateway(store)
        assertEquals(voted, otherGateway.load(1))

        val unvoted = otherGateway.vote(voted)
        assertEquals(false, unvoted.isVoted)
        assertEquals(10, unvoted.score)
    }

    @Test
    fun `Test load unknown id fails`() = runTest {
        assertFailsWith<NoSuchElementException> { gateway().load(42) }
    }

    @Test
    fun `Test load error is thrown`() = runTest {
        val gateway = InMemoryFeaturePlansGateway(loadError = IllegalStateException("Failed"))

        assertFailsWith<IllegalStateException> {
            gateway.load(FeaturePlans.StatusFilter.ALL, FeaturePlans.Sorting.BY_NEWEST, null)
        }
    }
}
