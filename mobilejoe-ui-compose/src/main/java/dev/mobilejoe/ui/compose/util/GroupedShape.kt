// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.util

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape

@Composable
internal fun groupedItemShape(
    index: Int,
    count: Int,
    shape: CornerBasedShape = MaterialTheme.shapes.medium,
): Shape {
    val isFirst = index == 0
    val isLast = index == count - 1
    val none = CornerSize(0)

    return when {
        isFirst && isLast -> shape
        isFirst -> shape.copy(bottomStart = none, bottomEnd = none)
        isLast -> shape.copy(topStart = none, topEnd = none)
        else -> RectangleShape
    }
}
