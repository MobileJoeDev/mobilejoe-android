// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.faq

import java.time.Instant

internal class InMemoryFaqsGateway(
    private val seedData: List<Faq> = emptyList(),
    private val loadError: Throwable? = null,
) : FaqsGateway {

    override suspend fun load(): List<Faq> {
        loadError?.let { throw it }
        return seedData
    }

    override suspend fun load(id: Int): Faq {
        loadError?.let { throw it }
        return seedData.firstOrNull { faq -> faq.id == id }
            ?: throw NoSuchElementException("Unknown FAQ $id.")
    }

    companion object {

        val seedData: List<Faq> = listOf(
            Faq(
                id = 1,
                title = "How do I reset my password?",
                body = "Click the 'Forgot Password' link on the login page and follow the instructions sent to your email.",
                position = 1,
                updatedAt = Instant.EPOCH,
                createdAt = Instant.EPOCH,
            ),
            Faq(
                id = 2,
                title = "How do I create an account?",
                body = "Navigate to the signup page and fill in the required fields including your email and password.",
                position = 2,
                updatedAt = Instant.EPOCH,
                createdAt = Instant.EPOCH,
            ),
            Faq(
                id = 3,
                title = "How can I contact support?",
                body = "You can reach our support team via email at support@example.com or through the in-app chat feature.",
                position = 3,
                updatedAt = Instant.EPOCH,
                createdAt = Instant.EPOCH,
            ),
        )
    }
}
