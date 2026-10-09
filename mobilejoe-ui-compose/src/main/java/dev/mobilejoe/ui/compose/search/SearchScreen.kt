// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.mobilejoe.ui.compose.R

/**
 * A full-screen search input, shown when a list screen's search action is tapped. Holds the
 * query text locally and only reports it to the caller on submit or cancel.
 *
 * @param placeholder Placeholder text shown in the empty search field; defaults to a generic
 * localized "Search" string.
 * @param onSubmit Invoked with the entered query text when the search is submitted (e.g. the
 * keyboard's search action).
 * @param onCancel Invoked when the back button is tapped, dismissing the search screen without
 * submitting.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchScreen(
    placeholder: String = stringResource(R.string.generic_search),
    onSubmit: (String) -> Unit = { _ -> },
    onCancel: () -> Unit = { },
) {
    var searchQuery by remember { mutableStateOf("") }

    Scaffold { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(contentPadding)
        ) {
            SearchBarDefaults.InputField(
                query = searchQuery,
                onQueryChange = { q -> searchQuery = q },
                onSearch = { onSubmit(it) },
                expanded = true,
                onExpandedChange = {},
                placeholder = { Text(placeholder) },
                leadingIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.generic_back),
                        )
                    }
                },
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxWidth()
            )
        }
    }
}

/** Preview of [SearchScreen] with default parameters. */
@Composable
@Preview
private fun Preview() {
    SearchScreen()
}