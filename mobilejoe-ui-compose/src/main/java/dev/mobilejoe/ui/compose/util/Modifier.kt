// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.util

import androidx.compose.ui.Modifier

/**
 * Conditionally chains [modifier] onto this [Modifier], avoiding an inline `if` at each call
 * site when building up modifier chains.
 *
 * @receiver The base modifier to conditionally extend.
 * @param condition When `true`, [modifier] is applied; when `false`, this modifier is
 * returned unchanged.
 * @param modifier Builder for the modifier to conditionally apply, receiving a fresh
 * [Modifier] as its receiver.
 * @return This modifier with [modifier] appended, or this modifier unchanged if [condition]
 * is `false`.
 */
internal inline fun Modifier.applyIf(condition : Boolean, modifier : Modifier.() -> Modifier) : Modifier {
    return if (condition) {
        then(modifier(Modifier))
    } else {
        this
    }
}