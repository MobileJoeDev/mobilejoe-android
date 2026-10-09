// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.feature

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

internal class InMemoryFeaturePlansGateway(
    private val store: Store = Store(),
    private val loadError: Throwable? = null,
) : FeaturePlansGateway {

    class Store(var featurePlans: List<FeaturePlan> = emptyList())

    private var isPageLoaded = false

    override suspend fun load(
        statusFilter: FeaturePlans.StatusFilter,
        sorting: FeaturePlans.Sorting,
        searchQuery: String?,
    ): List<FeaturePlan> {
        loadError?.let { throw it }

        if (isPageLoaded) {
            return emptyList()
        }
        isPageLoaded = true

        val featurePlans = store.featurePlans
            // The lowercase enum names equal the backend status keys used by the filter.
            .filter { featurePlan -> featurePlan.status.name.lowercase() in statusFilter.keys }
            .filter { featurePlan ->
                searchQuery == null || featurePlan.title.contains(searchQuery, ignoreCase = true)
            }

        return when (sorting) {
            FeaturePlans.Sorting.BY_NEWEST -> featurePlans.sortedByDescending { it.createdAt }
            FeaturePlans.Sorting.BY_SCORE -> featurePlans.sortedByDescending { it.score }
        }
    }

    override suspend fun load(id: Int): FeaturePlan {
        loadError?.let { throw it }
        return findFeaturePlan(id)
    }

    override suspend fun reset() {
        isPageLoaded = false
    }

    override suspend fun vote(featurePlan: FeaturePlan): FeaturePlan {
        loadError?.let { throw it }

        val current = findFeaturePlan(featurePlan.id)
        val updated = current.copy(
            isVoted = !current.isVoted,
            score = if (current.isVoted) current.score - 1 else current.score + 1,
        )

        store.featurePlans = store.featurePlans.map { plan ->
            if (plan.id == updated.id) updated else plan
        }
        return updated
    }

    private fun findFeaturePlan(id: Int): FeaturePlan {
        return store.featurePlans.firstOrNull { featurePlan -> featurePlan.id == id }
            ?: throw NoSuchElementException("Unknown feature plan $id.")
    }

    companion object {

        val seedStore: Store by lazy { Store(seedData) }

        private val seedData: List<FeaturePlan>
            get() = listOf(
                FeaturePlan(
                    id = 1,
                    title = "Import holidays from calendar",
                    body = "Choose an iOS calendar to automatically import and sync holidays in WorkTimes.",
                    score = 10,
                    status = FeaturePlan.Status.OPEN,
                    createdAt = utc(2025, 3, 17, 12),
                    updatedAt = utc(2025, 3, 17, 12),
                    isVoted = true,
                ),
                FeaturePlan(
                    id = 2,
                    title = "Cloud Sync",
                    body = "Sync records and accounts via iCloud on multiple devices.",
                    score = 33,
                    status = FeaturePlan.Status.PLANNED,
                    createdAt = utc(2025, 3, 15, 13),
                    updatedAt = utc(2025, 3, 18, 13),
                    isVoted = false,
                ),
                FeaturePlan(
                    id = 3,
                    title = "Apple Watch app",
                    body = "A companion Apple Watch app to quickly start and stop time tracking without reaching for your phone. Show current timer status and allow quick project switching.",
                    score = 87,
                    status = FeaturePlan.Status.UNDER_REVIEW,
                    createdAt = utc(2025, 2, 10, 9),
                    updatedAt = utc(2025, 4, 5, 14),
                    isVoted = true,
                ),
                FeaturePlan(
                    id = 4,
                    title = "Export to PDF",
                    body = "Generate professional PDF reports of time entries for clients. Include company logo, project breakdown, and hourly summaries.",
                    score = 45,
                    status = FeaturePlan.Status.IN_PROGRESS,
                    createdAt = utc(2025, 1, 22, 16),
                    updatedAt = utc(2025, 4, 12, 10),
                    isVoted = false,
                ),
                FeaturePlan(
                    id = 5,
                    title = "Dark mode improvements",
                    body = "The current dark mode has some contrast issues. Improve readability of text and make icons more visible in low-light conditions.",
                    score = 28,
                    status = FeaturePlan.Status.COMPLETED,
                    createdAt = utc(2025, 1, 5, 11),
                    updatedAt = utc(2025, 3, 1, 9),
                    isVoted = true,
                ),
                FeaturePlan(
                    id = 6,
                    title = "Recurring time entries",
                    body = "Allow setting up recurring time entries for regular meetings or tasks that happen on the same day and time each week.",
                    score = 52,
                    status = FeaturePlan.Status.PLANNED,
                    createdAt = utc(2025, 2, 28, 14),
                    updatedAt = utc(2025, 4, 8, 16),
                    isVoted = true,
                ),
                FeaturePlan(
                    id = 7,
                    title = "Widgets for home screen",
                    body = "Add iOS widgets to show today's tracked time, current running timer, and quick-start buttons for favorite projects.",
                    score = 71,
                    status = FeaturePlan.Status.IN_PROGRESS,
                    createdAt = utc(2025, 1, 15, 8),
                    updatedAt = utc(2025, 4, 10, 11),
                    isVoted = false,
                ),
                FeaturePlan(
                    id = 8,
                    title = "Pomodoro timer integration",
                    body = "Built-in Pomodoro technique support with customizable work/break intervals and automatic time entry creation after each session.",
                    score = 19,
                    status = FeaturePlan.Status.OPEN,
                    createdAt = utc(2025, 3, 25, 10),
                    updatedAt = utc(2025, 3, 25, 10),
                    isVoted = false,
                ),
                FeaturePlan(
                    id = 9,
                    title = "Team collaboration features",
                    body = "Share projects with team members, view team availability, and coordinate schedules. Include manager approval workflow for time entries.",
                    score = 64,
                    status = FeaturePlan.Status.UNDER_REVIEW,
                    createdAt = utc(2025, 2, 5, 13),
                    updatedAt = utc(2025, 4, 2, 15),
                    isVoted = true,
                ),
                FeaturePlan(
                    id = 10,
                    title = "Location-based automatic tracking",
                    body = "Automatically start/stop timers based on GPS location. When I arrive at the office, start work timer. When I leave, stop it.",
                    score = 38,
                    status = FeaturePlan.Status.OPEN,
                    createdAt = utc(2025, 3, 8, 17),
                    updatedAt = utc(2025, 3, 20, 9),
                    isVoted = false,
                ),
                FeaturePlan(
                    id = 11,
                    title = "Siri shortcuts support",
                    body = "Add Siri shortcuts to start/stop timers, switch projects, and query today's tracked time using voice commands.",
                    score = 41,
                    status = FeaturePlan.Status.PLANNED,
                    createdAt = utc(2025, 2, 18, 12),
                    updatedAt = utc(2025, 4, 6, 14),
                    isVoted = true,
                ),
                FeaturePlan(
                    id = 12,
                    title = "Invoice generation",
                    body = "Generate invoices directly from tracked time entries. Support multiple currencies, tax rates, and customizable invoice templates.",
                    score = 56,
                    status = FeaturePlan.Status.UNDER_REVIEW,
                    createdAt = utc(2025, 1, 30, 15),
                    updatedAt = utc(2025, 4, 1, 11),
                    isVoted = false,
                ),
            )

        private fun utc(year: Int, month: Int, day: Int, hour: Int): Instant =
            LocalDateTime.of(year, month, day, hour, 0).toInstant(ZoneOffset.UTC)
    }
}
