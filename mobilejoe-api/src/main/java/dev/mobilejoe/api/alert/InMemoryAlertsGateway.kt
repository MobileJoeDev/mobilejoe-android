// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.alert

import java.time.Instant

internal class InMemoryAlertsGateway(
    private val seedData: List<Alert> = emptyList(),
    private val loadError: Throwable? = null,
) : AlertsGateway {

    override suspend fun load(): List<Alert> {
        loadError?.let { throw it }
        return seedData
    }

    companion object {

        val seedData: List<Alert> = listOf(
            Alert(
                id = 1,
                title = "Critical error.",
                message = "Please update version.",
                kind = Alert.Kind.ERROR,
                occurred = Instant.parse("2025-04-14T09:30:00Z"),
                updatedAt = Instant.parse("2025-04-14T09:30:00Z"),
                createdAt = Instant.parse("2025-04-14T09:30:00Z"),
            ),
            Alert(
                id = 2,
                title = "Please be patient and stay tuned for further updates. We're working hard.",
                message = null,
                kind = Alert.Kind.ERROR,
                occurred = Instant.parse("2025-04-12T16:45:00Z"),
                updatedAt = Instant.parse("2025-04-12T16:45:00Z"),
                createdAt = Instant.parse("2025-04-12T16:45:00Z"),
            ),
            Alert(
                id = 3,
                title = "Please be patient and stay tuned for further updates. We're working hard.",
                message = "Our developer spilled coffee on the keyboard again. We've switched to decaf and are working on a fix.",
                kind = Alert.Kind.WARNING,
                occurred = Instant.parse("2025-04-10T08:15:00Z"),
                updatedAt = Instant.parse("2025-04-10T08:15:00Z"),
                createdAt = Instant.parse("2025-04-10T08:15:00Z"),
            ),
        )

        val singleAlert: List<Alert> = listOf(
            Alert(
                id = 1,
                title = "Please be patient and stay tuned for further updates. We're working hard.",
                message = "Please update version.",
                kind = Alert.Kind.WARNING,
                occurred = Instant.parse("2025-04-14T09:30:00Z"),
                updatedAt = Instant.parse("2025-04-14T09:30:00Z"),
                createdAt = Instant.parse("2025-04-14T09:30:00Z"),
            ),
        )
    }
}
