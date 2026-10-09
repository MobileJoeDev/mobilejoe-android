// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.feature.list

import android.util.Log
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import dev.mobilejoe.api.feature.FeaturePlan
import dev.mobilejoe.api.feature.FeaturePlans
import dev.mobilejoe.ui.compose.viewModelScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class FeaturePlanListViewModelTest {

    @Test
    fun `Test initial loading`() = runTest {
        val featurePlans = mockk<FeaturePlans>(relaxed = true) {
            every { all } returns flow {}
        }
        val viewModel = FeaturePlanListViewModel(featurePlans, viewModelScope)

        assertEquals(
            FeaturePlanListViewModel.State.Default(
                FeaturePlanListViewModel.State.ListState.Loading(
                    statusFilter = FeaturePlans.StatusFilter.ALL,
                    sorting = FeaturePlans.Sorting.BY_NEWEST,
                )
            ),
            viewModel.state.first()
        )
    }

    @Test
    fun `Test initial error`() = runTest {
        turbineScope {
            mockkStatic(Log::class)
            every { Log.e(any(), any(), any()) } returns 0

            val featurePlans = mockk<FeaturePlans>(relaxed = true) {
                every { all } returns flow {
                    emit(listOf())
                }
                every { statusFilter } returns FeaturePlans.StatusFilter.ALL
                every { sorting } returns FeaturePlans.Sorting.BY_NEWEST
                coEvery { load() } throws Exception()
            }
            val viewModel = FeaturePlanListViewModel(featurePlans, viewModelScope)

            viewModel.state.test {
                skipItems(1) // Skip loading state

                assertEquals(
                    FeaturePlanListViewModel.State.Default(
                        FeaturePlanListViewModel.State.ListState.Error(
                            statusFilter = FeaturePlans.StatusFilter.ALL,
                            sorting = FeaturePlans.Sorting.BY_NEWEST,
                        )
                    ),
                    awaitItem()
                )
            }
        }
    }

    @Test
    fun `Test initial empty`() = runTest {
        turbineScope {
            val featurePlans = mockk<FeaturePlans>(relaxed = true) {
                every { all } returns flow {
                    emit(listOf())
                }
                every { statusFilter } returns FeaturePlans.StatusFilter.ALL
                every { sorting } returns FeaturePlans.Sorting.BY_NEWEST
                coEvery { load() } returns Unit
            }
            val viewModel = FeaturePlanListViewModel(featurePlans, viewModelScope)

            viewModel.state.test {
                skipItems(1) // Skip loading state

                assertEquals(
                    FeaturePlanListViewModel.State.Default(
                        FeaturePlanListViewModel.State.ListState.Empty(
                            statusFilter = FeaturePlans.StatusFilter.ALL,
                            sorting = FeaturePlans.Sorting.BY_NEWEST,
                        )
                    ),
                    awaitItem()
                )
            }
        }
    }

    @Test
    fun `Test initial success`() = runTest {
        turbineScope {
            val featurePlanItems = listOf(
                FeaturePlan.empty().copy(id = 1),
                FeaturePlan.empty().copy(id = 2)
            )

            val featurePlans = mockk<FeaturePlans>(relaxed = true) {
                every { all } returns flow {
                    emit(featurePlanItems)
                }
                every { statusFilter } returns FeaturePlans.StatusFilter.ALL
                every { sorting } returns FeaturePlans.Sorting.BY_NEWEST
                coEvery { load() } returns Unit
            }
            val viewModel = FeaturePlanListViewModel(featurePlans, viewModelScope)

            viewModel.state.test {
                skipItems(1) // Skip loading state

                assertEquals(
                    FeaturePlanListViewModel.State.Default(
                        FeaturePlanListViewModel.State.ListState.Success(
                            statusFilter = FeaturePlans.StatusFilter.ALL,
                            sorting = FeaturePlans.Sorting.BY_NEWEST,
                            featurePlanItems = featurePlanItems,
                        )
                    ),
                    awaitItem()
                )
            }
        }
    }

    @Test
    fun `Test retry failed loading`() = runTest {
        turbineScope {
            mockkStatic(Log::class)
            every { Log.e(any(), any(), any()) } returns 0

            val featurePlanItems = listOf(FeaturePlan.empty().copy(id = 1))
            val featurePlans = mockk<FeaturePlans>(relaxed = true) {
                every { all } returns flow {
                    emit(featurePlanItems)
                }
                every { statusFilter } returns FeaturePlans.StatusFilter.ALL
                every { sorting } returns FeaturePlans.Sorting.BY_NEWEST
                coEvery { load() } throws Exception()
                coEvery { reload() } returns Unit
            }
            val viewModel = FeaturePlanListViewModel(featurePlans, viewModelScope)

            viewModel.state.test {
                skipItems(2) // Skip initial & error state

                viewModel.onRetryClicked()

                assertEquals(
                    FeaturePlanListViewModel.State.Default(
                        FeaturePlanListViewModel.State.ListState.Success(
                            statusFilter = FeaturePlans.StatusFilter.ALL,
                            sorting = FeaturePlans.Sorting.BY_NEWEST,
                            featurePlanItems = featurePlanItems,
                        )
                    ),
                    awaitItem()
                )
            }

            coVerify(exactly = 1) { featurePlans.reload() }
        }
    }

    @Test
    fun `Test retry failure after initial error stays on error`() = runTest {
        turbineScope {
            mockkStatic(Log::class)
            every { Log.e(any(), any(), any()) } returns 0

            val featurePlans = mockk<FeaturePlans>(relaxed = true) {
                every { all } returns flow {
                    emit(listOf())
                }
                every { statusFilter } returns FeaturePlans.StatusFilter.ALL
                every { sorting } returns FeaturePlans.Sorting.BY_NEWEST
                coEvery { load() } throws Exception()
                coEvery { reload() } throws Exception()
            }
            val viewModel = FeaturePlanListViewModel(featurePlans, viewModelScope)

            viewModel.state.test {
                skipItems(2) // Skip initial & error state

                viewModel.onRetryClicked()

                advanceUntilIdle()
                expectNoEvents()
            }

            assertEquals(
                FeaturePlanListViewModel.State.Default(
                    FeaturePlanListViewModel.State.ListState.Error(
                        statusFilter = FeaturePlans.StatusFilter.ALL,
                        sorting = FeaturePlans.Sorting.BY_NEWEST,
                    )
                ),
                viewModel.state.value
            )
            coVerify(exactly = 1) { featurePlans.reload() }
        }
    }

    @Test
    fun `Test refresh`() = runTest {
        turbineScope {
            mockkStatic(Log::class)
            every { Log.e(any(), any(), any()) } returns 0

            val featurePlanItems = listOf(FeaturePlan.empty().copy(id = 1))
            val featurePlans = mockk<FeaturePlans>(relaxed = true) {
                every { all } returns flow {
                    emit(featurePlanItems)
                }
                every { statusFilter } returns FeaturePlans.StatusFilter.ALL
                every { sorting } returns FeaturePlans.Sorting.BY_NEWEST
                coEvery { load() } throws Exception()
                coEvery { reload() } returns Unit
            }
            val viewModel = FeaturePlanListViewModel(featurePlans, viewModelScope)

            viewModel.state.test {
                skipItems(2) // Skip initial & error state

                viewModel.onRefresh()

                assertEquals(
                    FeaturePlanListViewModel.State.Default(
                        FeaturePlanListViewModel.State.ListState.Success(
                            statusFilter = FeaturePlans.StatusFilter.ALL,
                            sorting = FeaturePlans.Sorting.BY_NEWEST,
                            featurePlanItems = featurePlanItems,
                        )
                    ),
                    awaitItem()
                )
            }

            coVerify(exactly = 1) { featurePlans.reload() }
        }
    }

    @Test
    fun `Test refresh failure keeps previous list`() = runTest {
        turbineScope {
            mockkStatic(Log::class)
            every { Log.e(any(), any(), any()) } returns 0

            val featurePlanItems = listOf(FeaturePlan.empty().copy(id = 1))
            val featurePlans = mockk<FeaturePlans>(relaxed = true) {
                every { all } returns flow {
                    emit(featurePlanItems)
                }
                every { statusFilter } returns FeaturePlans.StatusFilter.ALL
                every { sorting } returns FeaturePlans.Sorting.BY_NEWEST
                coEvery { load() } returns Unit
                coEvery { reload() } throws Exception()
            }
            val viewModel = FeaturePlanListViewModel(featurePlans, viewModelScope)

            viewModel.state.test {
                skipItems(1) // Skip loading state

                assertEquals(
                    FeaturePlanListViewModel.State.Default(
                        FeaturePlanListViewModel.State.ListState.Success(
                            statusFilter = FeaturePlans.StatusFilter.ALL,
                            sorting = FeaturePlans.Sorting.BY_NEWEST,
                            featurePlanItems = featurePlanItems,
                        )
                    ),
                    awaitItem()
                )

                viewModel.onRefresh()

                advanceUntilIdle()
                expectNoEvents()
            }
        }
    }

    @Test
    fun `Test load more on list end reached`() = runTest {
        turbineScope {
            val featurePlanItems = listOf(FeaturePlan.empty().copy(id = 1))

            val featurePlans = mockk<FeaturePlans>(relaxed = true) {
                every { all } returns flow {
                    emit(featurePlanItems)
                }
                every { statusFilter } returns FeaturePlans.StatusFilter.ALL
                every { sorting } returns FeaturePlans.Sorting.BY_NEWEST
                coEvery { load() } returns Unit
            }
            val viewModel = FeaturePlanListViewModel(featurePlans, viewModelScope)

            viewModel.onListEndReached()

            advanceUntilIdle()
            coVerify(exactly = 2) { featurePlans.load() }
        }
    }

    @Test
    fun `Test vote`() = runTest {
        turbineScope {
            val featurePlanItem1 = FeaturePlan.empty().copy(id = 1)
            val featurePlanItems =
                listOf(featurePlanItem1, FeaturePlan.empty().copy(id = 2))

            val featurePlans = mockk<FeaturePlans>(relaxed = true) {
                every { all } returns flow {
                    emit(featurePlanItems)
                }
                every { statusFilter } returns FeaturePlans.StatusFilter.ALL
                every { sorting } returns FeaturePlans.Sorting.BY_NEWEST
                coEvery { load() } returns Unit
            }
            val viewModel = FeaturePlanListViewModel(featurePlans, viewModelScope)

            viewModel.onUpVoteClicked(featurePlanItem1)

            advanceUntilIdle()
            coVerify(exactly = 1) { featurePlans.vote(featurePlanItem1) }
        }
    }

    @Test
    fun `Test select status filter`() = runTest {
        turbineScope {
            val featurePlanItems =
                listOf(
                    FeaturePlan.empty().copy(id = 1),
                    FeaturePlan.empty().copy(id = 2)
                )

            val featurePlans = mockk<FeaturePlans>(relaxed = true) {
                every { all } returns flow {
                    emit(featurePlanItems)
                }
                every { statusFilter } returns FeaturePlans.StatusFilter.ALL
                every { sorting } returns FeaturePlans.Sorting.BY_NEWEST
                coEvery { load() } returns Unit
            }
            val viewModel = FeaturePlanListViewModel(featurePlans, viewModelScope)

            viewModel.state.test {
                skipItems(1) // Skip success state

                viewModel.onStatusFilterSelected(FeaturePlans.StatusFilter.PLANNED)

                assertEquals(
                    FeaturePlanListViewModel.State.Default(
                        FeaturePlanListViewModel.State.ListState.Success(
                            statusFilter = FeaturePlans.StatusFilter.PLANNED,
                            sorting = FeaturePlans.Sorting.BY_NEWEST,
                            featurePlanItems = featurePlanItems,
                        )
                    ),
                    awaitItem()
                )
            }

            coVerify(exactly = 1) { featurePlans.setStatusFilter(FeaturePlans.StatusFilter.PLANNED) }
        }
    }

    @Test
    fun `Test select sorting`() = runTest {
        turbineScope {
            val featurePlanItems =
                listOf(
                    FeaturePlan.empty().copy(id = 1),
                    FeaturePlan.empty().copy(id = 2)
                )

            val featurePlans = mockk<FeaturePlans>(relaxed = true) {
                every { all } returns flow {
                    emit(featurePlanItems)
                }
                every { statusFilter } returns FeaturePlans.StatusFilter.ALL
                every { sorting } returns FeaturePlans.Sorting.BY_NEWEST
                coEvery { load() } returns Unit
            }
            val viewModel = FeaturePlanListViewModel(featurePlans, viewModelScope)

            viewModel.state.test {
                viewModel.onSortSelected(FeaturePlans.Sorting.BY_SCORE)

                skipItems(1) // Skip intermediate state with updated sorting

                assertEquals(
                    FeaturePlanListViewModel.State.Default(
                        FeaturePlanListViewModel.State.ListState.Success(
                            statusFilter = FeaturePlans.StatusFilter.ALL,
                            sorting = FeaturePlans.Sorting.BY_SCORE,
                            featurePlanItems = featurePlanItems,
                        )
                    ),
                    awaitItem()
                )
            }

            coVerify(exactly = 1) { featurePlans.setSorting(FeaturePlans.Sorting.BY_SCORE) }
        }
    }

    @Test
    fun `Test start search`() = runTest {
        turbineScope {
            val featurePlans = mockk<FeaturePlans>(relaxed = true) {
                every { all } returns flow {
                    emit(listOf(FeaturePlan.empty().copy(id = 1)))
                }
                every { statusFilter } returns FeaturePlans.StatusFilter.ALL
                every { sorting } returns FeaturePlans.Sorting.BY_NEWEST
                coEvery { load() } returns Unit
            }
            val viewModel = FeaturePlanListViewModel(featurePlans, viewModelScope)

            viewModel.state.test {
                skipItems(1) // Skip success state

                viewModel.onSearchClicked()

                assertEquals(
                    FeaturePlanListViewModel.State.SearchInput,
                    awaitItem()
                )
            }
        }
    }

    @Test
    fun `Test cancel search`() = runTest {
        turbineScope {
            val featurePlanItems = listOf(FeaturePlan.empty().copy(id = 1))

            val featurePlans = mockk<FeaturePlans>(relaxed = true) {
                every { all } returns flow {
                    emit(featurePlanItems)
                }
                every { statusFilter } returns FeaturePlans.StatusFilter.ALL
                every { sorting } returns FeaturePlans.Sorting.BY_NEWEST
                coEvery { load() } returns Unit
            }
            val viewModel = FeaturePlanListViewModel(featurePlans, viewModelScope)

            viewModel.state.test {
                skipItems(1) // Skip success state

                viewModel.onSearchClicked()

                skipItems(1) // Skip search input state

                viewModel.onCancelSearchClicked()

                assertEquals(
                    FeaturePlanListViewModel.State.Default(
                        FeaturePlanListViewModel.State.ListState.Loading(
                            statusFilter = FeaturePlans.StatusFilter.ALL,
                            sorting = FeaturePlans.Sorting.BY_NEWEST,
                        )
                    ),
                    awaitItem()
                )

                assertEquals(
                    FeaturePlanListViewModel.State.Default(
                        FeaturePlanListViewModel.State.ListState.Success(
                            statusFilter = FeaturePlans.StatusFilter.ALL,
                            sorting = FeaturePlans.Sorting.BY_NEWEST,
                            featurePlanItems = featurePlanItems,
                        )
                    ),
                    awaitItem()
                )
            }
        }
    }

    @Test
    fun `Test submit search query`() = runTest {
        turbineScope {
            val featurePlanItems = listOf(FeaturePlan.empty().copy(id = 1))

            val featurePlans = mockk<FeaturePlans>(relaxed = true) {
                every { all } returns flow {
                    emit(featurePlanItems)
                }
                every { statusFilter } returns FeaturePlans.StatusFilter.ALL
                every { sorting } returns FeaturePlans.Sorting.BY_NEWEST
                coEvery { load() } returns Unit
            }
            val viewModel = FeaturePlanListViewModel(featurePlans, viewModelScope)

            viewModel.state.test {
                skipItems(1) // Skip success state

                viewModel.onSearchClicked()

                skipItems(1) // Skip search input state

                viewModel.onSearchSubmitted("my feature")

                assertEquals(
                    FeaturePlanListViewModel.State.SearchResult(
                        query = "my feature",
                        listState = FeaturePlanListViewModel.State.ListState.Loading(
                            statusFilter = FeaturePlans.StatusFilter.ALL,
                            sorting = FeaturePlans.Sorting.BY_NEWEST,
                        )
                    ),
                    awaitItem()
                )

                assertEquals(
                    FeaturePlanListViewModel.State.SearchResult(
                        query = "my feature",
                        listState = FeaturePlanListViewModel.State.ListState.Success(
                            statusFilter = FeaturePlans.StatusFilter.ALL,
                            sorting = FeaturePlans.Sorting.BY_NEWEST,
                            featurePlanItems = featurePlanItems,
                        )
                    ),
                    awaitItem()
                )
            }

            coVerify(exactly = 1) { featurePlans.setSearchQuery("my feature") }
        }
    }

    @Test
    fun `Test vote error`() = runTest {
        turbineScope {
            mockkStatic(Log::class)
            every { Log.e(any(), any(), any()) } returns 0

            val featurePlanItem1 = FeaturePlan.empty().copy(id = 1)
            val featurePlanItems =
                listOf(featurePlanItem1, FeaturePlan.empty().copy(id = 2))

            val featurePlans = mockk<FeaturePlans>(relaxed = true) {
                every { all } returns flow {
                    emit(featurePlanItems)
                }
                every { statusFilter } returns FeaturePlans.StatusFilter.ALL
                every { sorting } returns FeaturePlans.Sorting.BY_NEWEST
                coEvery { load() } returns Unit
                coEvery { vote(featurePlanItem1) } throws Exception()
            }
            val viewModel = FeaturePlanListViewModel(featurePlans, viewModelScope)


            viewModel.onUpVoteClicked(featurePlanItem1)

            advanceUntilIdle()
            coVerify(exactly = 1) { featurePlans.vote(featurePlanItem1) }
        }
    }

    @Test
    fun `Test select status filter error`() = runTest {
        turbineScope {
            mockkStatic(Log::class)
            every { Log.e(any(), any(), any()) } returns 0

            val featurePlanItems =
                listOf(
                    FeaturePlan.empty().copy(id = 1),
                    FeaturePlan.empty().copy(id = 2)
                )

            val featurePlans = mockk<FeaturePlans>(relaxed = true) {
                every { all } returns flow {
                    emit(featurePlanItems)
                }
                every { statusFilter } returns FeaturePlans.StatusFilter.ALL
                every { sorting } returns FeaturePlans.Sorting.BY_NEWEST
                coEvery { load() } returns Unit
                coEvery { setStatusFilter(FeaturePlans.StatusFilter.PLANNED) } throws Exception()
            }
            val viewModel = FeaturePlanListViewModel(featurePlans, viewModelScope)

            viewModel.state.test {
                skipItems(1) // Skip success state

                viewModel.onStatusFilterSelected(FeaturePlans.StatusFilter.PLANNED)

                assertEquals(
                    FeaturePlanListViewModel.State.Default(
                        FeaturePlanListViewModel.State.ListState.Error(
                            statusFilter = FeaturePlans.StatusFilter.PLANNED,
                            sorting = FeaturePlans.Sorting.BY_NEWEST,
                        )
                    ),
                    awaitItem()
                )
            }
        }
    }

    @Test
    fun `Test select sorting error`() = runTest {
        turbineScope {
            mockkStatic(Log::class)
            every { Log.e(any(), any(), any()) } returns 0

            val featurePlanItems =
                listOf(
                    FeaturePlan.empty().copy(id = 1),
                    FeaturePlan.empty().copy(id = 2)
                )

            val featurePlans = mockk<FeaturePlans>(relaxed = true) {
                every { all } returns flow {
                    emit(featurePlanItems)
                }
                every { statusFilter } returns FeaturePlans.StatusFilter.ALL
                every { sorting } returns FeaturePlans.Sorting.BY_NEWEST
                coEvery { load() } returns Unit
                coEvery { setSorting(FeaturePlans.Sorting.BY_SCORE) } throws Exception()
            }
            val viewModel = FeaturePlanListViewModel(featurePlans, viewModelScope)

            viewModel.state.test {
                viewModel.onSortSelected(FeaturePlans.Sorting.BY_SCORE)

                skipItems(1) // Skip intermediate state with updated sorting

                assertEquals(
                    FeaturePlanListViewModel.State.Default(
                        FeaturePlanListViewModel.State.ListState.Error(
                            statusFilter = FeaturePlans.StatusFilter.ALL,
                            sorting = FeaturePlans.Sorting.BY_SCORE,
                        )
                    ),
                    awaitItem()
                )
            }
        }
    }

    @Test
    fun `Test cancel search error`() = runTest {
        turbineScope {
            mockkStatic(Log::class)
            every { Log.e(any(), any(), any()) } returns 0

            val featurePlanItems = listOf(FeaturePlan.empty().copy(id = 1))

            val featurePlans = mockk<FeaturePlans>(relaxed = true) {
                every { all } returns flow {
                    emit(featurePlanItems)
                }
                every { statusFilter } returns FeaturePlans.StatusFilter.ALL
                every { sorting } returns FeaturePlans.Sorting.BY_NEWEST
                coEvery { load() } returns Unit
                coEvery { setSearchQuery(null) } throws Exception()
            }
            val viewModel = FeaturePlanListViewModel(featurePlans, viewModelScope)

            viewModel.state.test {
                skipItems(1) // Skip success state

                viewModel.onSearchClicked()

                skipItems(1) // Skip search input state

                viewModel.onCancelSearchClicked()

                skipItems(1) // Skip intermediate state with search cleared

                assertEquals(
                    FeaturePlanListViewModel.State.Default(
                        FeaturePlanListViewModel.State.ListState.Error(
                            statusFilter = FeaturePlans.StatusFilter.ALL,
                            sorting = FeaturePlans.Sorting.BY_NEWEST,
                        )
                    ),
                    awaitItem()
                )
            }
        }
    }

    @Test
    fun `Test submit search query error`() = runTest {
        turbineScope {
            mockkStatic(Log::class)
            every { Log.e(any(), any(), any()) } returns 0

            val featurePlanItems = listOf(FeaturePlan.empty().copy(id = 1))

            val featurePlans = mockk<FeaturePlans>(relaxed = true) {
                every { all } returns flow {
                    emit(featurePlanItems)
                }
                every { statusFilter } returns FeaturePlans.StatusFilter.ALL
                every { sorting } returns FeaturePlans.Sorting.BY_NEWEST
                coEvery { load() } returns Unit
                coEvery { setSearchQuery("my feature") } throws Exception()
            }
            val viewModel = FeaturePlanListViewModel(featurePlans, viewModelScope)

            viewModel.state.test {
                skipItems(1) // Skip success state

                viewModel.onSearchClicked()

                skipItems(1) // Skip search input state

                viewModel.onSearchSubmitted("my feature")

                skipItems(1) // Skip intermediate state with search query set

                assertEquals(
                    FeaturePlanListViewModel.State.SearchResult(
                        query = "my feature",
                        listState = FeaturePlanListViewModel.State.ListState.Error(
                            statusFilter = FeaturePlans.StatusFilter.ALL,
                            sorting = FeaturePlans.Sorting.BY_NEWEST,
                        )
                    ),
                    awaitItem()
                )
            }
        }
    }
}