// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.feature

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class FeaturePlansTest {

    @Test
    fun `Test load`() = runTest {
        val featurePlanList = listOf(FeaturePlan.empty().copy(id = 41))
        val featurePlansGateway = mockk<FeaturePlansGateway> {
            coEvery { load(any(), any(), any()) } returns featurePlanList
        }
        val featurePlans = FeaturePlans(featurePlansGateway)

        featurePlans.load()

        coVerify(exactly = 1) { featurePlansGateway.load(any(), any(), any()) }
        assertEquals(featurePlanList, featurePlans.all.first())
    }

    @Test
    fun `Test load with search`() = runTest {
        val featurePlanList = listOf(FeaturePlan.empty().copy(id = 41))
        val featurePlansGateway = mockk<FeaturePlansGateway> {
            coEvery { load(any(), any(), any()) } returns featurePlanList
            coEvery { reset() } returns Unit
        }
        val featurePlans = FeaturePlans(featurePlansGateway)

        featurePlans.setSearchQuery("query")

        coVerify(exactly = 1) { featurePlansGateway.reset() }
        coVerify(exactly = 1) { featurePlansGateway.load(any(), any(), "query") }
        assertEquals(featurePlanList, featurePlans.all.first())
    }

    @Test
    fun `Test load with sorting by newest`() = runTest {
        val featurePlanList = listOf(FeaturePlan.empty().copy(id = 41))
        val featurePlansGateway = mockk<FeaturePlansGateway> {
            coEvery { load(any(), any(), any()) } returns featurePlanList
            coEvery { reset() } returns Unit
        }
        val featurePlans = FeaturePlans(featurePlansGateway)

        featurePlans.setSorting(FeaturePlans.Sorting.BY_NEWEST)

        coVerify(exactly = 1) { featurePlansGateway.reset() }
        coVerify(exactly = 1) {
            featurePlansGateway.load(
                any(),
                FeaturePlans.Sorting.BY_NEWEST,
                any()
            )
        }
        assertEquals(featurePlanList, featurePlans.all.first())
    }

    @Test
    fun `Test load with sorting by votes`() = runTest {
        val featurePlanList = listOf(FeaturePlan.empty().copy(id = 41))
        val featurePlansGateway = mockk<FeaturePlansGateway> {
            coEvery { load(any(), any(), any()) } returns featurePlanList
            coEvery { reset() } returns Unit
        }
        val featurePlans = FeaturePlans(featurePlansGateway)

        featurePlans.setSorting(FeaturePlans.Sorting.BY_SCORE)

        coVerify(exactly = 1) { featurePlansGateway.reset() }
        coVerify(exactly = 1) {
            featurePlansGateway.load(
                any(),
                FeaturePlans.Sorting.BY_SCORE,
                any()
            )
        }
        assertEquals(featurePlanList, featurePlans.all.first())
    }

    @Test
    fun `Test load with all status filter`() = runTest {
        val featurePlanList = listOf(FeaturePlan.empty().copy(id = 41))
        val featurePlansGateway = mockk<FeaturePlansGateway> {
            coEvery { load(any(), any(), any()) } returns featurePlanList
            coEvery { reset() } returns Unit
        }
        val featurePlans = FeaturePlans(featurePlansGateway)

        featurePlans.setStatusFilter(FeaturePlans.StatusFilter.ALL)

        coVerify(exactly = 1) { featurePlansGateway.reset() }
        coVerify(exactly = 1) {
            featurePlansGateway.load(
                FeaturePlans.StatusFilter.ALL,
                any(),
                any()
            )
        }
        assertEquals(featurePlanList, featurePlans.all.first())
    }

    @Test
    fun `Test load with under review filter`() = runTest {
        val featurePlanList = listOf(FeaturePlan.empty().copy(id = 41))
        val featurePlansGateway = mockk<FeaturePlansGateway> {
            coEvery { load(any(), any(), any()) } returns featurePlanList
            coEvery { reset() } returns Unit
        }
        val featurePlans = FeaturePlans(featurePlansGateway)

        featurePlans.setStatusFilter(FeaturePlans.StatusFilter.UNDER_REVIEW)

        coVerify(exactly = 1) { featurePlansGateway.reset() }
        coVerify(exactly = 1) {
            featurePlansGateway.load(
                FeaturePlans.StatusFilter.UNDER_REVIEW,
                any(),
                any()
            )
        }
        assertEquals(featurePlanList, featurePlans.all.first())
    }

    @Test
    fun `Test load specific feature plan`() = runTest {
        val featurePlan = FeaturePlan.empty().copy(id = 42)
        val featurePlansGateway = mockk<FeaturePlansGateway> {
            coEvery { load(any()) } returns featurePlan
        }
        val featurePlans = FeaturePlans(featurePlansGateway)

        featurePlans.load(42)

        coVerify(exactly = 1) {
            featurePlansGateway.load(42)
        }
        assertEquals(featurePlan, featurePlans.single.first())
    }

    @Test
    fun `Test reload`() = runTest {
        val featurePlanList = listOf(
            FeaturePlan.empty().copy(id = 41),
            FeaturePlan.empty().copy(id = 42),
        )
        val featurePlansGateway = mockk<FeaturePlansGateway> {
            coEvery { load(any(), any(), any()) } returnsMany listOf(
                featurePlanList,
                featurePlanList.take(1)
            )
            coEvery { reset() } returns Unit
        }
        val featurePlans = FeaturePlans(featurePlansGateway)

        featurePlans.load()
        featurePlans.reload()

        coVerify(exactly = 2) { featurePlansGateway.load(any(), any(), any()) }
        assertEquals(featurePlanList.take(1), featurePlans.all.first())
    }

    @Test
    fun `Test reload keeps the previous list when it fails`() = runTest {
        val featurePlanList = listOf(FeaturePlan.empty().copy(id = 41))
        val featurePlansGateway = mockk<FeaturePlansGateway> {
            coEvery { load(any(), any(), any()) } returns featurePlanList andThenThrows Exception()
            coEvery { reset() } returns Unit
        }
        val featurePlans = FeaturePlans(featurePlansGateway)

        featurePlans.load()
        runCatching { featurePlans.reload() }

        assertEquals(featurePlanList, featurePlans.all.first())
    }

    @Test
    fun `Test vote`() = runTest {
        val featurePlan41 = FeaturePlan.empty().copy(id = 41, isVoted = false, score = 0)
        val featurePlan42 = FeaturePlan.empty().copy(id = 42, isVoted = false, score = 0)
        val featurePlansGateway = mockk<FeaturePlansGateway> {
            coEvery { load(any(), any(), any()) } returns listOf(
                featurePlan41,
                featurePlan42,
            )
            coEvery { load(any()) } returns featurePlan42
            coEvery { vote(any()) } returns featurePlan42.copy(isVoted = true, score = 1)
        }
        val featurePlans = FeaturePlans(featurePlansGateway)

        featurePlans.load()
        featurePlans.load(42)
        featurePlans.vote(featurePlan42)

        coVerify { featurePlansGateway.vote(featurePlan42) }
        assertEquals(
            listOf(
                featurePlan41,
                featurePlan42.copy(isVoted = true, score = 1),
            ),
            featurePlans.all.first()
        )
        assertEquals(
            featurePlan42.copy(isVoted = true, score = 1),
            featurePlans.single.first()
        )
    }
}
