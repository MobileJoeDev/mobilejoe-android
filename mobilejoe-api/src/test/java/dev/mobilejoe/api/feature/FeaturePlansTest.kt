// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.feature

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
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

    @Test
    fun `Test vote before the list is loaded keeps the list unloaded`() = runTest {
        val featurePlan42 = FeaturePlan.empty().copy(id = 42, isVoted = false, score = 0)
        val featurePlansGateway = mockk<FeaturePlansGateway> {
            coEvery { load(any()) } returns featurePlan42
            coEvery { vote(any()) } returns featurePlan42.copy(isVoted = true, score = 1)
        }
        val featurePlans = FeaturePlans(featurePlansGateway)

        featurePlans.load(42)
        featurePlans.vote(featurePlan42)

        assertEquals(null, featurePlans.all.first())
        assertEquals(
            featurePlan42.copy(isVoted = true, score = 1),
            featurePlans.single.first()
        )
    }

    @Test
    fun `Test concurrent load calls are serialized by lock`() = runTest {
        assertCallsAreSerializedByLock(FeaturePlans::load, FeaturePlans::load)
    }

    @Test
    fun `Test concurrent reload calls are serialized by lock`() = runTest {
        assertCallsAreSerializedByLock(FeaturePlans::reload, FeaturePlans::reload)
    }

    @Test
    fun `Test concurrent load and reload calls are serialized by lock`() = runTest {
        assertCallsAreSerializedByLock(FeaturePlans::load, FeaturePlans::reload)
    }

    @Test
    fun `Test concurrent load and setStatusFilter calls are serialized by lock`() = runTest {
        assertCallsAreSerializedByLock(FeaturePlans::load, { setStatusFilter(FeaturePlans.StatusFilter.COMPLETED) })
    }

    @Test
    fun `Test concurrent load and setSorting calls are serialized by lock`() = runTest {
        assertCallsAreSerializedByLock(FeaturePlans::load, { setSorting(FeaturePlans.Sorting.BY_SCORE) })
    }

    @Test
    fun `Test concurrent load and setSearchQuery calls are serialized by lock`() = runTest {
        assertCallsAreSerializedByLock(FeaturePlans::load, { setSearchQuery("search") })
    }

    private fun TestScope.assertCallsAreSerializedByLock(
        firstCall: suspend FeaturePlans.() -> Unit,
        secondCall: suspend FeaturePlans.() -> Unit,
    ) {
        var callCount = 0
        val firstLoadCallFinished = CompletableDeferred<Unit>()
        val featurePlansGateway = mockk<FeaturePlansGateway> {
            coEvery { load(any(), any(), any()) } coAnswers {
                if (++callCount == 1) {
                    firstLoadCallFinished.await()
                }

                listOf(FeaturePlan.empty())
            }
            coJustRun { reset() }
        }

        val featurePlans = FeaturePlans(featurePlansGateway)

        launch { featurePlans.firstCall() }
        runCurrent()

        launch { featurePlans.secondCall() }
        runCurrent()

        assertEquals(1, callCount)

        firstLoadCallFinished.complete(Unit)
        advanceUntilIdle()

        assertEquals(2, callCount)
    }
}
