// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.feature

import dev.mobilejoe.api.network.NetworkClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class RemoteFeaturePlansGatewayTest {

    @Test
    fun `Test load initial batch`() = runTest {
        val features = listOf(FeaturePlan.empty().copy(id = 42))

        val networkClient = mockk<NetworkClient> {
            coEvery { getFeaturePlans(any(), any(), any(), any()) } returns
                    NetworkClient.Response(features)
        }
        val featurePlansGateway = RemoteFeaturePlansGateway(networkClient)

        val result = featurePlansGateway.load(
            statusFilter = FeaturePlans.StatusFilter.UNDER_REVIEW,
            sorting = FeaturePlans.Sorting.BY_NEWEST,
            searchQuery = "my query",
        )

        coVerify {
            networkClient.getFeaturePlans(
                requestUrl = any(),
                statuses = listOf("under_review"),
                sorting = "created",
                search = "my query"
            )
        }
        assertEquals(features, result)
    }

    @Test
    fun `Test load next batch`() = runTest {
        val features1 = listOf(FeaturePlan.empty().copy(id = 42))
        val features2 = listOf(FeaturePlan.empty().copy(id = 43))
        val networkClient = mockk<NetworkClient> {
            coEvery { getFeaturePlans(any(), any(), any(), any()) } returnsMany listOf(
                NetworkClient.Response(features1, "next://feature-plans"),
                NetworkClient.Response(features2)
            )
        }
        val featurePlansGateway = RemoteFeaturePlansGateway(networkClient)

        featurePlansGateway.load(
            statusFilter = FeaturePlans.StatusFilter.UNDER_REVIEW,
            sorting = FeaturePlans.Sorting.BY_NEWEST,
            searchQuery = null,
        )
        val result = featurePlansGateway.load(
            statusFilter = FeaturePlans.StatusFilter.UNDER_REVIEW,
            sorting = FeaturePlans.Sorting.BY_NEWEST,
            searchQuery = null,
        )

        coVerify {
            networkClient.getFeaturePlans(
                requestUrl = "next://feature-plans",
            )
        }
        assertEquals(features2, result)
    }

    @Test
    fun `Test load final batch`() = runTest {
        val networkClient = mockk<NetworkClient> {
            coEvery { getFeaturePlans(any(), any(), any(), any()) } returnsMany listOf(
                NetworkClient.Response(listOf(FeaturePlan.empty().copy(id = 42)))
            )
        }
        val featurePlansGateway = RemoteFeaturePlansGateway(networkClient)

        featurePlansGateway.load(
            statusFilter = FeaturePlans.StatusFilter.UNDER_REVIEW,
            sorting = FeaturePlans.Sorting.BY_NEWEST,
            searchQuery = null,
        )
        val result = featurePlansGateway.load(
            statusFilter = FeaturePlans.StatusFilter.UNDER_REVIEW,
            sorting = FeaturePlans.Sorting.BY_NEWEST,
            searchQuery = null,
        )

        coVerify(exactly = 1) {
            networkClient.getFeaturePlans(any(), any(), any(), any())
        }
        assertEquals(emptyList(), result)
    }

    @Test
    fun `Test load single`() = runTest {
        val feature = FeaturePlan.empty().copy(id = 42)

        val networkClient = mockk<NetworkClient> {
            coEvery { getFeaturePlan(any()) } returns feature
        }
        val featurePlansGateway = RemoteFeaturePlansGateway(networkClient)

        val result = featurePlansGateway.load(42)

        coVerify {
            networkClient.getFeaturePlan(42)
        }
        assertEquals(feature, result)
    }

    @Test
    fun `Test reset`() = runTest {
        val features = listOf(FeaturePlan.empty().copy(id = 42))
        val networkClient = mockk<NetworkClient> {
            coEvery { getFeaturePlans(any(), any(), any(), any()) } returns
                    NetworkClient.Response(features)
        }
        val featurePlansGateway = RemoteFeaturePlansGateway(networkClient)

        featurePlansGateway.load(
            statusFilter = FeaturePlans.StatusFilter.UNDER_REVIEW,
            sorting = FeaturePlans.Sorting.BY_NEWEST,
            searchQuery = null,
        )
        featurePlansGateway.reset()
        val result = featurePlansGateway.load(
            statusFilter = FeaturePlans.StatusFilter.UNDER_REVIEW,
            sorting = FeaturePlans.Sorting.BY_NEWEST,
            searchQuery = null
        )

        coVerify(exactly = 2) {
            networkClient.getFeaturePlans(
                requestUrl = any(),
                statuses = listOf("under_review"),
                sorting = "created",
                search = null,
            )
        }
        assertEquals(features, result)
    }

    @Test
    fun `Test vote`() = runTest {
        val feature = FeaturePlan.empty().copy(id = 42)

        val networkClient = mockk<NetworkClient> {
            coEvery { postVoteFeaturePlans(any()) } returns feature.copy(score = 1)
        }
        val featurePlansGateway = RemoteFeaturePlansGateway(networkClient)

        val result = featurePlansGateway.vote(feature)

        coVerify { networkClient.postVoteFeaturePlans(42) }
        assertEquals(feature.copy(score = 1), result)
    }
}
